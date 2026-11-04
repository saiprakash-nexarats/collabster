package com.collabsphere_first.messaging.websocket;

import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagingWebSocketEventListener {

    private final MessagingPresenceService presenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (principal instanceof UsernamePasswordAuthenticationToken auth &&
                auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            Long userId = userPrincipal.getUser().getId();
            String username = userPrincipal.getUsername();
            presenceService.onSessionConnected(userId, username, sessionId);
            log.debug("STOMP session connected: user={}, sessionId={}", username, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (principal instanceof UsernamePasswordAuthenticationToken auth &&
                auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            Long userId = userPrincipal.getUser().getId();
            String username = userPrincipal.getUsername();
            presenceService.onSessionDisconnected(userId, username, sessionId);
            log.debug("STOMP session disconnected: user={}, sessionId={}", username, sessionId);
        }
    }
}
