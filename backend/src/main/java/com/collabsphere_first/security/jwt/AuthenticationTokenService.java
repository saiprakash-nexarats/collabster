package com.collabsphere_first.security.jwt;

import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.collabsphere_first.refreshtoken.entity.RefreshToken;
import com.collabsphere_first.refreshtoken.repository.RefreshTokenRepository;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import com.collabsphere_first.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticationTokenService {

    private final JwtService jwtService;
    private final JwtCookieService jwtCookieService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthenticationResponse authenticateUser(
            User user,
            HttpServletResponse response) {

        UserPrincipal userPrincipal = new UserPrincipal(user);

        String accessToken =
                jwtService.generateAccessToken(userPrincipal);

        String refreshToken =
                jwtService.generateRefreshToken(userPrincipal);

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByUser(user)
                .map(existingToken -> {
                    existingToken.setToken(refreshToken);
                    existingToken.setExpiresAt(LocalDateTime.now().plusDays(7));
                    existingToken.setRevoked(false);
                    return existingToken;
                })
                .orElseGet(() -> RefreshToken.builder()
                        .token(refreshToken)
                        .user(user)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .revoked(false)
                        .build());

        refreshTokenRepository.save(refreshTokenEntity);

        jwtCookieService.addRefreshTokenCookie(
                response,
                refreshToken
        );

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .displayName(((user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "")).trim())
                .username(user.getUsername())
                .userId(user.getId())
                .profileId(user.getId())
                .build();
    }
}
