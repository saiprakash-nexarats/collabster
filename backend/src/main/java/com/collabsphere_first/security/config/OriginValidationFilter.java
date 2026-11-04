package com.collabsphere_first.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates Origin header on state-changing authentication requests to prevent CSRF.
 */
@Slf4j
public class OriginValidationFilter extends OncePerRequestFilter {

    private final List<String> allowedOrigins;

    public OriginValidationFilter(String allowedOriginsConfig) {
        if (allowedOriginsConfig != null && !allowedOriginsConfig.isBlank()) {
            this.allowedOrigins = Arrays.stream(allowedOriginsConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } else {
            this.allowedOrigins = List.of("http://localhost:3000", "http://localhost:5173", "http://localhost:8080");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isAuthEndpoint = path != null && (path.startsWith("/api/v1/auth/") || path.startsWith("/v1/auth/"));
        boolean isStateChanging = "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);

        if (isAuthEndpoint && isStateChanging) {
            String origin = request.getHeader("Origin");
            if (origin != null && !origin.isBlank()) {
                if (!isOriginAllowed(origin)) {
                    log.warn("Blocked state-changing auth request from untrusted origin: {} on path {}", origin, path);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Cross-origin request rejected: unauthorized origin\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isOriginAllowed(String origin) {
        if (allowedOrigins.contains("*")) {
            return true;
        }
        for (String allowed : allowedOrigins) {
            if (allowed.equalsIgnoreCase(origin)) {
                return true;
            }
            if (allowed.contains("*")) {
                String regex = allowed.replace(".", "\\.").replace("*", ".*");
                if (origin.matches(regex)) {
                    return true;
                }
            }
        }
        return false;
    }
}

