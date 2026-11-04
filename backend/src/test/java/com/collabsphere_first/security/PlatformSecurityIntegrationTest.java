package com.collabsphere_first.security;

import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.role.repository.RoleRepository;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import com.collabsphere_first.refreshtoken.entity.RefreshToken;
import com.collabsphere_first.refreshtoken.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/collabster",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@DisplayName("Platform Security Filter Chain & Header Hardening Tests")
public class PlatformSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private User testUser;
    private User deactivatedUser;

    @BeforeEach
    void setUp() {
        Role studentRole = roleRepository.findByName(RoleName.ROLE_STUDENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_STUDENT).build()));

        testUser = userRepository.findByEmail("security_test_active@collobster.com")
                .orElseGet(() -> {
                    User u = User.builder()
                            .firstName("Security")
                            .lastName("Active")
                            .email("security_test_active@collobster.com")
                            .username("sec_test_active")
                            .password("$2a$10$wN1iN61mNnN7hU0Xq7hHxe7iN1o0yq5qH6Q/lFjT0O9dO4zN7B3.K")
                            .provider(AuthProvider.LOCAL)
                            .emailVerified(true)
                            .accountEnabled(true)
                            .deactivated(false)
                            .deleted(false)
                            .roles(Set.of(studentRole))
                            .build();
                    return userRepository.save(u);
                });

        transactionTemplate.execute(status -> {
            testUser.setAuthVersion(0L);
            testUser.setAccountEnabled(true);
            testUser.setDeactivated(false);
            testUser.setDeleted(false);
            testUser = userRepository.save(testUser);
            return null;
        });

        deactivatedUser = userRepository.findByEmail("security_test_deactivated@collobster.com")
                .orElseGet(() -> {
                    User u = User.builder()
                            .firstName("Security")
                            .lastName("Deactivated")
                            .email("security_test_deactivated@collobster.com")
                            .username("sec_test_deact")
                            .password("$2a$10$wN1iN61mNnN7hU0Xq7hHxe7iN1o0yq5qH6Q/lFjT0O9dO4zN7B3.K")
                            .provider(AuthProvider.LOCAL)
                            .emailVerified(true)
                            .accountEnabled(false)
                            .deactivated(true)
                            .deleted(false)
                            .roles(Set.of(studentRole))
                            .build();
                    return userRepository.save(u);
                });
    }

    @Test
    @DisplayName("Security headers (X-Frame-Options, X-Content-Type-Options, Referrer-Policy) must be present on responses")
    void testSecurityHeadersEnforced() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid@test.com\",\"password\":\"wrong\"}"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    @DisplayName("Protected endpoints must reject unauthenticated requests")
    void testProtectedEndpointRejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/feed"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/v1/platform/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/v1/account/deactivate"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Refresh tokens must NOT be accepted as Bearer access tokens on protected routes")
    void testRefreshTokenRejectedAsAccessToken() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(testUser.getEmail());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Tokens belonging to deactivated accounts must be rejected")
    void testDeactivatedUserTokenRejected() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(deactivatedUser.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Stale access token rejected after authVersion increment; new token succeeds")
    void testStaleAccessTokenRejectedAfterLogoutAll() throws Exception {
        var userDetailsV0 = userDetailsService.loadUserByUsername(testUser.getEmail());
        String tokenV0 = jwtService.generateAccessToken(userDetailsV0);

        // Access token with matching authVersion works
        mockMvc.perform(get("/api/v1/notification-center/unread-count")
                        .header("Authorization", "Bearer " + tokenV0))
                .andExpect(status().isOk());

        // Increment authVersion in database (simulating logoutAll or revocation)
        transactionTemplate.execute(status -> {
            userRepository.incrementAuthVersion(testUser.getId());
            return null;
        });

        // Old token with outdated authVersion must be rejected
        mockMvc.perform(get("/api/v1/notification-center/unread-count")
                        .header("Authorization", "Bearer " + tokenV0))
                .andExpect(status().is4xxClientError());

        // New login/mint produces token with incremented authVersion
        var userDetailsV1 = userDetailsService.loadUserByUsername(testUser.getEmail());
        String tokenV1 = jwtService.generateAccessToken(userDetailsV1);

        // New token succeeds
        mockMvc.perform(get("/api/v1/notification-center/unread-count")
                        .header("Authorization", "Bearer " + tokenV1))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Stale refresh token rejected after authVersion increment")
    void testStaleRefreshTokenRejectedAfterLogoutAll() throws Exception {
        var userDetailsV0 = userDetailsService.loadUserByUsername(testUser.getEmail());
        String refreshV0 = jwtService.generateRefreshToken(userDetailsV0);

        transactionTemplate.execute(status -> {
            refreshTokenRepository.findByUser(testUser).ifPresent(existing -> {
                refreshTokenRepository.delete(existing);
                refreshTokenRepository.flush();
            });
            RefreshToken rt = RefreshToken.builder()
                    .token(refreshV0)
                    .user(testUser)
                    .revoked(false)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            refreshTokenRepository.saveAndFlush(rt);
            return null;
        });

        // Increment authVersion
        transactionTemplate.execute(status -> {
            userRepository.incrementAuthVersion(testUser.getId());
            return null;
        });

        // Attempting to refresh with old refresh token must be rejected with 4xx
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .header("Authorization", "Bearer " + refreshV0))
                .andExpect(status().is4xxClientError());

        // Assert that the outdated refresh token was deleted from database
        assertThat(refreshTokenRepository.findByToken(refreshV0)).isEmpty();
    }

    @Test
    @DisplayName("Legacy token without auth_version claim accepted at version 0, rejected at version > 0")
    void testLegacyTokenWithoutAuthVersionClaim() throws Exception {
        transactionTemplate.execute(status -> {
            testUser.setAuthVersion(0L);
            userRepository.save(testUser);
            return null;
        });

        var userDetailsV0 = userDetailsService.loadUserByUsername(testUser.getEmail());
        String legacyToken = jwtService.generateLegacyAccessToken(userDetailsV0);

        // Accepted when user authVersion is 0
        mockMvc.perform(get("/api/v1/notification-center/unread-count")
                        .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isOk());

        // Increment user authVersion to 1
        transactionTemplate.execute(status -> {
            userRepository.incrementAuthVersion(testUser.getId());
            return null;
        });

        // Legacy token must now be rejected - no null bypass
        mockMvc.perform(get("/api/v1/notification-center/unread-count")
                        .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Tokens belonging to suspended or disabled accounts must be rejected")
    void testSuspendedAccountRejected() throws Exception {
        transactionTemplate.execute(status -> {
            testUser.setAccountEnabled(false);
            userRepository.save(testUser);
            return null;
        });

        var userDetails = userDetailsService.loadUserByUsername(testUser.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        mockMvc.perform(get("/api/v1/notification-center/unread-count")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().is4xxClientError());
    }
}
