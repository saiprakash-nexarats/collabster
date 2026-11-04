package com.collabsphere_first.messaging.websocket;

import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.userdetails.CustomUserDetailsService;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import com.collabsphere_first.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingWebSocketSecurityTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private MessageChannel messageChannel;
    @Mock private com.collabsphere_first.messaging.presence.MessagingPresenceService presenceService;

    @InjectMocks
    private MessagingStompAuthorizationInterceptor interceptor;

    private Long userId = 201L;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(userId).email("messenger@example.com").build();
        userPrincipal = new UserPrincipal(user);
    }

    @Test
    @DisplayName("CONNECT frame requires Authorization header with Bearer token")
    void connect_requiresBearerToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bearer token is required");
    }

    @Test
    @DisplayName("CONNECT frame rejects invalid JWT token")
    void connect_rejectsInvalidToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer invalid-token");
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.extractUsername("invalid-token")).thenReturn("messenger@example.com");
        when(userDetailsService.loadUserByUsername("messenger@example.com")).thenReturn(userPrincipal);
        when(jwtService.isTokenValid("invalid-token", userPrincipal)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid bearer token");
    }

    @Test
    @DisplayName("CONNECT frame succeeds and populates user principal when JWT is valid")
    void connect_succeedsWithValidToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valid-token");
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.extractUsername("valid-token")).thenReturn("messenger@example.com");
        when(userDetailsService.loadUserByUsername("messenger@example.com")).thenReturn(userPrincipal);
        when(jwtService.isTokenValid("valid-token", userPrincipal)).thenReturn(true);

        Message<?> result = interceptor.preSend(msg, messageChannel);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);

        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser().getName()).isEqualTo("messenger@example.com");
    }

    @Test
    @DisplayName("SUBSCRIBE frame to allowed private user queue succeeds")
    void subscribe_allowedQueuesSucceed() {
        String[] queues = {
                "/user/queue/messages",
                "/user/queue/message-events",
                "/user/queue/receipts",
                "/user/queue/typing"
        };

        for (String queue : queues) {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            accessor.setDestination(queue);
            accessor.setUser(new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList()));
            Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            Message<?> result = interceptor.preSend(msg, messageChannel);
            assertThat(result).isNotNull();
        }
    }

    @Test
    @DisplayName("SUBSCRIBE frame without authentication is rejected")
    void subscribe_unauthenticatedRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/user/queue/messages");
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated STOMP session required");
    }

    @Test
    @DisplayName("SUBSCRIBE frame to unauthorized destination is rejected")
    void subscribe_unauthorizedDestinationRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/global-eavesdrop");
        accessor.setUser(new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList()));
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized or invalid messaging subscription destination");
    }

    @Test
    @DisplayName("SEND frame to allowed app destination succeeds")
    void send_allowedAppDestinationSucceeds() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/messages/typing");
        accessor.setUser(new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList()));
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(msg, messageChannel);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("SEND frame to unauthorized destination is rejected")
    void send_unauthorizedDestinationRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/messages/arbitrary-cmd");
        accessor.setUser(new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList()));
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized messaging send destination");
    }

    @Test
    @DisplayName("SUBSCRIBE frame to direct resolved queue /queue/... is rejected")
    void subscribe_directResolvedQueueRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/queue/messages-user-victim");
        accessor.setUser(new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList()));
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized or invalid messaging subscription destination");
    }

    @Test
    @DisplayName("SEND frame to heartbeat app destination succeeds")
    void send_heartbeatAppDestinationSucceeds() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/messages/heartbeat");
        accessor.setUser(new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList()));
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(msg, messageChannel);
        assertThat(result).isNotNull();
    }
}
