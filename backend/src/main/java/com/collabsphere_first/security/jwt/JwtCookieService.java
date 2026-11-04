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

    @Value("${jwt.cookie.secure:false}")
    private boolean defaultSecure;

    private boolean determineSecure(HttpServletRequest request) {
        if (defaultSecure) {
            return true;
        }
        if (request == null) {
            return false;
        }
        if (request.isSecure()) {
            return true;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(proto);
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
                        refreshToken
                )
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
                        ""
                )
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
