package com.collabsphere_first.security.websocket;

import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

/**
 * Guards the client outbound channel.
 * Prevents passive subscription leakage: if an authenticated session's JWT has expired,
 * no private outbound events are delivered. The frame is dropped and the transport session is closed.
 */
@Slf4j
@Component
public class WebSocketOutboundExpiryInterceptor implements ChannelInterceptor {

    private final WebSocketAuthenticatedSessionRegistry sessionRegistry;
    private final MessagingPresenceService presenceService;

    @Autowired
    public WebSocketOutboundExpiryInterceptor(WebSocketAuthenticatedSessionRegistry sessionRegistry,
                                              MessagingPresenceService presenceService) {
        this.sessionRegistry = sessionRegistry;
        this.presenceService = presenceService;
    }

    public WebSocketOutboundExpiryInterceptor(WebSocketAuthenticatedSessionRegistry sessionRegistry) {
        this(sessionRegistry, null);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String sessionId = accessor.getSessionId();

        if (sessionId != null && (sessionRegistry.isExpired(sessionId) || sessionRegistry.isRevoked(sessionId))) {
            boolean isRevoked = sessionRegistry.isRevoked(sessionId);
            log.warn("Blocked outbound message delivery to {} WebSocket session id={}",
                    isRevoked ? "revoked" : "expired", sessionId);
            WebSocketSessionMetadata meta = sessionRegistry.getSession(sessionId);
            if (meta != null && meta.getTransportSession() != null) {
                try {
                    meta.getTransportSession().close(new CloseStatus(4401, isRevoked ? "Account session revoked" : "Access token expired"));
                } catch (Exception ex) {
                    log.debug("Error closing expired/revoked transport session: {}", ex.getMessage());
                }
            }
            sessionRegistry.unregisterSession(sessionId);
            if (meta != null && meta.getUserId() != null && presenceService != null) {
                try {
                    presenceService.handleSessionDisconnected(meta.getUserId(), sessionId);
                } catch (Exception ex) {
                    log.debug("Error cleaning presence on outbound expiry/revocation: {}", ex.getMessage());
                }
            }
            // Drop outbound message - prevents delivery after expiry or revocation
            return null;
        }

        return message;
    }
}
