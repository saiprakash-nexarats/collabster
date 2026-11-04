package com.collabsphere_first.community.config;

import com.collabsphere_first.messaging.websocket.MessagingStompAuthorizationInterceptor;
import com.collabsphere_first.notification.security.NotificationStompAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class CommunityWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final CommunityStompAuthorizationInterceptor authorizationInterceptor;
    private final MessagingStompAuthorizationInterceptor messagingInterceptor;
    private final NotificationStompAuthorizationInterceptor notificationInterceptor;

    @Value("${collabster.communities.websocket.allowed-origins:http://localhost:*}")
    private String allowedOrigins;

    @Value("${collabster.communities.websocket.relay.enabled:false}")
    private boolean relayEnabled;

    @Value("${collabster.communities.websocket.relay.host:localhost}")
    private String relayHost;

    @Value("${collabster.communities.websocket.relay.port:61613}")
    private int relayPort;

    @Value("${collabster.communities.websocket.relay.client-login:guest}")
    private String relayClientLogin;

    @Value("${collabster.communities.websocket.relay.client-passcode:guest}")
    private String relayClientPasscode;

    @Value("${collabster.communities.websocket.relay.system-login:guest}")
    private String relaySystemLogin;

    @Value("${collabster.communities.websocket.relay.system-passcode:guest}")
    private String relaySystemPasscode;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (relayEnabled) {
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(relayClientLogin)
                    .setClientPasscode(relayClientPasscode)
                    .setSystemLogin(relaySystemLogin)
                    .setSystemPasscode(relaySystemPasscode);
        } else {
            registry.enableSimpleBroker("/topic", "/queue");
        }
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(value -> !value.isBlank()).toArray(String[]::new);
        registry.addEndpoint("/ws/community", "/realtime", "/ws/messages", "/ws", "/api/ws/community").setAllowedOriginPatterns(origins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authorizationInterceptor, messagingInterceptor, notificationInterceptor);
    }
}
