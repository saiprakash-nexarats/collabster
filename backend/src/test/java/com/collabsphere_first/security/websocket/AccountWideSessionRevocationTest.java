package com.collabsphere_first.security.websocket;

import com.collabsphere_first.account.service.AccountLifecycleService;
import com.collabsphere_first.account.service.AccountLifecycleServiceImpl;
import com.collabsphere_first.auth.dto.request.LogoutRequest;
import com.collabsphere_first.auth.service.AuthenticationServiceImpl;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.notification.service.NotificationDeviceService;
import com.collabsphere_first.refreshtoken.repository.RefreshTokenRepository;
import com.collabsphere_first.report.service.PlatformAuditLogService;
import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.security.event.AccountRevocationEvent;
import com.collabsphere_first.security.event.AccountRevocationListener;
import com.collabsphere_first.security.jwt.JwtCookieService;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.service.AccountRevocationCoordinator;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Account-Wide Realtime Session Revocation & Multi-Node Fallback Tests")
public class AccountWideSessionRevocationTest {

    private WebSocketAuthenticatedSessionRegistry sessionRegistryNodeA;
    private WebSocketAuthenticatedSessionRegistry sessionRegistryNodeB;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private NotificationDeviceService notificationDeviceService;

    @Mock
    private MessagingPresenceService presenceService;

    @Mock
    private PlatformAuditLogService auditLogService;

    @Mock
    private com.collabsphere_first.account.erasure.service.UserDataErasureOutboxService erasureOutboxService;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtCookieService jwtCookieService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Mock
    private MessageChannel messageChannel;

    @Mock
    private WebSocketSession phoneTransportSession;

    @Mock
    private WebSocketSession desktopTransportSession;

    @Mock
    private WebSocketSession tabletTransportSession;

    private ObjectMapper objectMapper;
    private AccountRevocationCoordinator revocationCoordinatorNodeA;
    private AccountRevocationListener revocationListenerNodeB;
    private WebSocketOutboundExpiryInterceptor outboundInterceptor;
    private WebSocketSessionReaper sessionReaperNodeB;

    private Long testUserId = 42L;
    private User testUser;
    private User adminUser;
    private long currentAuthVersion = 0L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sessionRegistryNodeA = new WebSocketAuthenticatedSessionRegistry();
        sessionRegistryNodeB = new WebSocketAuthenticatedSessionRegistry();

        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);

        currentAuthVersion = 0L;
        testUser = User.builder()
                .id(testUserId)
                .email("user@collobster.com")
                .username("testuser")
                .accountEnabled(true)
                .deactivated(false)
                .deleted(false)
                .authVersion(currentAuthVersion)
                .build();

        Role adminRole = Role.builder().id(1L).name(RoleName.ROLE_ADMIN).build();
        adminUser = User.builder()
                .id(999L)
                .email("admin@collobster.com")
                .username("admin")
                .roles(Set.of(adminRole))
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.findAuthVersionByUserId(testUserId)).thenAnswer(inv -> Optional.of(currentAuthVersion));

        doAnswer(inv -> {
            currentAuthVersion++;
            testUser.setAuthVersion(currentAuthVersion);
            return 1;
        }).when(userRepository).incrementAuthVersion(testUserId);

        when(phoneTransportSession.isOpen()).thenReturn(true);
        when(desktopTransportSession.isOpen()).thenReturn(true);
        when(tabletTransportSession.isOpen()).thenReturn(true);

        revocationCoordinatorNodeA = new AccountRevocationCoordinator(
                userRepository,
                sessionRegistryNodeA,
                presenceService,
                redisTemplateProvider,
                objectMapper
        );

        revocationListenerNodeB = new AccountRevocationListener(
                sessionRegistryNodeB,
                presenceService,
                objectMapper
        );

        outboundInterceptor = new WebSocketOutboundExpiryInterceptor(
                sessionRegistryNodeA,
                presenceService
        );

        sessionReaperNodeB = new WebSocketSessionReaper(
                sessionRegistryNodeB,
                presenceService,
                userRepository
        );
    }

    private void registerTestSession(WebSocketAuthenticatedSessionRegistry registry, String sessionId, Long userId, Instant expiresAt, Long authVersion, WebSocketSession transport) {
        registry.registerTransportSession(sessionId, transport);
        registry.registerSession(sessionId, userId, "testuser", expiresAt, authVersion, "/ws/messages");
    }

    @Test
    @DisplayName("20. Multi-Device Logout-All terminates all active sessions, clears presence, and revokes tokens")
    void logoutAll_terminatesAllActiveDevicesAndEvictsPresence() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);

        // Register 3 active sessions across devices on Node A
        registerTestSession(sessionRegistryNodeA, "session-phone", testUserId, futureExpiry, 0L, phoneTransportSession);
        registerTestSession(sessionRegistryNodeA, "session-desktop", testUserId, futureExpiry, 0L, desktopTransportSession);
        registerTestSession(sessionRegistryNodeA, "session-tablet", testUserId, futureExpiry, 0L, tabletTransportSession);

        assertThat(sessionRegistryNodeA.getActiveSessionsForUser(testUserId)).hasSize(3);

        // Perform logout-all revocation
        revocationCoordinatorNodeA.revokeAccountSessions(testUserId, "Logged out from all devices");

        // Assert database authVersion incremented
        assertThat(currentAuthVersion).isEqualTo(1L);

        // Assert all local transports closed
        verify(phoneTransportSession).close(any(CloseStatus.class));
        verify(desktopTransportSession).close(any(CloseStatus.class));
        verify(tabletTransportSession).close(any(CloseStatus.class));

        // Assert all sessions marked revoked
        assertThat(sessionRegistryNodeA.isRevoked("session-phone")).isTrue();
        assertThat(sessionRegistryNodeA.isRevoked("session-desktop")).isTrue();
        assertThat(sessionRegistryNodeA.isRevoked("session-tablet")).isTrue();
        assertThat(sessionRegistryNodeA.getActiveSessionsForUser(testUserId)).isEmpty();

        // Assert presence evicted
        verify(presenceService).forceOffline(testUserId);

        // Assert Redis publication occurred
        verify(redisTemplate).convertAndSend(eq(AccountRevocationCoordinator.REVOCATION_CHANNEL), any(String.class));
    }

    @Test
    @DisplayName("21. Deactivation immediately invalidates REST tokens and terminates realtime sessions")
    void accountDeactivation_terminatesRealtimeSessionsAndBlocksOutbound() throws Exception {
        AccountLifecycleService service = new AccountLifecycleServiceImpl(
                userRepository,
                refreshTokenRepository,
                notificationDeviceService,
                presenceService,
                auditLogService,
                erasureOutboxService,
                revocationCoordinatorNodeA
        );

        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeA, "session-deact", testUserId, futureExpiry, 0L, phoneTransportSession);

        // Deactivate account
        service.deactivateAccount(testUserId);

        assertThat(testUser.getDeactivated()).isTrue();
        assertThat(testUser.getAccountEnabled()).isFalse();
        assertThat(currentAuthVersion).isEqualTo(1L);

        // Session closed and marked revoked
        verify(phoneTransportSession).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeA.isRevoked("session-deact")).isTrue();

        // Outbound interceptor must drop frames for revoked session
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
        accessor.setSessionId("session-deact");
        accessor.setDestination("/queue/messages");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = outboundInterceptor.preSend(message, messageChannel);
        assertThat(result).isNull(); // Dropped!
    }

    @Test
    @DisplayName("22. Account deletion terminates realtime session, increments authVersion, and enqueues erasure job")
    void accountDeletion_terminatesRealtimeSessionsAndEnqueuesDownstreamErasure() throws Exception {
        AccountLifecycleService service = new AccountLifecycleServiceImpl(
                userRepository,
                refreshTokenRepository,
                notificationDeviceService,
                presenceService,
                auditLogService,
                erasureOutboxService,
                revocationCoordinatorNodeA
        );

        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeA, "session-del", testUserId, futureExpiry, 0L, phoneTransportSession);

        service.deleteAccount(testUserId);

        assertThat(testUser.getDeleted()).isTrue();
        assertThat(currentAuthVersion).isEqualTo(1L);
        verify(phoneTransportSession).close(any(CloseStatus.class));
        verify(erasureOutboxService).enqueueErasureJob(testUserId, "RECOMMENDATION", "ERASE");
    }

    @Test
    @DisplayName("23. Admin suspension terminates target user's active WebSocket sessions")
    void adminSuspension_terminatesTargetUserSessions() throws Exception {
        AccountLifecycleService service = new AccountLifecycleServiceImpl(
                userRepository,
                refreshTokenRepository,
                notificationDeviceService,
                presenceService,
                auditLogService,
                erasureOutboxService,
                revocationCoordinatorNodeA
        );

        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeA, "session-suspend", testUserId, futureExpiry, 0L, phoneTransportSession);

        service.suspendAccount(adminUser.getId(), testUserId, "Terms violation");

        assertThat(testUser.getDeactivated()).isTrue();
        assertThat(currentAuthVersion).isEqualTo(1L);
        verify(phoneTransportSession).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeA.isRevoked("session-suspend")).isTrue();
    }

    @Test
    @DisplayName("24. Normal device logout does NOT increment global authVersion and preserves sibling sessions")
    void normalDeviceLogout_preservesSiblingDeviceSessions() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeA, "phone-session", testUserId, futureExpiry, 0L, phoneTransportSession);
        registerTestSession(sessionRegistryNodeA, "desktop-session", testUserId, futureExpiry, 0L, desktopTransportSession);

        // Normal single-device logout on phone
        AuthenticationServiceImpl authService = new AuthenticationServiceImpl(
                null, userRepository, null, null, refreshTokenRepository, null, null,
                jwtService, null, jwtCookieService, notificationDeviceService, presenceService,
                revocationCoordinatorNodeA
        );

        LogoutRequest logoutReq = new LogoutRequest();
        logoutReq.setDeviceId(UUID.randomUUID());

        authService.logout(null, null, logoutReq);

        // Verify account-wide authVersion was NOT incremented
        assertThat(currentAuthVersion).isEqualTo(0L);
        verify(userRepository, never()).incrementAuthVersion(any());

        // Verify phone and desktop WebSocket sessions remain active
        assertThat(sessionRegistryNodeA.isRevoked("phone-session")).isFalse();
        assertThat(sessionRegistryNodeA.isRevoked("desktop-session")).isFalse();
        verify(phoneTransportSession, never()).close(any(CloseStatus.class));
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("25. Cross-node invalidation: Node A publishes revocation, Node B listener terminates local sessions")
    void crossNodeRevocation_nodeBTerminatesLocalSessionsOnRedisEvent() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        // User has active session connected to Node B
        registerTestSession(sessionRegistryNodeB, "nodeB-session", testUserId, futureExpiry, 0L, desktopTransportSession);
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).hasSize(1);

        // Node A triggers revocation
        AccountRevocationEvent event = new AccountRevocationEvent(testUserId, 1L, "Logged out from all devices", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(event);

        // Node B receives message via Redis listener
        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload), null);

        // Node B local session must be terminated
        verify(desktopTransportSession).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-session")).isTrue();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).isEmpty();
    }

    @Test
    @DisplayName("26. Pub/Sub failure test: missed message caught by durable reconciliation reaper within scheduled interval")
    void pubSubFailure_durableReconciliationReaperTerminatesStaleSession() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        // Node B has active session with authVersion=0L
        registerTestSession(sessionRegistryNodeB, "nodeB-stale-session", testUserId, futureExpiry, 0L, desktopTransportSession);

        // DB authVersion has already advanced to 1L (e.g. pub/sub packet dropped)
        List<Object[]> authStates = List.<Object[]>of(new Object[]{testUserId, 1L, true, false, false});
        when(userRepository.findAuthStatesByUserIds(any())).thenReturn(authStates);

        // Scheduled reaper runs on Node B
        sessionReaperNodeB.reapRevokedSessions();

        // Reaper must discover session.authVersion (0L) < db.authVersion (1L) and terminate session
        verify(desktopTransportSession).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-stale-session")).isTrue();
    }

    @Test
    @DisplayName("27. Redis outage: revocation coordinator succeeds atomically in DB, terminates local sessions, and tolerates Redis failure")
    void redisOutage_durableIncrementSucceedsAndLocalSessionsClose() throws Exception {
        // Simulate Redis unavailable
        when(redisTemplateProvider.getIfAvailable()).thenReturn(null);

        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeA, "session-local", testUserId, futureExpiry, 0L, phoneTransportSession);

        // Revocation must not throw exception
        revocationCoordinatorNodeA.revokeAccountSessions(testUserId, "Account suspended during Redis outage");

        // DB increment succeeded
        assertThat(currentAuthVersion).isEqualTo(1L);

        // Local sessions closed immediately
        verify(phoneTransportSession).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeA.isRevoked("session-local")).isTrue();
    }

    @Test
    @DisplayName("Token auth_version validation: old token rejected after authVersion increment")
    void tokenAuthVersionMismatch_rejectsTokenEvenIfUnexpired() {
        UserPrincipal principal = new UserPrincipal(testUser); // authVersion = 0

        // Mock token claims with auth_version = 0
        when(jwtService.extractUsername("token-v0")).thenReturn("user@collobster.com");
        when(jwtService.extractTokenType("token-v0")).thenReturn("ACCESS");
        when(jwtService.isAccessToken("token-v0")).thenReturn(true);
        when(jwtService.extractAuthVersion("token-v0")).thenReturn(0L);
        when(jwtService.isTokenExpired("token-v0")).thenReturn(false);
        when(jwtService.isTokenValid(eq("token-v0"), any())).thenCallRealMethod();

        assertThat(jwtService.isTokenValid("token-v0", principal)).isTrue();

        // Advance authVersion to 1
        testUser.setAuthVersion(1L);
        UserPrincipal updatedPrincipal = new UserPrincipal(testUser);

        // Same token-v0 is now invalid!
        assertThat(jwtService.isTokenValid("token-v0", updatedPrincipal)).isFalse();
    }

    @Test
    @DisplayName("28. Out-of-order event test: stale event (authVersion=4) does not close newer session (authVersion=5)")
    void staleRemoteEvent_doesNotCloseNewerSession_andPresenceRemains() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        // Node B has active session with authVersion=5L
        registerTestSession(sessionRegistryNodeB, "nodeB-session-v5", testUserId, futureExpiry, 5L, desktopTransportSession);

        // Node B receives stale event with authVersion=4L
        AccountRevocationEvent staleEvent = new AccountRevocationEvent(testUserId, 4L, "Delayed event", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(staleEvent);

        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload), null);

        // Newer session must NOT be closed or marked revoked
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-session-v5")).isFalse();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).hasSize(1);

        // Presence must NOT be touched
        verify(presenceService, never()).forceOffline(any());
        verify(presenceService, never()).handleSessionDisconnected(any(), any());
    }

    @Test
    @DisplayName("29. Current event test: event (authVersion=5) closes outdated session (authVersion=4) with 4401 and prunes presence")
    void currentRemoteEvent_closesOutdatedSession_andPrunesPresence() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        // Node B has active session with authVersion=4L
        registerTestSession(sessionRegistryNodeB, "nodeB-session-v4", testUserId, futureExpiry, 4L, desktopTransportSession);

        // Node B receives event with authVersion=5L
        AccountRevocationEvent event = new AccountRevocationEvent(testUserId, 5L, "Security revocation", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(event);

        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload), null);

        // Outdated session must be closed with status 4401
        verify(desktopTransportSession).close(argThat(status -> status.getCode() == 4401));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-session-v4")).isTrue();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).isEmpty();

        // Presence must be removed specifically for the closed session, NOT forceOffline
        verify(presenceService).handleSessionDisconnected(testUserId, "nodeB-session-v4");
        verify(presenceService, never()).forceOffline(testUserId);
    }

    @Test
    @DisplayName("30. Duplicate event test: duplicate event delivery is idempotent and causes no errors or duplicate close")
    void duplicateRemoteEvent_isIdempotent() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeB, "nodeB-dup-session", testUserId, futureExpiry, 4L, desktopTransportSession);

        AccountRevocationEvent event = new AccountRevocationEvent(testUserId, 5L, "Duplicate test", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(event);

        // First delivery
        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload), null);
        verify(desktopTransportSession, times(1)).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-dup-session")).isTrue();

        // Second duplicate delivery
        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload), null);
        // Transport close should not be invoked a second time
        verify(desktopTransportSession, times(1)).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-dup-session")).isTrue();
    }

    @Test
    @DisplayName("31. New login after logout-all test: new session (v8) survives delayed event (v8)")
    void newLoginAfterLogoutAll_survivesDelayedEvent() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);

        // User establishes new session with authVersion=8L after logout-all
        registerTestSession(sessionRegistryNodeB, "nodeB-new-session-v8", testUserId, futureExpiry, 8L, desktopTransportSession);

        // Delayed event with authVersion=8L arrives
        AccountRevocationEvent delayedEvent = new AccountRevocationEvent(testUserId, 8L, "Delayed logout-all", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(delayedEvent);

        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload), null);

        // Session version 8 must NOT be closed because sessionVersion (8) < minVersion (8) is false!
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-new-session-v8")).isFalse();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).hasSize(1);
    }

    @Test
    @DisplayName("32. Newer revocation test: later logout-all (v9) closes session (v8)")
    void newerRevocation_closesSession() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeB, "nodeB-session-v8", testUserId, futureExpiry, 8L, desktopTransportSession);

        // Later event with authVersion=9L arrives
        AccountRevocationEvent newerEvent = new AccountRevocationEvent(testUserId, 9L, "Subsequent logout-all", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(newerEvent);

        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload), null);

        // Session version 8 is closed because 8 < 9 is true
        verify(desktopTransportSession).close(argThat(status -> status.getCode() == 4401));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-session-v8")).isTrue();
    }

    @Test
    @DisplayName("33. Mixed session versions: only sessions older than event version are closed")
    void mixedSessionVersions_closesOnlyStaleSessions() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);

        // Session A with version 4
        registerTestSession(sessionRegistryNodeB, "session-v4", testUserId, futureExpiry, 4L, phoneTransportSession);
        // Session B with version 5
        registerTestSession(sessionRegistryNodeB, "session-v5", testUserId, futureExpiry, 5L, desktopTransportSession);

        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).hasSize(2);

        // Event version 5 arrives
        AccountRevocationEvent event = new AccountRevocationEvent(testUserId, 5L, "Selective close", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(event);

        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload), null);

        // Session A (v4) closed, Session B (v5) remains
        verify(phoneTransportSession).close(argThat(status -> status.getCode() == 4401));
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));

        assertThat(sessionRegistryNodeB.isRevoked("session-v4")).isTrue();
        assertThat(sessionRegistryNodeB.isRevoked("session-v5")).isFalse();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId))
                .extracting(WebSocketSessionMetadata::getSessionId)
                .containsExactly("session-v5");

        // Presence removed only for session-v4
        verify(presenceService).handleSessionDisconnected(testUserId, "session-v4");
        verify(presenceService, never()).handleSessionDisconnected(testUserId, "session-v5");
    }

    @Test
    @DisplayName("34. Malformed Redis pub/sub events are rejected safely without closing sessions")
    void malformedEvent_isRejectedSafely() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeB, "session-live", testUserId, futureExpiry, 5L, desktopTransportSession);

        // Case 1: Null userId
        AccountRevocationEvent nullUserEvent = new AccountRevocationEvent(null, 6L, "bad", System.currentTimeMillis());
        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), objectMapper.writeValueAsBytes(nullUserEvent)), null);

        // Case 2: Negative authVersion
        AccountRevocationEvent negVersionEvent = new AccountRevocationEvent(testUserId, -1L, "bad", System.currentTimeMillis());
        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), objectMapper.writeValueAsBytes(negVersionEvent)), null);

        // Case 3: Corrupt JSON
        byte[] corruptJson = "{invalid-json:".getBytes(StandardCharsets.UTF_8);
        revocationListenerNodeB.onMessage(new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), corruptJson), null);

        // Live session must remain completely untouched
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("session-live")).isFalse();
    }

    @Test
    @DisplayName("35. Legacy token semantics: missing auth_version claim treated as 0L; rejected once authVersion > 0")
    void legacyTokenWithoutAuthVersion_noNullBypass() {
        // User with authVersion = 0
        testUser.setAuthVersion(0L);
        UserPrincipal principalV0 = new UserPrincipal(testUser);

        when(jwtService.extractUsername("legacy-token")).thenReturn("user@collobster.com");
        when(jwtService.extractTokenType("legacy-token")).thenReturn("ACCESS");
        when(jwtService.isAccessToken("legacy-token")).thenReturn(true);
        when(jwtService.extractAuthVersion("legacy-token")).thenReturn(null); // Missing claim in legacy token
        when(jwtService.isTokenExpired("legacy-token")).thenReturn(false);
        when(jwtService.isTokenValid(eq("legacy-token"), any())).thenCallRealMethod();

        // Legacy token without auth_version claim defaults to 0L -> matches principalV0 (0L)
        assertThat(jwtService.isTokenValid("legacy-token", principalV0)).isTrue();

        // Increment user to 1L
        testUser.setAuthVersion(1L);
        UserPrincipal principalV1 = new UserPrincipal(testUser);

        // Legacy token must be rejected! No null bypass!
        assertThat(jwtService.isTokenValid("legacy-token", principalV1)).isFalse();
    }

    @Test
    @DisplayName("36. Mandatory Equality Test: sessionVersion == eventVersion (5 == 5) preserves session, socket, and presence")
    void remoteRevocation_equalAuthVersion_preservesSession() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        // Node B has active session with authVersion = 5L
        registerTestSession(sessionRegistryNodeB, "nodeB-session-v5-exact", testUserId, futureExpiry, 5L, desktopTransportSession);

        // Remote event arrives with authVersion = 5L
        AccountRevocationEvent event = new AccountRevocationEvent(testUserId, 5L, "Logout all generation 5", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(event);

        revocationListenerNodeB.onMessage(
                new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload),
                null
        );

        // Strict less-than semantics: 5 < 5 is FALSE -> session MUST remain active!
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-session-v5-exact")).isFalse();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).hasSize(1);

        // Presence must not be evicted
        verify(presenceService, never()).forceOffline(testUserId);
        verify(presenceService, never()).handleSessionDisconnected(any(), any());
    }

    @Test
    @DisplayName("37. Older Session Test: sessionVersion < eventVersion (4 < 5) closes session with 4401 and evicts session presence")
    void remoteRevocation_olderAuthVersion_closesSession() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        // Node B has active session with authVersion = 4L
        registerTestSession(sessionRegistryNodeB, "nodeB-session-v4-older", testUserId, futureExpiry, 4L, phoneTransportSession);

        // Remote event arrives with authVersion = 5L
        AccountRevocationEvent event = new AccountRevocationEvent(testUserId, 5L, "Security reset", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(event);

        revocationListenerNodeB.onMessage(
                new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload),
                null
        );

        // 4 < 5 is TRUE -> session must be closed with status 4401
        verify(phoneTransportSession).close(argThat(status -> status.getCode() == 4401));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-session-v4-older")).isTrue();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).isEmpty();

        // Presence evicted for this closed session
        verify(presenceService).handleSessionDisconnected(testUserId, "nodeB-session-v4-older");
        verify(presenceService, never()).forceOffline(testUserId);
    }

    @Test
    @DisplayName("38. Newer Session Test: sessionVersion > eventVersion (6 > 5) preserves session")
    void remoteRevocation_newerAuthVersion_preservesSession() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        // Node B has active session with authVersion = 6L
        registerTestSession(sessionRegistryNodeB, "nodeB-session-v6-newer", testUserId, futureExpiry, 6L, tabletTransportSession);

        // Remote event arrives with authVersion = 5L
        AccountRevocationEvent event = new AccountRevocationEvent(testUserId, 5L, "Stale event", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(event);

        revocationListenerNodeB.onMessage(
                new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload),
                null
        );

        // 6 < 5 is FALSE -> session must remain untouched
        verify(tabletTransportSession, never()).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("nodeB-session-v6-newer")).isFalse();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).hasSize(1);
    }

    @Test
    @DisplayName("39. Mixed Session Test: User has sessions A(v4), B(v5), C(v6). Event v5 closes A, preserves B and C, user remains online")
    void remoteRevocation_mixedSessionVersions_closesOnlyOlderAndPreservesPresence() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeB, "session-mixed-A4", testUserId, futureExpiry, 4L, phoneTransportSession);
        registerTestSession(sessionRegistryNodeB, "session-mixed-B5", testUserId, futureExpiry, 5L, desktopTransportSession);
        registerTestSession(sessionRegistryNodeB, "session-mixed-C6", testUserId, futureExpiry, 6L, tabletTransportSession);

        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).hasSize(3);

        // Remote event with authVersion = 5L
        AccountRevocationEvent event = new AccountRevocationEvent(testUserId, 5L, "Mixed version revocation", System.currentTimeMillis());
        byte[] payload = objectMapper.writeValueAsBytes(event);

        revocationListenerNodeB.onMessage(
                new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload),
                null
        );

        // A(4) closed, B(5) and C(6) preserved
        verify(phoneTransportSession).close(argThat(status -> status.getCode() == 4401));
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));
        verify(tabletTransportSession, never()).close(any(CloseStatus.class));

        assertThat(sessionRegistryNodeB.isRevoked("session-mixed-A4")).isTrue();
        assertThat(sessionRegistryNodeB.isRevoked("session-mixed-B5")).isFalse();
        assertThat(sessionRegistryNodeB.isRevoked("session-mixed-C6")).isFalse();

        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId))
                .extracting(WebSocketSessionMetadata::getSessionId)
                .containsExactlyInAnyOrder("session-mixed-B5", "session-mixed-C6");

        // Presence removed only for session A; user remains online because B and C are active
        verify(presenceService).handleSessionDisconnected(testUserId, "session-mixed-A4");
        verify(presenceService, never()).handleSessionDisconnected(testUserId, "session-mixed-B5");
        verify(presenceService, never()).handleSessionDisconnected(testUserId, "session-mixed-C6");
        verify(presenceService, never()).forceOffline(testUserId);
    }

    @Test
    @DisplayName("40. Duplicate Event Test: Duplicate delivery of event v5 does not terminate newly established v5 session")
    void remoteRevocation_duplicateEvent_doesNotCloseNewSameVersionSession() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);

        // First delivery of event v5
        AccountRevocationEvent event1 = new AccountRevocationEvent(testUserId, 5L, "Logout all", System.currentTimeMillis());
        byte[] payload1 = objectMapper.writeValueAsBytes(event1);
        revocationListenerNodeB.onMessage(
                new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload1),
                null
        );

        // User logs in and establishes new session with authVersion = 5L
        registerTestSession(sessionRegistryNodeB, "session-new-v5", testUserId, futureExpiry, 5L, desktopTransportSession);
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).hasSize(1);

        // Duplicate delivery of event v5 arrives
        AccountRevocationEvent event2 = new AccountRevocationEvent(testUserId, 5L, "Logout all duplicate", System.currentTimeMillis());
        byte[] payload2 = objectMapper.writeValueAsBytes(event2);
        revocationListenerNodeB.onMessage(
                new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), payload2),
                null
        );

        // 5 < 5 is false: New session must NOT be closed!
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("session-new-v5")).isFalse();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).hasSize(1);
    }

    @Test
    @DisplayName("41. Out-of-Order Event Test: Stale event v5 cannot close newer v6 session after event v6 processed")
    void remoteRevocation_outOfOrderEvents_doesNotAffectValidSessions() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);

        // Event v6 arrives
        AccountRevocationEvent eventV6 = new AccountRevocationEvent(testUserId, 6L, "Admin reset", System.currentTimeMillis());
        revocationListenerNodeB.onMessage(
                new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), objectMapper.writeValueAsBytes(eventV6)),
                null
        );

        // User establishes legitimate session with authVersion = 6L
        registerTestSession(sessionRegistryNodeB, "session-v6-valid", testUserId, futureExpiry, 6L, desktopTransportSession);

        // Delayed stale event v5 arrives
        AccountRevocationEvent eventV5 = new AccountRevocationEvent(testUserId, 5L, "Delayed stale logout", System.currentTimeMillis());
        revocationListenerNodeB.onMessage(
                new DefaultMessage(AccountRevocationCoordinator.REVOCATION_CHANNEL.getBytes(StandardCharsets.UTF_8), objectMapper.writeValueAsBytes(eventV5)),
                null
        );

        // Session v6 remains completely safe
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("session-v6-valid")).isFalse();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId)).hasSize(1);
    }

    @Test
    @DisplayName("42. Reaper Rule: session equal to DB authority (5 == 5) is preserved; session older than DB (4 < 5) is reaped")
    void reaper_equalAuthVersion_preservesSession_andOlderReaped() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeB, "reaper-sess-equal", testUserId, futureExpiry, 5L, desktopTransportSession);
        registerTestSession(sessionRegistryNodeB, "reaper-sess-stale", testUserId, futureExpiry, 4L, phoneTransportSession);

        // DB state: authVersion = 5L, account enabled
        when(userRepository.findAuthStatesByUserIds(any())).thenReturn(List.<Object[]>of(
                new Object[]{testUserId, 5L, true, false, false}
        ));

        sessionReaperNodeB.reapRevokedSessions();

        // Older session reaped, equal session preserved!
        verify(phoneTransportSession).close(any(CloseStatus.class));
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));

        assertThat(sessionRegistryNodeB.isRevoked("reaper-sess-stale")).isTrue();
        assertThat(sessionRegistryNodeB.isRevoked("reaper-sess-equal")).isFalse();
        assertThat(sessionRegistryNodeB.getActiveSessionsForUser(testUserId))
                .extracting(WebSocketSessionMetadata::getSessionId)
                .containsExactly("reaper-sess-equal");
    }

    @Test
    @DisplayName("43. Legacy Session: session without auth_version (null/0L) is valid at DB 0, but reaped when DB > 0")
    void legacySession_validAtVersionZero_revokedAtVersionGreaterThanZero() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        registerTestSession(sessionRegistryNodeB, "legacy-sess", testUserId, futureExpiry, null, desktopTransportSession);

        // Case A: DB is 0L -> valid
        when(userRepository.findAuthStatesByUserIds(any())).thenReturn(List.<Object[]>of(
                new Object[]{testUserId, 0L, true, false, false}
        ));
        sessionReaperNodeB.reapRevokedSessions();
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("legacy-sess")).isFalse();

        // Case B: DB is 1L -> reaped
        when(userRepository.findAuthStatesByUserIds(any())).thenReturn(List.<Object[]>of(
                new Object[]{testUserId, 1L, true, false, false}
        ));
        sessionReaperNodeB.reapRevokedSessions();
        verify(desktopTransportSession).close(any(CloseStatus.class));
        assertThat(sessionRegistryNodeB.isRevoked("legacy-sess")).isTrue();
    }

    @Test
    @DisplayName("44. Local Revocation: Coordinator terminates older generation sessions and preserves concurrent new-generation session")
    void localRevocation_preservesConcurrentSessionWithNewVersion() throws Exception {
        Instant futureExpiry = Instant.now().plusSeconds(900);
        // Old session with authVersion 0L
        registerTestSession(sessionRegistryNodeA, "local-old-v0", testUserId, futureExpiry, 0L, phoneTransportSession);
        // New session with authVersion 1L (e.g. established concurrently during revocation)
        registerTestSession(sessionRegistryNodeA, "local-new-v1", testUserId, futureExpiry, 1L, desktopTransportSession);

        // Coordinator revokes sessions (DB advances from 0 to 1)
        revocationCoordinatorNodeA.revokeAccountSessions(testUserId, "Logout all");

        // Old session (0 < 1) closed
        verify(phoneTransportSession).close(any(CloseStatus.class));
        // New session (1 >= 1) preserved
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));

        assertThat(sessionRegistryNodeA.isRevoked("local-old-v0")).isTrue();
        assertThat(sessionRegistryNodeA.isRevoked("local-new-v1")).isFalse();
        assertThat(sessionRegistryNodeA.getActiveSessionsForUser(testUserId))
                .extracting(WebSocketSessionMetadata::getSessionId)
                .containsExactly("local-new-v1");

        // Presence must not be forceOffline since local-new-v1 is active
        verify(presenceService, never()).forceOffline(testUserId);
        verify(presenceService).handleSessionDisconnected(testUserId, "local-old-v0");
    }
}
