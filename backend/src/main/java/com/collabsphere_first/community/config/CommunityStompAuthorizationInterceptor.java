package com.collabsphere_first.community.config;

import com.collabsphere_first.community.security.CommunityAccessService;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.userdetails.CustomUserDetailsService;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import com.collabsphere_first.security.websocket.WebSocketAuthenticatedSessionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Authenticates STOMP CONNECT frames and guards Community topic subscriptions. */
@Component
public class CommunityStompAuthorizationInterceptor implements ChannelInterceptor {
    private static final Pattern COMMUNITY_CHANNEL_DESTINATION = Pattern.compile("^/(?:topic|app)/communities/([0-9a-fA-F-]{36})/channels/[0-9a-fA-F-]{36}(?:/messages)?$");

    private static final Set<String> ALLOWED_USER_DESTINATIONS = Set.of(
            "/user/queue/updates",
            "/user/queue/notifications",
            "/user/queue/messages",
            "/user/queue/message-events",
            "/user/queue/receipts",
            "/user/queue/typing"
    );

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final CommunityAccessService accessService;

    @Autowired(required = false)
    private WebSocketAuthenticatedSessionRegistry sessionRegistry;

    public CommunityStompAuthorizationInterceptor(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            CommunityAccessService accessService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.accessService = accessService;
    }

    public CommunityStompAuthorizationInterceptor(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            CommunityAccessService accessService,
            WebSocketAuthenticatedSessionRegistry sessionRegistry) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.accessService = accessService;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();

        if (StompCommand.CONNECT.equals(command)) {
            if (accessor.getUser() == null) {
                String authorization = accessor.getFirstNativeHeader("Authorization");
                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    throw new IllegalArgumentException("A bearer token is required");
                }
                String token = authorization.substring(7);
                var user = userDetailsService.loadUserByUsername(jwtService.extractUsername(token));
                if (!jwtService.isTokenValid(token, user)) {
                    throw new IllegalArgumentException("Invalid bearer token");
                }
                if (sessionRegistry != null && sessionId != null && user instanceof UserPrincipal userPrincipal) {
                    sessionRegistry.registerSession(sessionId, userPrincipal.getUser().getId(), userPrincipal.getUsername(),
                            jwtService.extractExpirationInstant(token), userPrincipal.getAuthVersion(), "/ws/community");
                }
                accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
            }
            return message;
        }

        if (StompCommand.DISCONNECT.equals(command)) {
            if (sessionRegistry != null && sessionId != null) {
                sessionRegistry.unregisterSession(sessionId);
            }
            return message;
        }

        // Hard boundary: reject frames on expired or revoked WebSocket sessions
        if (sessionRegistry != null && sessionId != null && (sessionRegistry.isExpired(sessionId) || sessionRegistry.isRevoked(sessionId))) {
            throw new IllegalArgumentException("Session expired or revoked. Please reconnect with fresh access token.");
        }

        if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            Authentication authentication = authentication(accessor.getUser());
            String destination = accessor.getDestination();
            if (destination == null || destination.isBlank()) {
                throw new IllegalArgumentException("Destination required for STOMP " + command);
            }

            // Direct subscription to raw broker queues (e.g. /queue/*) is strictly forbidden
            if (destination.startsWith("/queue/")) {
                throw new IllegalArgumentException("Direct subscription to broker queues is forbidden: " + destination);
            }

            // Client SEND directly to broker destinations (/topic/* or /queue/*) is strictly forbidden
            if (StompCommand.SEND.equals(command) && (destination.startsWith("/topic/") || destination.startsWith("/queue/"))) {
                throw new IllegalArgumentException("Direct SEND to broker destinations is forbidden: " + destination);
            }

            // Handling SUBSCRIBE
            if (StompCommand.SUBSCRIBE.equals(command)) {
                if (ALLOWED_USER_DESTINATIONS.contains(destination)) {
                    // Allowed user-scoped queue subscription
                    return message;
                }

                if (destination.startsWith("/user/")) {
                    throw new IllegalArgumentException("Unauthorized user destination: " + destination);
                }

                Matcher matcher = COMMUNITY_CHANNEL_DESTINATION.matcher(destination);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Unauthorized or invalid destination: " + destination);
                }
                UUID communityId = UUID.fromString(matcher.group(1));
                Long userId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
                accessService.requireMember(communityId, userId);
                return message;
            }

            // Handling SEND
            if (StompCommand.SEND.equals(command)) {
                if (destination.startsWith("/app/messages/")) {
                    // Yield to Messaging interceptor
                    return message;
                }

                Matcher matcher = COMMUNITY_CHANNEL_DESTINATION.matcher(destination);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Unauthorized or invalid destination: " + destination);
                }
                UUID communityId = UUID.fromString(matcher.group(1));
                Long userId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
                accessService.requireWritableMember(communityId, userId);
                return message;
            }
        }
        return message;
    }

    private Authentication authentication(Principal principal) {
        if (principal instanceof Authentication authentication && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal) return authentication;
        throw new IllegalArgumentException("Authenticated STOMP session required");
    }
}
