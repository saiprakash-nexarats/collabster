package com.collabsphere_first.security.event;

import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.security.websocket.WebSocketAuthenticatedSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Listens for cross-node account session revocation events published via Redis Pub/Sub.
 * Closes all local WebSocket sessions for the revoked user and evicts presence.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountRevocationListener implements MessageListener {

    private final WebSocketAuthenticatedSessionRegistry sessionRegistry;
    private final MessagingPresenceService presenceService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            AccountRevocationEvent event = objectMapper.readValue(body, AccountRevocationEvent.class);

            if (event == null || event.getUserId() == null || event.getNewAuthVersion() == null || event.getNewAuthVersion() < 0) {
                log.warn("Rejected malformed or invalid account revocation event: {}", event);
                return;
            }

            Long userId = event.getUserId();
            Long targetVersion = event.getNewAuthVersion();
            String reason = event.getReason();

            log.info("Received cross-node account revocation for userId={} (authVersion={}, reason={})",
                    userId, targetVersion, reason);

            java.util.List<String> closedSessionIds = sessionRegistry.closeSessionsOlderThanAuthVersion(
                    userId, targetVersion, "Account session revoked remotely: " + reason);

            // Version-aware presence cleanup: prune only the closed sessions so newer sessions remain ONLINE
            for (String sessionId : closedSessionIds) {
                try {
                    presenceService.handleSessionDisconnected(userId, sessionId);
                } catch (Exception ex) {
                    log.debug("Error cleaning presence for revoked session {}: {}", sessionId, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to process account revocation message from Redis: {}", ex.getMessage());
        }
    }
}
