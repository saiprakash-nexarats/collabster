package com.collabsphere_first.security.websocket;

import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.messaging.websocket.MessagingStompAuthorizationInterceptor;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.userdetails.CustomUserDetailsService;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import com.collabsphere_first.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Multi-Device WebSocket Expiry & Presence Correctness Tests")
public class MultiDeviceWebSocketExpiryTest {

    private WebSocketAuthenticatedSessionRegistry sessionRegistry;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private MessageChannel messageChannel;

    @Mock
    private WebSocketSession phoneTransportSession;

    @Mock
    private WebSocketSession desktopTransportSession;

    private MockMessagingPresenceService presenceService;
    private MessagingStompAuthorizationInterceptor messagingInterceptor;
    private WebSocketOutboundExpiryInterceptor outboundInterceptor;
    private WebSocketSessionReaper sessionReaper;

    private Instant initialTime;
    private MutableClock testClock;
    private User testUser;
    private UserPrincipal testPrincipal;

    static class MutableClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone;

        MutableClock(Instant initial, ZoneId zone) {
            this.currentInstant = initial;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }

        public void advance(Duration duration) {
            this.currentInstant = this.currentInstant.plus(duration);
        }
    }

    /**
     * In-memory presence service tracking active sessions per user,
     * matching the Redis ZSET architecture of MessagingPresenceServiceImpl.
     */
    static class MockMessagingPresenceService implements MessagingPresenceService {
        private final Set<String> activeSessions = new HashSet<>();
        private final Long userId;

        MockMessagingPresenceService(Long userId) {
            this.userId = userId;
        }

        @Override
        public void onSessionConnected(Long uid, String username, String sessionId) {
            if (userId.equals(uid)) {
                activeSessions.add(sessionId);
            }
        }

        @Override
        public void onHeartbeat(Long uid, String sessionId) {
            if (userId.equals(uid)) {
                activeSessions.add(sessionId);
            }
        }

        @Override
        public void onSessionDisconnected(Long uid, String username, String sessionId) {
            if (userId.equals(uid)) {
                activeSessions.remove(sessionId);
            }
        }

        @Override
        public void removeSession(Long uid, String sessionId) {
            onSessionDisconnected(uid, null, sessionId);
        }

        @Override
        public void handleSessionDisconnected(Long uid, String sessionId) {
            onSessionDisconnected(uid, null, sessionId);
        }

        @Override
        public boolean isUserOnline(Long uid) {
            return userId.equals(uid) && !activeSessions.isEmpty();
        }

        @Override
        public void forceOffline(Long uid) {
            if (userId.equals(uid)) {
                activeSessions.clear();
            }
        }

        public int getActiveSessionCount() {
            return activeSessions.size();
        }
    }

    @BeforeEach
    void setUp() {
        initialTime = Instant.parse("2026-09-30T10:00:00Z");
        testClock = new MutableClock(initialTime, ZoneId.of("UTC"));

        sessionRegistry = new WebSocketAuthenticatedSessionRegistry();
        sessionRegistry.setClock(testClock);

        Long userId = 501L;
        presenceService = new MockMessagingPresenceService(userId);

        messagingInterceptor = new MessagingStompAuthorizationInterceptor(
                jwtService, userDetailsService, presenceService, sessionRegistry
        );

        outboundInterceptor = new WebSocketOutboundExpiryInterceptor(sessionRegistry, presenceService);
        sessionReaper = new WebSocketSessionReaper(sessionRegistry, presenceService);

        testUser = User.builder()
                .id(userId)
                .email("user-multidevice@collobster.com")
                .accountEnabled(true)
                .roles(Collections.emptySet())
                .build();
        testPrincipal = new UserPrincipal(testUser);

        when(userDetailsService.loadUserByUsername("user-multidevice@collobster.com")).thenReturn(testPrincipal);
        when(phoneTransportSession.isOpen()).thenReturn(true);
        when(desktopTransportSession.isOpen()).thenReturn(true);
    }

    private Message<?> createStompMessage(StompCommand command, String sessionId, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(sessionId);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setUser(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                testPrincipal, null, testPrincipal.getAuthorities()
        ));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("twoSessions_oneExpires_otherRemainsAuthorized: Phone expires, desktop stays connected and receives private messages")
    void twoSessions_oneExpires_otherRemainsAuthorized() throws Exception {
        Long userId = 501L;
        String phoneSession = "sess-phone-1";
        String desktopSession = "sess-desktop-2";

        // 1. Establish two sessions: Phone expires at T+10m, Desktop expires at T+30m
        sessionRegistry.registerTransportSession(phoneSession, phoneTransportSession);
        sessionRegistry.registerSession(phoneSession, userId, "user-multidevice@collobster.com",
                initialTime.plus(Duration.ofMinutes(10)), "/ws/messages");
        presenceService.onSessionConnected(userId, "user-multidevice@collobster.com", phoneSession);

        sessionRegistry.registerTransportSession(desktopSession, desktopTransportSession);
        sessionRegistry.registerSession(desktopSession, userId, "user-multidevice@collobster.com",
                initialTime.plus(Duration.ofMinutes(30)), "/ws/messages");
        presenceService.onSessionConnected(userId, "user-multidevice@collobster.com", desktopSession);

        assertThat(sessionRegistry.getActiveSessionCountForUser(userId)).isEqualTo(2);
        assertThat(presenceService.isUserOnline(userId)).isTrue();
        assertThat(presenceService.getActiveSessionCount()).isEqualTo(2);

        // 2. Advance clock by 15 minutes: Phone expired, Desktop still valid
        testClock.advance(Duration.ofMinutes(15));
        assertThat(sessionRegistry.isExpired(phoneSession)).isTrue();
        assertThat(sessionRegistry.isExpired(desktopSession)).isFalse();

        // 3. Verify Phone cannot SEND or SUBSCRIBE
        Message<?> phoneSend = createStompMessage(StompCommand.SEND, phoneSession, "/app/messages/typing");
        assertThatThrownBy(() -> messagingInterceptor.preSend(phoneSend, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        Message<?> phoneSub = createStompMessage(StompCommand.SUBSCRIBE, phoneSession, "/user/queue/messages");
        assertThatThrownBy(() -> messagingInterceptor.preSend(phoneSub, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        // 4. Verify Desktop CAN still SEND and SUBSCRIBE
        Message<?> desktopSend = createStompMessage(StompCommand.SEND, desktopSession, "/app/messages/typing");
        assertThat(messagingInterceptor.preSend(desktopSend, messageChannel)).isNotNull();

        Message<?> desktopSub = createStompMessage(StompCommand.SUBSCRIBE, desktopSession, "/user/queue/messages");
        assertThat(messagingInterceptor.preSend(desktopSub, messageChannel)).isNotNull();

        // 5. User destination fanout: Dispatch private notification to both sessions
        // Outbound for Phone (expired): dropped, transport closed
        Message<?> phoneOutbound = createStompMessage(StompCommand.MESSAGE, phoneSession, "/user/queue/notifications");
        Message<?> phoneDelivered = outboundInterceptor.preSend(phoneOutbound, messageChannel);
        assertThat(phoneDelivered).isNull();
        verify(phoneTransportSession).close(any(CloseStatus.class));

        // Outbound for Desktop (valid): delivered intact!
        Message<?> desktopOutbound = createStompMessage(StompCommand.MESSAGE, desktopSession, "/user/queue/notifications");
        Message<?> desktopDelivered = outboundInterceptor.preSend(desktopOutbound, messageChannel);
        assertThat(desktopDelivered).isNotNull();
        assertThat(desktopDelivered).isSameAs(desktopOutbound);
        verify(desktopTransportSession, never()).close(any(CloseStatus.class));

        // 6. User presence remains ONLINE because Desktop is still active
        assertThat(presenceService.isUserOnline(userId)).isTrue();
        assertThat(presenceService.getActiveSessionCount()).isEqualTo(1);
        assertThat(sessionRegistry.getActiveSessionCountForUser(userId)).isEqualTo(1);
        assertThat(sessionRegistry.getSession(phoneSession)).isNull();
        assertThat(sessionRegistry.getSession(desktopSession)).isNotNull();
    }

    @Test
    @DisplayName("finalSessionExpiry: When the last remaining session expires, user transitions to OFFLINE")
    void finalSessionExpiry() throws Exception {
        Long userId = 501L;
        String phoneSession = "sess-phone-1";
        String desktopSession = "sess-desktop-2";

        // Setup both sessions
        sessionRegistry.registerTransportSession(phoneSession, phoneTransportSession);
        sessionRegistry.registerSession(phoneSession, userId, "user-multidevice@collobster.com",
                initialTime.plus(Duration.ofMinutes(10)), "/ws/messages");
        presenceService.onSessionConnected(userId, "user-multidevice@collobster.com", phoneSession);

        sessionRegistry.registerTransportSession(desktopSession, desktopTransportSession);
        sessionRegistry.registerSession(desktopSession, userId, "user-multidevice@collobster.com",
                initialTime.plus(Duration.ofMinutes(30)), "/ws/messages");
        presenceService.onSessionConnected(userId, "user-multidevice@collobster.com", desktopSession);

        // Advance past phone expiry (T+15m) and reap
        testClock.advance(Duration.ofMinutes(15));
        sessionReaper.reapExpiredSessions();

        verify(phoneTransportSession).close(any(CloseStatus.class));
        assertThat(presenceService.isUserOnline(userId)).isTrue();
        assertThat(sessionRegistry.getActiveSessionCountForUser(userId)).isEqualTo(1);

        // Advance past desktop expiry (T+35m) and reap
        testClock.advance(Duration.ofMinutes(20));
        assertThat(sessionRegistry.isExpired(desktopSession)).isTrue();
        sessionReaper.reapExpiredSessions();

        verify(desktopTransportSession).close(any(CloseStatus.class));
        // All sessions reaped: User is now OFFLINE
        assertThat(presenceService.isUserOnline(userId)).isFalse();
        assertThat(presenceService.getActiveSessionCount()).isEqualTo(0);
        assertThat(sessionRegistry.getActiveSessionCountForUser(userId)).isEqualTo(0);
        assertThat(sessionRegistry.getActiveSessionCount()).isEqualTo(0);
    }
}
