package com.collabsphere_first.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class JwtCookieService {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/";

    @Value("${jwt.cookie.secure:#{null}}")
    private Boolean configuredSecure;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Value("${spring.profiles.default:prod}")
    private String defaultProfiles;

    public boolean determineSecure(HttpServletRequest request) {
        if (configuredSecure != null) {
            return configuredSecure;
        }

        String profiles = (activeProfiles != null && !activeProfiles.isBlank()) ? activeProfiles : defaultProfiles;
        boolean isDev = profiles != null && profiles.toLowerCase().contains("dev");

        if (request != null) {
            boolean isHttps = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
            if (isHttps) {
                return true;
            }
            if (!isDev) {
                // Production environment behind proxy or direct HTTPS must be Secure
                return true;
            }
            return false;
        }

        // When request is omitted or in production, enforce Secure cookies
        return !isDev;
    }

    public void addRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken) {
        addRefreshTokenCookie(null, response, refreshToken);
    }

    public void addRefreshTokenCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String refreshToken) {
        boolean secure = determineSecure(request);
        String sameSite = secure ? "None" : "Lax";

        ResponseCookie cookie = ResponseCookie.from(
                REFRESH_COOKIE_NAME,
                refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearRefreshTokenCookie(
            HttpServletResponse response) {
        clearRefreshTokenCookie(null, response);
    }

    public void clearRefreshTokenCookie(
            HttpServletRequest request,
            HttpServletResponse response) {
        boolean secure = determineSecure(request);
        String sameSite = secure ? "None" : "Lax";

        ResponseCookie cookie = ResponseCookie.from(
                REFRESH_COOKIE_NAME,
                "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String getRefreshToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        Object attr = request.getAttribute("refreshToken");
        if (attr instanceof String tokenStr && !tokenStr.isBlank()) {
            return tokenStr.trim();
        }

        String xRefreshToken = request.getHeader("X-Refresh-Token");
        if (xRefreshToken != null && !xRefreshToken.isBlank()) {
            return xRefreshToken.trim();
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        return null;
    }
}
