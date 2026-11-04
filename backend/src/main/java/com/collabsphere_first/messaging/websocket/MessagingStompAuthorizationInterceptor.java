package com.collabsphere_first.messaging.websocket;

import com.collabsphere_first.messaging.presence.MessagingPresenceService;
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
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagingStompAuthorizationInterceptor implements ChannelInterceptor {

    private static final Set<String> ALLOWED_USER_DESTINATIONS = Set.of(
            "/user/queue/messages",
            "/user/queue/message-events",
            "/user/queue/receipts",
            "/user/queue/typing"
    );

    private static final Pattern ALLOWED_APP_DESTINATIONS = Pattern.compile("^/app/messages/(?:typing|delivery-ack|heartbeat)$");

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final MessagingPresenceService presenceService;
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
                    throw new IllegalArgumentException("A bearer token is required for messaging WebSocket connection");
                }
                String token = authorization.substring(7);
                var user = userDetailsService.loadUserByUsername(jwtService.extractUsername(token));
                if (!jwtService.isTokenValid(token, user)) {
                    throw new IllegalArgumentException("Invalid bearer token for messaging WebSocket connection");
                }
                if (user instanceof UserPrincipal userPrincipal && sessionId != null) {
                    java.time.Instant expiresAt = jwtService.extractExpirationInstant(token);
                    Long authVersion = userPrincipal.getAuthVersion();
                    sessionRegistry.registerSession(sessionId, userPrincipal.getUser().getId(), userPrincipal.getUsername(), expiresAt, authVersion, "/ws/messages");
                }
                accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
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
            throw new IllegalArgumentException("Messaging session token expired or revoked. Please reconnect with fresh access token.");
        }

        // Renew presence on any active authenticated frame
        renewPresenceIfAuthenticated(accessor);

        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (destination != null && (destination.startsWith("/topic/communities") || destination.startsWith("/app/communities"))) {
                // Yield to Community interceptor
                return message;
            }

            // Yield to Notification and Realtime updates
            if (destination != null && (destination.equals("/user/queue/notifications") || destination.equals("/user/queue/updates"))) {
                return message;
            }

            Authentication auth = getAuthentication(accessor.getUser());
            // Clients must subscribe strictly to /user/queue/... - direct subscription to /queue/ is forbidden
            if (destination == null || !ALLOWED_USER_DESTINATIONS.contains(destination)) {
                log.warn("Rejected unauthorized subscription attempt by user {}: {}", auth.getName(), destination);
                throw new IllegalArgumentException("Unauthorized or invalid messaging subscription destination: " + destination);
            }
        }

        if (StompCommand.SEND.equals(command)) {
            String destination = accessor.getDestination();
            if (destination != null && (destination.startsWith("/topic/communities") || destination.startsWith("/app/communities"))) {
                // Yield to Community interceptor
                return message;
            }

            Authentication auth = getAuthentication(accessor.getUser());
            if (destination == null || !ALLOWED_APP_DESTINATIONS.matcher(destination).matches()) {
                log.warn("Rejected unauthorized app send attempt by user {}: {}", auth.getName(), destination);
                throw new IllegalArgumentException("Unauthorized messaging send destination: " + destination);
            }
        }

        return message;
    }

    private void renewPresenceIfAuthenticated(StompHeaderAccessor accessor) {
        try {
            Principal principal = accessor.getUser();
            if (principal instanceof UsernamePasswordAuthenticationToken auth &&
                    auth.getPrincipal() instanceof UserPrincipal userPrincipal &&
                    accessor.getSessionId() != null) {
                presenceService.onHeartbeat(userPrincipal.getUser().getId(), accessor.getSessionId());
            }
        } catch (Exception ex) {
            log.trace("Suppressed presence heartbeat renewal error: {}", ex.getMessage());
        }
    }

    private Authentication getAuthentication(Principal principal) {
        if (principal instanceof Authentication auth && auth.isAuthenticated()
                && auth.getPrincipal() instanceof UserPrincipal) {
            return auth;
        }
        throw new IllegalArgumentException("Authenticated STOMP session required");
    }
}
