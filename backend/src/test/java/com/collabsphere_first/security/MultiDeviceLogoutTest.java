package com.collabsphere_first.security;

import com.collabsphere_first.auth.dto.request.LogoutRequest;
import com.collabsphere_first.auth.dto.response.MessageResponse;
import com.collabsphere_first.auth.service.AuthenticationServiceImpl;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.notification.exception.NotificationAccessDeniedException;
import com.collabsphere_first.notification.service.NotificationDeviceService;
import com.collabsphere_first.refreshtoken.entity.RefreshToken;
import com.collabsphere_first.refreshtoken.repository.RefreshTokenRepository;
import com.collabsphere_first.security.jwt.JwtCookieService;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Multi-Device Logout & Push Token Revocation Tests")
public class MultiDeviceLogoutTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private NotificationDeviceService notificationDeviceService;

    @Mock
    private MessagingPresenceService presenceService;

    @Mock
    private com.collabsphere_first.security.service.AccountRevocationCoordinator accountRevocationCoordinator;

    @Mock
    private JwtCookieService jwtCookieService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private User userPhoneAndTablet;
    private RefreshToken phoneRefreshToken;
    private RefreshToken tabletRefreshToken;
    private UUID phoneDeviceId;
    private UUID tabletDeviceId;
    private UUID otherUserDeviceId;

    @BeforeEach
    void setUp() {
        userPhoneAndTablet = User.builder()
                .id(100L)
                .email("multi@example.com")
                .accountEnabled(true)
                .build();

        phoneDeviceId = UUID.randomUUID();
        tabletDeviceId = UUID.randomUUID();
        otherUserDeviceId = UUID.randomUUID();

        phoneRefreshToken = RefreshToken.builder()
                .id(1L)
                .token("phone-refresh-token")
                .user(userPhoneAndTablet)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        tabletRefreshToken = RefreshToken.builder()
                .id(2L)
                .token("tablet-refresh-token")
                .user(userPhoneAndTablet)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(jwtCookieService.getRefreshToken(request)).thenReturn("phone-refresh-token");
        when(refreshTokenRepository.findByToken("phone-refresh-token")).thenReturn(Optional.of(phoneRefreshToken));
        when(refreshTokenRepository.findByToken("tablet-refresh-token")).thenReturn(Optional.of(tabletRefreshToken));
        when(userRepository.findById(100L)).thenReturn(Optional.of(userPhoneAndTablet));

        // Set authenticated user in SecurityContext
        UserPrincipal principal = new UserPrincipal(userPhoneAndTablet);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Single device logout on Phone revokes Phone refresh token and Phone push device; Tablet remains intact")
    void logout_phoneDevice_revokesPhoneSessionAndPhoneDeviceOnly() {
        LogoutRequest logoutRequest = LogoutRequest.builder()
                .deviceId(phoneDeviceId)
                .build();

        MessageResponse result = authenticationService.logout(request, response, logoutRequest);

        assertThat(result.getMessage()).isEqualTo("Logged out successfully.");

        // Phone refresh token deleted
        verify(refreshTokenRepository).delete(phoneRefreshToken);
        // Tablet refresh token NOT deleted
        verify(refreshTokenRepository, never()).delete(tabletRefreshToken);

        // Only Phone device revoked
        verify(notificationDeviceService).revokeDevice(eq(phoneDeviceId), eq(100L));
        verify(notificationDeviceService, never()).revokeDevice(eq(tabletDeviceId), anyLong());
        verify(notificationDeviceService, never()).revokeAllForUser(anyLong());

        // Cookie cleared
        verify(jwtCookieService).clearRefreshTokenCookie(response);
    }

    @Test
    @DisplayName("Legacy logout without body revokes current refresh token without revoking push devices")
    void logout_legacyWithoutBody_revokesCurrentSessionOnly() {
        MessageResponse result = authenticationService.logout(request, response, null);

        assertThat(result.getMessage()).isEqualTo("Logged out successfully.");

        verify(refreshTokenRepository).delete(phoneRefreshToken);
        verify(notificationDeviceService, never()).revokeDevice(any(), anyLong());
        verify(notificationDeviceService, never()).revokeAllForUser(anyLong());
        verify(jwtCookieService).clearRefreshTokenCookie(response);
    }

    @Test
    @DisplayName("Device ownership test: User A cannot revoke User B's deviceId during logout")
    void logout_attemptToRevokeOtherUserDevice_throwsAccessDenied() {
        LogoutRequest maliciousRequest = LogoutRequest.builder()
                .deviceId(otherUserDeviceId)
                .build();

        doThrow(new NotificationAccessDeniedException("You do not have permission to revoke this device"))
                .when(notificationDeviceService).revokeDevice(eq(otherUserDeviceId), eq(100L));

        assertThatThrownBy(() -> authenticationService.logout(request, response, maliciousRequest))
                .isInstanceOf(NotificationAccessDeniedException.class)
                .hasMessageContaining("permission");

        // Stored token was revoked, but device revocation failed as forbidden
        verify(notificationDeviceService).revokeDevice(eq(otherUserDeviceId), eq(100L));
    }

    @Test
    @DisplayName("Logout-all revokes all refresh tokens, revokes all push devices, and evicts presence")
    void logoutAll_revokesAllRefreshTokensAllDevicesAndPresence() {
        MessageResponse result = authenticationService.logoutAll(request, response);

        assertThat(result.getMessage()).isEqualTo("Logged out from all devices successfully.");

        // Revoke all refresh tokens for this user
        verify(refreshTokenRepository).deleteByUser(userPhoneAndTablet);

        // Revoke all notification devices for this user
        verify(notificationDeviceService).revokeAllForUser(100L);

        // Presence evicted
        verify(presenceService).forceOffline(100L);

        // Revocation coordinator invoked
        verify(accountRevocationCoordinator).revokeAccountSessions(eq(100L), anyString());

        // Cookie cleared
        verify(jwtCookieService).clearRefreshTokenCookie(response);
    }

    @Test
    @DisplayName("Logout-all without authenticated user or refresh token throws AccessDeniedException")
    void logoutAll_unauthenticated_throwsAccessDenied() {
        SecurityContextHolder.clearContext();
        when(jwtCookieService.getRefreshToken(request)).thenReturn(null);

        assertThatThrownBy(() -> authenticationService.logoutAll(request, response))
                .isInstanceOf(AccessDeniedException.class);

        verify(refreshTokenRepository, never()).deleteByUser(any());
        verify(notificationDeviceService, never()).revokeAllForUser(any());
    }
}
