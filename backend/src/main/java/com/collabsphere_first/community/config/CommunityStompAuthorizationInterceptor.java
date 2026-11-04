package com.collabsphere_first.community.config;

import com.collabsphere_first.community.security.CommunityAccessService;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.userdetails.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Authenticates STOMP CONNECT frames and guards Community topic subscriptions. */
@Component
@RequiredArgsConstructor
public class CommunityStompAuthorizationInterceptor implements ChannelInterceptor {
    private static final Pattern COMMUNITY_CHANNEL_DESTINATION = Pattern.compile("^/(?:topic|app)/communities/([0-9a-fA-F-]{36})/channels/[0-9a-fA-F-]{36}(?:/messages)?$");
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final CommunityAccessService accessService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException("A bearer token is required");
            String token = authorization.substring(7);
            var user = userDetailsService.loadUserByUsername(jwtService.extractUsername(token));
            if (!jwtService.isTokenValid(token, user)) throw new IllegalArgumentException("Invalid bearer token");
            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
            Authentication authentication = authentication(accessor.getUser());
            String destination = accessor.getDestination();
            if (destination == null || destination.isBlank()) {
                throw new IllegalArgumentException("Destination required for STOMP " + accessor.getCommand());
            }

            // User-scoped queues (e.g. /user/queue/updates): permitted for authenticated user sessions
            if (destination.startsWith("/user/") || destination.startsWith("/queue/")) {
                return message;
            }

            Matcher matcher = COMMUNITY_CHANNEL_DESTINATION.matcher(destination);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Unauthorized or invalid destination: " + destination);
            }
            UUID communityId = UUID.fromString(matcher.group(1));
            Long userId = ((com.collabsphere_first.security.userdetails.UserPrincipal) authentication.getPrincipal()).getUser().getId();
            if (StompCommand.SEND.equals(accessor.getCommand())) {
                accessService.requireWritableMember(communityId, userId);
            } else {
                accessService.requireMember(communityId, userId);
            }
        }
        return message;
    }

    private Authentication authentication(Principal principal) {
        if (principal instanceof Authentication authentication && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof com.collabsphere_first.security.userdetails.UserPrincipal) return authentication;
        throw new IllegalArgumentException("Authenticated STOMP session required");
    }
}
