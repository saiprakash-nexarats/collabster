package com.collabsphere_first.notification.security;

import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.userdetails.CustomUserDetailsService;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStompAuthorizationInterceptor implements ChannelInterceptor {

    public static final String NOTIFICATION_QUEUE = "/user/queue/notifications";

    private static final Set<String> OTHER_ALLOWED_PREFIXES = Set.of(
            "/topic/communities",
            "/app/communities",
            "/user/queue/messages",
            "/user/queue/message-events",
            "/user/queue/receipts",
            "/user/queue/typing",
            "/app/messages"
    );

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final com.collabsphere_first.security.websocket.WebSocketAuthenticatedSessionRegistry sessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();

        if (StompCommand.CONNECT.equals(command)) {
            if (accessor.getUser() == null) {
                String authorization = accessor.getFirstNativeHeader("Authorization");
                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    throw new IllegalArgumentException("A bearer token is required for notification WebSocket connection");
                }
                String token = authorization.substring(7);
                try {
                    var user = userDetailsService.loadUserByUsername(jwtService.extractUsername(token));
                    if (!jwtService.isTokenValid(token, user)) {
                        throw new IllegalArgumentException("Invalid bearer token for notification WebSocket connection");
                    }
                    if (user instanceof UserPrincipal userPrincipal && sessionId != null) {
                        java.time.Instant expiresAt = jwtService.extractExpirationInstant(token);
                        Long authVersion = userPrincipal.getAuthVersion();
                        sessionRegistry.registerSession(sessionId, userPrincipal.getUser().getId(), userPrincipal.getUsername(), expiresAt, authVersion, "/ws/notifications");
                    }
                    accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                    return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
                } catch (Exception ex) {
                    log.warn("Invalid JWT in notification STOMP CONNECT: {}", ex.getMessage());
                    throw new IllegalArgumentException("Invalid bearer token for notification WebSocket connection: " + ex.getMessage());
                }
            }
            return message;
        }

        if (StompCommand.DISCONNECT.equals(command)) {
            if (sessionId != null) {
                sessionRegistry.unregisterSession(sessionId);
            }
            return message;
        }

        // Hard boundary: reject frames on expired or revoked WebSocket sessions
        if (sessionId != null && (sessionRegistry.isExpired(sessionId) || sessionRegistry.isRevoked(sessionId))) {
            throw new IllegalArgumentException("Notification session token expired or revoked. Please reconnect with fresh access token.");
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (destination != null) {
                // If it's a community or messaging destination, yield to other interceptors
                for (String prefix : OTHER_ALLOWED_PREFIXES) {
                    if (destination.startsWith(prefix)) {
                        return message;
                    }
                }

                // Strictly validate notification queue
                if (NOTIFICATION_QUEUE.equals(destination)) {
                    Authentication auth = getAuthentication(accessor.getUser());
                    log.debug("User {} subscribed to notification destination {}", auth.getName(), destination);
                    return message;
                }

                // If someone tries to subscribe to raw /queue/notifications or /topic/notifications or /user/{other}/...
                if (destination.contains("notification")) {
                    log.warn("Rejected unauthorized notification subscription attempt: destination={}", destination);
                    throw new IllegalArgumentException("Unauthorized notification subscription destination: " + destination);
                }
            }
        }

        return message;
    }

    private Authentication getAuthentication(Principal principal) {
        if (principal instanceof Authentication auth && auth.isAuthenticated()
                && auth.getPrincipal() instanceof UserPrincipal) {
            return auth;
        }
        throw new IllegalArgumentException("Authenticated STOMP session required for notification subscriptions");
    }
}
