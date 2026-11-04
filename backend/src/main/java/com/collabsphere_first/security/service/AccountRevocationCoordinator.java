package com.collabsphere_first.security.service;

import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.security.event.AccountRevocationEvent;
import com.collabsphere_first.security.websocket.WebSocketAuthenticatedSessionRegistry;
import com.collabsphere_first.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Coordinates account-wide session revocation across all storage authorities and cluster nodes:
 * 1. PostgreSQL (Durable Authority): Atomically increments User.authVersion.
 * 2. Local Node Registry: Closes all active WebSocket sessions immediately.
 * 3. Realtime Presence: Evicts presence across all devices (forceOffline).
 * 4. Cross-Node Broadcast: Publishes invalidation event via Redis Pub/Sub (with graceful fallback).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRevocationCoordinator {

    public static final String REVOCATION_CHANNEL = "collobster:security:account-revocation";

    private final UserRepository userRepository;
    private final WebSocketAuthenticatedSessionRegistry sessionRegistry;
    private final MessagingPresenceService presenceService;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    /**
     * Executes account-wide revocation.
     *
     * @param userId The ID of the account being revoked.
     * @param reason Human-readable revocation reason.
     */
    @Transactional
    public void revokeAccountSessions(Long userId, String reason) {
        if (userId == null) {
            return;
        }

        // 1. PostgreSQL is the durable authority: atomically increment auth_version
        userRepository.incrementAuthVersion(userId);
        Long newAuthVersion = userRepository.findAuthVersionByUserId(userId).orElse(1L);

        // 2. Immediately close only sessions belonging to previous generations (sessionVersion < newAuthVersion)
        java.util.List<String> closedSessionIds = sessionRegistry.closeSessionsOlderThanAuthVersion(
                userId, newAuthVersion, reason);

        // 3. Evict presence safely: forceOffline if all sessions are gone, otherwise prune only closed sessions
        if (!sessionRegistry.hasActiveSessions(userId)) {
            try {
                presenceService.forceOffline(userId);
            } catch (Exception ex) {
                log.warn("Failed to evict presence for revoked user {}: {}", userId, ex.getMessage());
            }
        } else {
            for (String sessionId : closedSessionIds) {
                try {
                    presenceService.handleSessionDisconnected(userId, sessionId);
                } catch (Exception ex) {
                    log.debug("Error cleaning presence for revoked session {}: {}", sessionId, ex.getMessage());
                }
            }
        }

        // 4. Publish cross-node invalidation event
        publishRevocationEvent(userId, newAuthVersion, reason);

        log.info("Revoked older sessions for userId={} (new authVersion={}): {}", userId, newAuthVersion, reason);
    }

    private void publishRevocationEvent(Long userId, Long newAuthVersion, String reason) {
        Runnable publishAction = () -> {
            StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
            if (redis == null) {
                log.info("Redis unavailable; skipping pub/sub broadcast for userId={}. Durable reconciliation fallback will enforce.", userId);
                return;
            }
            try {
                AccountRevocationEvent event = new AccountRevocationEvent(userId, newAuthVersion, reason, System.currentTimeMillis());
                String json = objectMapper.writeValueAsString(event);
                redis.convertAndSend(REVOCATION_CHANNEL, json);
                log.debug("Published account revocation event to {}: {}", REVOCATION_CHANNEL, json);
            } catch (Exception ex) {
                log.warn("Failed to publish account revocation event to Redis for user {}: {}. Durable reconciliation fallback will enforce.",
                        userId, ex.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
        } else {
            publishAction.run();
        }
    }
}
