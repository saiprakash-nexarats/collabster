package com.collabsphere_first.feed.session;

import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.feed.exception.FeedSessionStorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class RedisFeedSessionStore implements FeedSessionStore {

    private static final String KEY_PREFIX = "collobster:feed:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FeedProperties properties;

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    @Override
    public void saveSession(FeedSession session, Duration ttl) {
        if (session == null || session.sessionId() == null) {
            return;
        }
        String key = buildKey(session.sessionId());
        Duration effectiveTtl = ttl != null ? ttl : properties.getSessionTtl();
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, json, effectiveTtl);
            log.debug("Saved feed session {} to Redis with TTL {}", session.sessionId(), effectiveTtl);
        } catch (Exception ex) {
            log.error("Failed to save feed session {} to Redis: {}", session.sessionId(), ex.getMessage(), ex);
            throw new FeedSessionStorageException("Unable to persist feed session to Redis", ex);
        }
    }

    @Override
    public Optional<FeedSession> getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        String key = buildKey(sessionId);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            FeedSession session = objectMapper.readValue(json, FeedSession.class);
            return Optional.ofNullable(session);
        } catch (Exception ex) {
            log.error("Failed to retrieve feed session {} from Redis: {}", sessionId, ex.getMessage(), ex);
            throw new FeedSessionStorageException("Unable to retrieve feed session from Redis", ex);
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        String key = buildKey(sessionId);
        try {
            redisTemplate.delete(key);
            log.debug("Deleted feed session {} from Redis", sessionId);
        } catch (Exception ex) {
            log.warn("Failed to delete feed session {} from Redis: {}", sessionId, ex.getMessage());
        }
    }
}
