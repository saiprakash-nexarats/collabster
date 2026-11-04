package com.collabsphere_first.security.websocket;

import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Periodically detects expired or revoked WebSocket sessions, terminates their transport connections,
 * cleans up presence state, and removes them from active registries.
 * Provides durable reconciliation fallback for missed cross-node pub/sub revocation signals.
 */
@Slf4j
@Component
public class WebSocketSessionReaper {

    private final WebSocketAuthenticatedSessionRegistry sessionRegistry;
    private final MessagingPresenceService presenceService;
    private final UserRepository userRepository;

    @Autowired
    public WebSocketSessionReaper(
            WebSocketAuthenticatedSessionRegistry sessionRegistry,
            MessagingPresenceService presenceService,
            @Autowired(required = false) UserRepository userRepository
    ) {
        this.sessionRegistry = sessionRegistry;
        this.presenceService = presenceService;
        this.userRepository = userRepository;
    }

    public WebSocketSessionReaper(
            WebSocketAuthenticatedSessionRegistry sessionRegistry,
            MessagingPresenceService presenceService
    ) {
        this(sessionRegistry, presenceService, null);
    }

    @Scheduled(fixedDelayString = "${collobster.websocket.reaper-interval-ms:5000}")
    public void reapSessions() {
        reapExpiredSessions();
        reapRevokedSessions();
        sessionRegistry.pruneRevokedSessionIds();
    }

    public void reapExpiredSessions() {
        List<WebSocketSessionMetadata> expired = sessionRegistry.getExpiredSessions();
        if (expired.isEmpty()) {
            return;
        }

        for (WebSocketSessionMetadata meta : expired) {
            log.info("Reaping expired WebSocket session id={} for userId={} on endpoint {}",
                    meta.getSessionId(), meta.getUserId(), meta.getEndpoint());

            // 1. Terminate underlying transport session if active
            if (meta.getTransportSession() != null && meta.getTransportSession().isOpen()) {
                try {
                    meta.getTransportSession().close(new CloseStatus(4401, "Access token expired"));
                } catch (Exception ex) {
                    log.debug("Error closing expired WebSocket session {}: {}", meta.getSessionId(), ex.getMessage());
                }
            }

            // 2. Unregister session from in-memory registry
            sessionRegistry.unregisterSession(meta.getSessionId());

            // 3. Clean presence state for this specific session
            try {
                presenceService.handleSessionDisconnected(meta.getUserId(), meta.getSessionId());
            } catch (Exception ex) {
                log.debug("Error evicting presence during session reap: {}", ex.getMessage());
            }

            int remainingSessions = sessionRegistry.getActiveSessionCountForUser(meta.getUserId());
            log.info("Session {} reaped for userId={}; remaining active registry sessions: {}",
                    meta.getSessionId(), meta.getUserId(), remainingSessions);
        }
    }

    public void reapRevokedSessions() {
        if (userRepository == null) {
            return;
        }

        Set<Long> activeUserIds = sessionRegistry.getAllActiveUserIds();
        if (activeUserIds.isEmpty()) {
            return;
        }

        try {
            Map<Long, UserAuthState> stateMap = new HashMap<>();
            List<Long> idList = new java.util.ArrayList<>(activeUserIds);
            int chunkSize = 500;
            for (int i = 0; i < idList.size(); i += chunkSize) {
                List<Long> chunk = idList.subList(i, Math.min(i + chunkSize, idList.size()));
                List<Object[]> rows = userRepository.findAuthStatesByUserIds(new java.util.HashSet<>(chunk));
                for (Object[] row : rows) {
                    Long uid = (Long) row[0];
                    Long version = (Long) row[1];
                    Boolean enabled = (Boolean) row[2];
                    Boolean deact = (Boolean) row[3];
                    Boolean del = (Boolean) row[4];
                    stateMap.put(uid, new UserAuthState(version, enabled, deact, del));
                }
            }

            for (WebSocketSessionMetadata meta : sessionRegistry.getAllActiveSessions()) {
                if (meta == null || meta.getUserId() == null) {
                    continue;
                }
                Long uid = meta.getUserId();
                UserAuthState state = stateMap.get(uid);

                boolean shouldRevoke = false;
                String reason = "Session auth version revoked";

                long sessionAuthVersion = meta.getAuthVersion() != null ? meta.getAuthVersion() : 0L;
                long requiredAuthVersion = (state != null && state.authVersion != null) ? state.authVersion : 0L;

                if (state == null) {
                    shouldRevoke = true;
                    reason = "User account not found";
                } else if (!Boolean.TRUE.equals(state.accountEnabled)
                        || Boolean.TRUE.equals(state.deactivated)
                        || Boolean.TRUE.equals(state.deleted)) {
                    shouldRevoke = true;
                    reason = "User account disabled or deleted";
                } else if (sessionAuthVersion < requiredAuthVersion) {
                    shouldRevoke = true;
                    reason = "Session authVersion outdated (" + sessionAuthVersion + " < " + requiredAuthVersion + ")";
                }

                if (shouldRevoke) {
                    log.info("Reaping revoked WebSocket session id={} for userId={} on endpoint {} reason: {}",
                            meta.getSessionId(), uid, meta.getEndpoint(), reason);

                    sessionRegistry.markSessionRevoked(meta.getSessionId());
                    meta.setRevoked(true);
                    if (meta.getTransportSession() != null && meta.getTransportSession().isOpen()) {
                        try {
                            meta.getTransportSession().close(new CloseStatus(4401, reason));
                        } catch (Exception ex) {
                            log.debug("Error closing revoked transport session {}: {}", meta.getSessionId(), ex.getMessage());
                        }
                    }

                    sessionRegistry.unregisterSession(meta.getSessionId());

                    try {
                        presenceService.handleSessionDisconnected(uid, meta.getSessionId());
                    } catch (Exception ex) {
                        log.debug("Error cleaning presence during session revocation reap: {}", ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Error during batch session revocation check: {}", ex.getMessage());
        }
    }

    private record UserAuthState(Long authVersion, Boolean accountEnabled, Boolean deactivated, Boolean deleted) {}
}

