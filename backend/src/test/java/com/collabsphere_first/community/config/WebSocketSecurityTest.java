package com.collabsphere_first.community.config;

import com.collabsphere_first.community.security.CommunityAccessService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private CommunityAccessService accessService;
    @Mock private MessageChannel messageChannel;

    @InjectMocks
    private CommunityStompAuthorizationInterceptor interceptor;

    private UUID communityId;
    private UUID channelId;
    private Long userId = 101L;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        channelId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").build();
        userPrincipal = new UserPrincipal(user);
    }

    @Test
    @DisplayName("CONNECT frame requires valid Bearer token")
    void connect_requiresBearerToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bearer token is required");
    }

    @Test
    @DisplayName("SUBSCRIBE frame validates authorized channel membership")
    void subscribe_validatesChannelMembership() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/communities/" + communityId + "/channels/" + channelId);
        accessor.setUser(new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList()));
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(msg, messageChannel);

        verify(accessService).requireMember(communityId, userId);
    }

    @Test
    @DisplayName("SUBSCRIBE frame to invalid destination is rejected")
    void subscribe_invalidDestinationRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/admin/secret-data");
        accessor.setUser(new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList()));
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized or invalid destination");
    }

    @Test
    @DisplayName("SEND frame requires writable active membership (not muted or banned)")
    void send_requiresWritableMember() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/communities/" + communityId + "/channels/" + channelId + "/messages");
        accessor.setUser(new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList()));
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(msg, messageChannel);

        verify(accessService).requireWritableMember(communityId, userId);
    }
}
