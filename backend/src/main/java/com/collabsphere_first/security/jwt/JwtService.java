package com.collabsphere_first.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(UserDetails userDetails) {
        Long authVersion = 0L;
        if (userDetails instanceof com.collabsphere_first.security.userdetails.UserPrincipal up && up.getAuthVersion() != null) {
            authVersion = up.getAuthVersion();
        }

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("token_type", "ACCESS")
                .claim("auth_version", authVersion)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()
                        + jwtProperties.getAccessTokenExpiration()))
                .signWith(secretKey)
                .compact();
    }

    public String generateLegacyAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("token_type", "ACCESS")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()
                        + jwtProperties.getAccessTokenExpiration()))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Long authVersion = 0L;
        if (userDetails instanceof com.collabsphere_first.security.userdetails.UserPrincipal up && up.getAuthVersion() != null) {
            authVersion = up.getAuthVersion();
        }

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("token_type", "REFRESH")
                .claim("auth_version", authVersion)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()
                        + jwtProperties.getRefreshTokenExpiration()))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        try {
            return extractClaim(token, claims -> claims.get("token_type", String.class));
        } catch (Exception ex) {
            return null;
        }
    }

    public Long extractAuthVersion(String token) {
        try {
            return extractClaim(token, claims -> {
                Object val = claims.get("auth_version");
                if (val instanceof Number n) {
                    return n.longValue();
                }
                return null;
            });
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean isAccessToken(String token) {
        String tokenType = extractTokenType(token);
        return !"REFRESH".equalsIgnoreCase(tokenType);
    }

    public boolean isRefreshToken(String token) {
        String tokenType = extractTokenType(token);
        return "REFRESH".equalsIgnoreCase(tokenType);
    }

    public boolean isTokenValid(String token,
                                UserDetails userDetails) {

        if (!isAccessToken(token)) {
            return false;
        }

        if (userDetails == null || !userDetails.isEnabled()) {
            return false;
        }

        String username = extractUsername(token);

        if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }

        Long tokenAuthVersion = extractAuthVersion(token);
        long tokenVersion = (tokenAuthVersion != null) ? tokenAuthVersion : 0L;
        if (userDetails instanceof com.collabsphere_first.security.userdetails.UserPrincipal up) {
            long currentAuthVersion = (up.getAuthVersion() != null) ? up.getAuthVersion() : 0L;
            if (tokenVersion != currentAuthVersion) {
                return false;
            }
        }

        return true;
    }

    public boolean isTokenExpired(String token) {

        return extractExpiration(token)
                .before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public java.time.Instant extractExpirationInstant(String token) {
        Date expiration = extractExpiration(token);
        return expiration != null ? expiration.toInstant() : null;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
