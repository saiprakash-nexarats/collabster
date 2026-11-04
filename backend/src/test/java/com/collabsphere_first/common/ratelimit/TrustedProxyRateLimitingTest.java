package com.collabsphere_first.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Trusted Proxy & Rate Limiting Spoofing Protection Tests")
public class TrustedProxyRateLimitingTest {

    private ClientIpResolver resolver;
    private PlatformRateLimiter rateLimiter;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisProvider;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        // Configure trusted proxies: loopback and private 10.0.0.0/8 CIDR
        resolver = new ClientIpResolver("127.0.0.1,::1,10.0.0.0/8,172.20.0.1");
        rateLimiter = new PlatformRateLimiter(redisProvider, resolver);
    }

    @Test
    @DisplayName("Direct untrusted client cannot spoof IP via X-Forwarded-For header")
    void resolveClientIp_untrustedClient_ignoresForwardedFor() {
        when(request.getRemoteAddr()).thenReturn("198.51.100.25");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");

        String resolvedIp = resolver.resolveClientIp(request);

        // Attacker's real remoteAddr is used, spoofed header is ignored
        assertThat(resolvedIp).isEqualTo("198.51.100.25");
    }

    @Test
    @DisplayName("Direct untrusted client cannot spoof IP via X-Real-IP header")
    void resolveClientIp_untrustedClient_ignoresXRealIp() {
        when(request.getRemoteAddr()).thenReturn("198.51.100.25");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("8.8.8.8");

        String resolvedIp = resolver.resolveClientIp(request);

        assertThat(resolvedIp).isEqualTo("198.51.100.25");
    }

    @Test
    @DisplayName("Trusted proxy correctly extracts client IP from X-Forwarded-For")
    void resolveClientIp_trustedProxy_extractsClientIp() {
        when(request.getRemoteAddr()).thenReturn("10.0.1.50"); // within 10.0.0.0/8
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195");

        String resolvedIp = resolver.resolveClientIp(request);

        assertThat(resolvedIp).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("Multi-hop trusted proxy chain resolves genuine client by walking right-to-left")
    void resolveClientIp_multiHopProxyChain_resolvesFirstUntrustedHop() {
        // Client (203.0.113.195) -> Intermediate Proxy (10.0.2.1) -> Edge Proxy (10.0.1.50) -> Collobster
        when(request.getRemoteAddr()).thenReturn("10.0.1.50");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 10.0.2.1");

        String resolvedIp = resolver.resolveClientIp(request);

        assertThat(resolvedIp).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("Malformed X-Forwarded-For does not crash and safely falls back to remoteAddr")
    void resolveClientIp_malformedHeader_fallsBackSafely() {
        when(request.getRemoteAddr()).thenReturn("10.0.1.50");
        when(request.getHeader("X-Forwarded-For")).thenReturn("garbage-not-an-ip, <script>alert(1)</script>");

        String resolvedIp = resolver.resolveClientIp(request);

        assertThat(resolvedIp).isEqualTo("10.0.1.50");
    }

    @Test
    @DisplayName("Rate-limit abuse key prioritizes authenticated user ID over IP")
    void resolveAbuseKey_authenticatedUser_prioritizesUserId() {
        when(request.getRemoteAddr()).thenReturn("198.51.100.25");

        String abuseKey = rateLimiter.resolveAbuseKey(request, 999L);

        // Uses user:999, rendering IP rotation ineffective for bypassing limits
        assertThat(abuseKey).isEqualTo("user:999");
    }

    @Test
    @DisplayName("Rate-limit abuse key falls back to resolved IP for unauthenticated requests")
    void resolveAbuseKey_unauthenticated_usesResolvedIp() {
        when(request.getRemoteAddr()).thenReturn("198.51.100.25");

        String abuseKey = rateLimiter.resolveAbuseKey(request, null);

        assertThat(abuseKey).isEqualTo("ip:198.51.100.25");
    }

    @Test
    @DisplayName("PlatformRateLimiter enforces rate limit and throws PlatformRateLimitExceededException")
    void checkRequestRate_exceeded_throwsPlatformRateLimitExceededException() {
        when(request.getRemoteAddr()).thenReturn("198.51.100.25");

        // Allow 2 requests per minute
        rateLimiter.checkRequestRate("test-action", request, null, 2, Duration.ofMinutes(1));
        rateLimiter.checkRequestRate("test-action", request, null, 2, Duration.ofMinutes(1));

        // 3rd request throws
        assertThatThrownBy(() ->
                rateLimiter.checkRequestRate("test-action", request, null, 2, Duration.ofMinutes(1))
        ).isInstanceOf(PlatformRateLimitExceededException.class);
    }
}
