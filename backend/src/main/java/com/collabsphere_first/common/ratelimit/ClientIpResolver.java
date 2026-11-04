package com.collabsphere_first.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hardened client IP extraction preventing X-Forwarded-For rate-limit identity spoofing.
 *
 * Default Policy:
 * DO NOT trust X-Forwarded-For or X-Real-IP from arbitrary internet peers.
 * If request.getRemoteAddr() is not in configured trusted-proxies, remoteAddr is strictly used.
 * If remoteAddr is a trusted proxy, X-Forwarded-For is parsed right-to-left through the trusted proxy chain
 * to locate the genuine client hop.
 * Malformed headers fail safe to remoteAddr.
 */
@Slf4j
@Component
public class ClientIpResolver {

    private final List<IpMatcher> trustedProxyMatchers = new ArrayList<>();

    public ClientIpResolver(
            @Value("${collobster.security.trusted-proxies:127.0.0.1,::1}") String trustedProxiesConfig) {
        parseTrustedProxies(trustedProxiesConfig);
    }

    private void parseTrustedProxies(String config) {
        if (config == null || config.isBlank()) {
            return;
        }
        Arrays.stream(config.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(token -> {
                    try {
                        if (token.contains("/")) {
                            trustedProxyMatchers.add(new CidrMatcher(token));
                        } else {
                            trustedProxyMatchers.add(new ExactIpMatcher(token));
                        }
                    } catch (Exception ex) {
                        log.warn("Invalid trusted proxy configuration entry '{}': {}", token, ex.getMessage());
                    }
                });
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return "unknown";
        }

        remoteAddr = normalizeIp(remoteAddr);

        // 1. Direct client: if remoteAddr is NOT a trusted proxy, DO NOT trust headers!
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        // 2. Sender is a trusted proxy: inspect forwarding headers safely
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String resolved = extractFromForwardedFor(xff, remoteAddr);
            if (resolved != null && !resolved.isBlank()) {
                return resolved;
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            String cleaned = xRealIp.trim();
            if (isValidIp(cleaned)) {
                return normalizeIp(cleaned);
            }
        }

        return remoteAddr;
    }

    /**
     * Parses X-Forwarded-For right-to-left.
     * The immediate reverse proxy connects directly as remoteAddr.
     * Rightmost hops in X-Forwarded-For represent upstream intermediate proxies.
     * The first non-trusted IP encountered walking right-to-left is the client IP.
     */
    private String extractFromForwardedFor(String xff, String fallbackRemoteAddr) {
        String[] hops = xff.split(",");
        List<String> validHops = new ArrayList<>();

        for (String rawHop : hops) {
            String hop = rawHop.trim();
            if (hop.isEmpty()) {
                continue;
            }
            if (!isValidIp(hop)) {
                log.debug("Malformed IP '{}' in X-Forwarded-For header '{}'; falling back safely", hop, xff);
                return fallbackRemoteAddr;
            }
            validHops.add(normalizeIp(hop));
        }

        if (validHops.isEmpty()) {
            return fallbackRemoteAddr;
        }

        // Walk right-to-left
        for (int i = validHops.size() - 1; i >= 0; i--) {
            String hop = validHops.get(i);
            if (!isTrustedProxy(hop)) {
                return hop;
            }
        }

        // All hops in XFF are trusted proxies -> genuine client is the leftmost entry
        return validHops.get(0);
    }

    public boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        for (IpMatcher matcher : trustedProxyMatchers) {
            if (matcher.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private String normalizeIp(String ip) {
        try {
            return InetAddress.getByName(ip).getHostAddress();
        } catch (UnknownHostException e) {
            return ip.trim();
        }
    }

    interface IpMatcher {
        boolean matches(String ip);
    }

    static class ExactIpMatcher implements IpMatcher {
        private final String normalizedTarget;

        ExactIpMatcher(String ip) throws UnknownHostException {
            this.normalizedTarget = InetAddress.getByName(ip).getHostAddress();
        }

        @Override
        public boolean matches(String ip) {
            try {
                String normalized = InetAddress.getByName(ip).getHostAddress();
                return normalizedTarget.equalsIgnoreCase(normalized);
            } catch (UnknownHostException e) {
                return false;
            }
        }
    }

    static class CidrMatcher implements IpMatcher {
        private final byte[] networkBytes;
        private final int prefixLength;

        CidrMatcher(String cidr) throws UnknownHostException {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid CIDR format: " + cidr);
            }
            InetAddress baseAddress = InetAddress.getByName(parts[0].trim());
            this.networkBytes = baseAddress.getAddress();
            this.prefixLength = Integer.parseInt(parts[1].trim());
        }

        @Override
        public boolean matches(String ip) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                byte[] addressBytes = address.getAddress();

                if (addressBytes.length != networkBytes.length) {
                    return false; // IPv4 vs IPv6 mismatch
                }

                int fullBytes = prefixLength / 8;
                int remainingBits = prefixLength % 8;

                for (int i = 0; i < fullBytes; i++) {
                    if (addressBytes[i] != networkBytes[i]) {
                        return false;
                    }
                }

                if (remainingBits > 0) {
                    int mask = 0xFF << (8 - remainingBits);
                    return (addressBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
