package com.collabsphere_first.feed.session;

import com.collabsphere_first.feed.config.FeedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class InMemoryFeedSessionStore implements FeedSessionStore {

    private final FeedProperties properties;
    private final Map<String, ExpiringSession> store = new ConcurrentHashMap<>();

    private record ExpiringSession(
            FeedSession session,
            Instant expiresAt
    ) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @Override
    public void saveSession(FeedSession session, Duration ttl) {
        if (session == null || session.sessionId() == null) {
            return;
        }
        Duration effectiveTtl = ttl != null ? ttl : properties.getSessionTtl();
        Instant expiresAt = Instant.now().plus(effectiveTtl);
        store.put(session.sessionId(), new ExpiringSession(session, expiresAt));
        log.debug("Saved feed session {} to in-memory store with TTL {}", session.sessionId(), effectiveTtl);
    }

    @Override
    public Optional<FeedSession> getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        ExpiringSession entry = store.get(sessionId);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            store.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(entry.session());
    }

    @Override
    public void deleteSession(String sessionId) {
        if (sessionId != null) {
            store.remove(sessionId);
        }
    }

    public void clear() {
        store.clear();
    }
}
