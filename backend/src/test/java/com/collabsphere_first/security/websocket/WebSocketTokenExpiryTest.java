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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebSocket Token Expiry Hardening Tests")
public class WebSocketTokenExpiryTest {

    private WebSocketAuthenticatedSessionRegistry sessionRegistry;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private MessagingPresenceService presenceService;

    @Mock
    private MessageChannel messageChannel;

    @Mock
    private WebSocketSession transportSession;

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

    @BeforeEach
    void setUp() {
        initialTime = Instant.parse("2026-09-30T10:00:00Z");
        testClock = new MutableClock(initialTime, ZoneId.of("UTC"));

        sessionRegistry = new WebSocketAuthenticatedSessionRegistry();
        sessionRegistry.setClock(testClock);

        messagingInterceptor = new MessagingStompAuthorizationInterceptor(
                jwtService, userDetailsService, presenceService, sessionRegistry
        );

        outboundInterceptor = new WebSocketOutboundExpiryInterceptor(sessionRegistry);
        sessionReaper = new WebSocketSessionReaper(sessionRegistry, presenceService);

        testUser = User.builder()
                .id(501L)
                .email("ws-user@collobster.com")
                .accountEnabled(true)
                .roles(Collections.emptySet())
                .build();
        testPrincipal = new UserPrincipal(testUser);

        when(userDetailsService.loadUserByUsername("ws-user@collobster.com")).thenReturn(testPrincipal);
        when(jwtService.extractUsername("short-lived-token")).thenReturn("ws-user@collobster.com");
        when(jwtService.isTokenValid(eq("short-lived-token"), any())).thenReturn(true);
        when(jwtService.extractExpirationInstant("short-lived-token"))
                .thenReturn(initialTime.plus(Duration.ofMinutes(15))); // expires at 10:15:00

        when(transportSession.isOpen()).thenReturn(true);
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
    @DisplayName("CONNECT registers session with JWT expiry in session registry")
    void connect_registersSessionWithTokenExpiry() {
        StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectAccessor.setSessionId("sess-100");
        connectAccessor.setNativeHeader("Authorization", "Bearer short-lived-token");
        Message<?> connectMsg = MessageBuilder.createMessage(new byte[0], connectAccessor.getMessageHeaders());

        sessionRegistry.registerTransportSession("sess-100", transportSession);
        messagingInterceptor.preSend(connectMsg, messageChannel);

        WebSocketSessionMetadata meta = sessionRegistry.getSession("sess-100");
        assertThat(meta).isNotNull();
        assertThat(meta.getUserId()).isEqualTo(501L);
        assertThat(meta.getExpiresAt()).isEqualTo(initialTime.plus(Duration.ofMinutes(15)));
        assertThat(sessionRegistry.isExpired("sess-100")).isFalse();
    }

    @Test
    @DisplayName("SEND and SUBSCRIBE work normally before token expiry")
    void sendAndSubscribe_succeedBeforeExpiry() {
        sessionRegistry.registerSession("sess-100", 501L, "ws-user@collobster.com",
                initialTime.plus(Duration.ofMinutes(15)), "/ws/messages");

        // SEND typing
        Message<?> sendMsg = createStompMessage(StompCommand.SEND, "sess-100", "/app/messages/typing");
        Message<?> result = messagingInterceptor.preSend(sendMsg, messageChannel);
        assertThat(result).isNotNull();

        // SUBSCRIBE user queue
        Message<?> subMsg = createStompMessage(StompCommand.SUBSCRIBE, "sess-100", "/user/queue/messages");
        Message<?> subResult = messagingInterceptor.preSend(subMsg, messageChannel);
        assertThat(subResult).isNotNull();
    }

    @Test
    @DisplayName("SEND after token expiry is rejected with session expired error")
    void send_afterExpiry_isRejected() {
        sessionRegistry.registerSession("sess-100", 501L, "ws-user@collobster.com",
                initialTime.plus(Duration.ofMinutes(15)), "/ws/messages");

        // Advance clock past 15-minute token expiry
        testClock.advance(Duration.ofMinutes(16));
        assertThat(sessionRegistry.isExpired("sess-100")).isTrue();

        Message<?> sendMsg = createStompMessage(StompCommand.SEND, "sess-100", "/app/messages/typing");
        assertThatThrownBy(() -> messagingInterceptor.preSend(sendMsg, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("SUBSCRIBE after token expiry is rejected with session expired error")
    void subscribe_afterExpiry_isRejected() {
        sessionRegistry.registerSession("sess-100", 501L, "ws-user@collobster.com",
                initialTime.plus(Duration.ofMinutes(15)), "/ws/messages");

        testClock.advance(Duration.ofMinutes(16));

        Message<?> subMsg = createStompMessage(StompCommand.SUBSCRIBE, "sess-100", "/user/queue/messages");
        assertThatThrownBy(() -> messagingInterceptor.preSend(subMsg, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Passive subscription protection: outbound messages to expired sessions are dropped and transport closed")
    void outboundChannel_dropsMessagesToExpiredSessions() throws Exception {
        sessionRegistry.registerTransportSession("sess-100", transportSession);
        sessionRegistry.registerSession("sess-100", 501L, "ws-user@collobster.com",
                initialTime.plus(Duration.ofMinutes(15)), "/ws/messages");

        // Advance clock past expiry
        testClock.advance(Duration.ofMinutes(16));

        Message<?> outboundMsg = createStompMessage(StompCommand.MESSAGE, "sess-100", "/user/queue/messages");
        Message<?> intercepted = outboundInterceptor.preSend(outboundMsg, messageChannel);

        // Frame dropped
        assertThat(intercepted).isNull();
        // Transport session closed
        verify(transportSession).close(any(CloseStatus.class));
        // Session unregistered
        assertThat(sessionRegistry.getSession("sess-100")).isNull();
    }

    @Test
    @DisplayName("Reaper task periodically closes expired sessions and triggers presence cleanup")
    void sessionReaper_closesExpiredSessionsAndCleansPresence() throws Exception {
        sessionRegistry.registerTransportSession("sess-100", transportSession);
        sessionRegistry.registerSession("sess-100", 501L, "ws-user@collobster.com",
                initialTime.plus(Duration.ofMinutes(15)), "/ws/messages");

        testClock.advance(Duration.ofMinutes(16));

        sessionReaper.reapExpiredSessions();

        verify(transportSession).close(any(CloseStatus.class));
        verify(presenceService).handleSessionDisconnected(501L, "sess-100");
        assertThat(sessionRegistry.getSession("sess-100")).isNull();
    }

    @Test
    @DisplayName("Multi-device WebSocket sessions expire independently")
    void multiDevice_sessionsExpireIndependently() {
        // Phone expires in 10 mins, Web expires in 30 mins
        sessionRegistry.registerSession("phone-session", 501L, "ws-user@collobster.com",
                initialTime.plus(Duration.ofMinutes(10)), "/ws/messages");
        sessionRegistry.registerSession("web-session", 501L, "ws-user@collobster.com",
                initialTime.plus(Duration.ofMinutes(30)), "/ws/messages");

        // Advance 15 minutes: Phone expired, Web still valid
        testClock.advance(Duration.ofMinutes(15));

        assertThat(sessionRegistry.isExpired("phone-session")).isTrue();
        assertThat(sessionRegistry.isExpired("web-session")).isFalse();

        Message<?> phoneSend = createStompMessage(StompCommand.SEND, "phone-session", "/app/messages/typing");
        assertThatThrownBy(() -> messagingInterceptor.preSend(phoneSend, messageChannel))
                .isInstanceOf(IllegalArgumentException.class);

        Message<?> webSend = createStompMessage(StompCommand.SEND, "web-session", "/app/messages/typing");
        Message<?> webResult = messagingInterceptor.preSend(webSend, messageChannel);
        assertThat(webResult).isNotNull();
    }

    @Test
    @DisplayName("Reconnect with fresh token restores authorization after previous session expired")
    void reconnect_withFreshToken_restoresAuthorization() {
        sessionRegistry.registerSession("expired-session", 501L, "ws-user@collobster.com",
                initialTime.plus(Duration.ofMinutes(10)), "/ws/messages");

        testClock.advance(Duration.ofMinutes(15));
        assertThat(sessionRegistry.isExpired("expired-session")).isTrue();

        // Fresh connection with new token
        when(jwtService.extractUsername("fresh-token")).thenReturn("ws-user@collobster.com");
        when(jwtService.isTokenValid(eq("fresh-token"), any())).thenReturn(true);
        when(jwtService.extractExpirationInstant("fresh-token"))
                .thenReturn(testClock.instant().plus(Duration.ofMinutes(15)));

        StompHeaderAccessor reconnectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        reconnectAccessor.setSessionId("new-session");
        reconnectAccessor.setNativeHeader("Authorization", "Bearer fresh-token");
        Message<?> reconnectMsg = MessageBuilder.createMessage(new byte[0], reconnectAccessor.getMessageHeaders());

        messagingInterceptor.preSend(reconnectMsg, messageChannel);

        assertThat(sessionRegistry.isExpired("new-session")).isFalse();
        Message<?> newSend = createStompMessage(StompCommand.SEND, "new-session", "/app/messages/typing");
        assertThat(messagingInterceptor.preSend(newSend, messageChannel)).isNotNull();
    }
}
