package com.collabsphere_first.security.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Tracks authenticated WebSocket/STOMP sessions, their derived JWT expiry timestamps,
 * auth versions, and associated underlying transport sessions.
 * Allows independent session tracking for multi-device support and immediate account-wide revocation.
 */
@Slf4j
@Component
public class WebSocketAuthenticatedSessionRegistry {

    private final ConcurrentMap<String, WebSocketSessionMetadata> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WebSocketSession> transportSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> revokedSessionIds = new ConcurrentHashMap<>();
    private volatile Clock clock = Clock.systemUTC();

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public Clock getClock() {
        return clock;
    }

    public void registerTransportSession(String sessionId, WebSocketSession session) {
        transportSessions.put(sessionId, session);
        WebSocketSessionMetadata meta = activeSessions.get(sessionId);
        if (meta != null) {
            meta.setTransportSession(session);
        }
    }

    public void unregisterTransportSession(String sessionId) {
        transportSessions.remove(sessionId);
        activeSessions.remove(sessionId);
    }

    public void registerSession(String sessionId, Long userId, String username, Instant expiresAt, String endpoint) {
        registerSession(sessionId, userId, username, expiresAt, 0L, endpoint);
    }

    public void registerSession(String sessionId, Long userId, String username, Instant expiresAt, Long authVersion, String endpoint) {
        WebSocketSession transport = transportSessions.get(sessionId);
        WebSocketSessionMetadata meta = WebSocketSessionMetadata.builder()
                .sessionId(sessionId)
                .userId(userId)
                .username(username)
                .connectedAt(clock.instant())
                .expiresAt(expiresAt)
                .authVersion(authVersion != null ? authVersion : 0L)
                .revoked(false)
                .endpoint(endpoint)
                .transportSession(transport)
                .build();
        activeSessions.put(sessionId, meta);
        log.debug("Registered authenticated WebSocket session id={} for userId={} (authVersion={}) expiring at {} on endpoint {}",
                sessionId, userId, authVersion, expiresAt, endpoint);
    }

    public void unregisterSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    public boolean isExpired(String sessionId) {
        WebSocketSessionMetadata meta = activeSessions.get(sessionId);
        if (meta == null) {
            return false;
        }
        return meta.getExpiresAt() != null && !clock.instant().isBefore(meta.getExpiresAt());
    }

    public boolean isRevoked(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        Instant expiry = revokedSessionIds.get(sessionId);
        if (expiry != null) {
            if (clock.instant().isBefore(expiry)) {
                return true;
            } else {
                revokedSessionIds.remove(sessionId);
            }
        }
        WebSocketSessionMetadata meta = activeSessions.get(sessionId);
        return meta != null && meta.isRevoked();
    }

    public void markSessionRevoked(String sessionId) {
        if (sessionId != null) {
            revokedSessionIds.put(sessionId, clock.instant().plus(java.time.Duration.ofMinutes(30)));
            WebSocketSessionMetadata meta = activeSessions.get(sessionId);
            if (meta != null) {
                meta.setRevoked(true);
            }
        }
    }

    public WebSocketSessionMetadata getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public List<WebSocketSessionMetadata> getExpiredSessions() {
        Instant now = clock.instant();
        return activeSessions.values().stream()
                .filter(s -> s.getExpiresAt() != null && !now.isBefore(s.getExpiresAt()))
                .toList();
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public List<WebSocketSessionMetadata> getActiveSessionsForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        Instant now = clock.instant();
        return activeSessions.values().stream()
                .filter(s -> userId.equals(s.getUserId()))
                .filter(s -> !s.isRevoked())
                .filter(s -> s.getExpiresAt() == null || now.isBefore(s.getExpiresAt()))
                .toList();
    }

    public List<WebSocketSessionMetadata> getAllSessionsForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return activeSessions.values().stream()
                .filter(s -> userId.equals(s.getUserId()))
                .toList();
    }

    public Set<Long> getAllActiveUserIds() {
        return activeSessions.values().stream()
                .map(WebSocketSessionMetadata::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public List<WebSocketSessionMetadata> getAllActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }

    public boolean hasActiveSessions(Long userId) {
        return !getActiveSessionsForUser(userId).isEmpty();
    }

    public int getActiveSessionCountForUser(Long userId) {
        return getActiveSessionsForUser(userId).size();
    }

    /**
     * Immediately closes and invalidates all active sessions for a user.
     * Used for account-wide revocation (logout-all, deactivation, deletion, admin suspension).
     *
     * @param userId The ID of the user whose sessions should be closed.
     * @param reason The close reason to send over the WebSocket transport.
     * @return List of session IDs that were terminated.
     */
    public List<String> closeAllSessionsForUser(Long userId, String reason) {
        if (userId == null) {
            return List.of();
        }
        List<WebSocketSessionMetadata> sessions = getAllSessionsForUser(userId);
        List<String> closedSessionIds = new ArrayList<>();
        String closeReason = (reason != null && !reason.isBlank()) ? reason : "Account session revoked";

        for (WebSocketSessionMetadata meta : sessions) {
            meta.setRevoked(true);
            closedSessionIds.add(meta.getSessionId());
            revokedSessionIds.put(meta.getSessionId(), clock.instant().plus(java.time.Duration.ofMinutes(30)));

            if (meta.getTransportSession() != null && meta.getTransportSession().isOpen()) {
                try {
                    meta.getTransportSession().close(new CloseStatus(4401, closeReason));
                } catch (Exception ex) {
                    log.debug("Error closing revoked transport session {}: {}", meta.getSessionId(), ex.getMessage());
                }
            }

            transportSessions.remove(meta.getSessionId());
            activeSessions.remove(meta.getSessionId());
        }

        log.info("Closed {} active WebSocket sessions for userId={} with reason: {}",
                closedSessionIds.size(), userId, closeReason);
        return closedSessionIds;
    }

    public void markUserSessionsRevoked(Long userId) {
        if (userId == null) {
            return;
        }
        for (WebSocketSessionMetadata meta : activeSessions.values()) {
            if (userId.equals(meta.getUserId())) {
                meta.setRevoked(true);
                revokedSessionIds.put(meta.getSessionId(), clock.instant().plus(java.time.Duration.ofMinutes(30)));
            }
        }
    }

    /**
     * Closes and revokes only sessions whose authVersion is strictly older than the minimum required authVersion.
     * Preserves sessions whose authVersion is greater than or equal to minimumRequiredAuthVersion.
     *
     * @param userId The user ID.
     * @param minimumRequiredAuthVersion The minimum auth version required to remain active.
     * @param reason The close reason.
     * @return List of session IDs that were terminated.
     */
    public List<String> closeSessionsOlderThanAuthVersion(Long userId, Long minimumRequiredAuthVersion, String reason) {
        if (userId == null || minimumRequiredAuthVersion == null) {
            return List.of();
        }
        List<WebSocketSessionMetadata> sessions = getAllSessionsForUser(userId);
        List<String> closedSessionIds = new ArrayList<>();
        String closeReason = (reason != null && !reason.isBlank()) ? reason : "Account session revoked";

        for (WebSocketSessionMetadata meta : sessions) {
            long sessionVersion = meta.getAuthVersion() != null ? meta.getAuthVersion() : 0L;
            if (sessionVersion < minimumRequiredAuthVersion) {
                meta.setRevoked(true);
                closedSessionIds.add(meta.getSessionId());
                revokedSessionIds.put(meta.getSessionId(), clock.instant().plus(java.time.Duration.ofMinutes(30)));

                if (meta.getTransportSession() != null && meta.getTransportSession().isOpen()) {
                    try {
                        meta.getTransportSession().close(new CloseStatus(4401, closeReason));
                    } catch (Exception ex) {
                        log.debug("Error closing revoked transport session {}: {}", meta.getSessionId(), ex.getMessage());
                    }
                }

                transportSessions.remove(meta.getSessionId());
                activeSessions.remove(meta.getSessionId());
            }
        }

        if (!closedSessionIds.isEmpty()) {
            log.info("Closed {} outdated WebSocket sessions (older than authVersion={}) for userId={} with reason: {}",
                    closedSessionIds.size(), minimumRequiredAuthVersion, userId, closeReason);
        }
        return closedSessionIds;
    }

    /**
     * Periodically prunes expired entries from the revokedSessionIds map to bound in-memory size.
     */
    public void pruneRevokedSessionIds() {
        Instant now = clock.instant();
        revokedSessionIds.entrySet().removeIf(entry -> entry.getValue() != null && entry.getValue().isBefore(now));
    }
}
