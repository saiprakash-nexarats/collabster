package com.collabsphere_first.messaging.presence;

import com.collabsphere_first.messaging.config.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingPresenceServiceImpl implements MessagingPresenceService {

    private static final String PRESENCE_PREFIX = "collobster:messages:presence:";
    private static final String SESSIONS_PREFIX = "collobster:messages:presence:sessions:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final MessagingProperties messagingProperties;

    @Override
    public void onSessionConnected(Long userId, String username, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            log.warn("Redis unavailable for presence session connect (user: {})", userId);
            return;
        }

        try {
            Duration ttl = messagingProperties.getPresenceTtl();
            long ttlMillis = ttl.toMillis();
            long expiryTime = System.currentTimeMillis() + ttlMillis;

            String userPresenceKey = PRESENCE_PREFIX + userId;
            String sessionsKey = SESSIONS_PREFIX + userId;

            redis.opsForZSet().add(sessionsKey, sessionId, (double) expiryTime);
            redis.expire(sessionsKey, ttl);
            redis.opsForValue().set(userPresenceKey, "ONLINE", ttl);

            log.debug("User {} session {} connected, marked ONLINE in ZSET (score: {})", userId, sessionId, expiryTime);
        } catch (Exception ex) {
            log.warn("Failed to update presence on session connect: {}", ex.getMessage());
        }
    }

    @Override
    public void onHeartbeat(Long userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return;
        }

        try {
            Duration ttl = messagingProperties.getPresenceTtl();
            long ttlMillis = ttl.toMillis();
            long expiryTime = System.currentTimeMillis() + ttlMillis;

            String userPresenceKey = PRESENCE_PREFIX + userId;
            String sessionsKey = SESSIONS_PREFIX + userId;

            redis.opsForZSet().add(sessionsKey, sessionId, (double) expiryTime);
            redis.expire(sessionsKey, ttl);
            redis.opsForValue().set(userPresenceKey, "ONLINE", ttl);
            log.debug("Heartbeat refreshed for user {} session {} (score: {})", userId, sessionId, expiryTime);
        } catch (Exception ex) {
            log.warn("Failed to refresh presence heartbeat: {}", ex.getMessage());
        }
    }

    @Override
    public void onSessionDisconnected(Long userId, String username, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return;
        }

        try {
            String userPresenceKey = PRESENCE_PREFIX + userId;
            String sessionsKey = SESSIONS_PREFIX + userId;

            redis.opsForZSet().remove(sessionsKey, sessionId);

            long now = System.currentTimeMillis();
            redis.opsForZSet().removeRangeByScore(sessionsKey, 0, (double) now);

            Long remainingSessions = redis.opsForZSet().zCard(sessionsKey);
            if (remainingSessions == null || remainingSessions <= 0) {
                redis.delete(userPresenceKey);
                redis.delete(sessionsKey);
                log.debug("User {} all sessions disconnected, marked OFFLINE", userId);
            } else {
                log.debug("User {} session {} disconnected, {} active sessions remain in ZSET", userId, sessionId, remainingSessions);
            }
        } catch (Exception ex) {
            log.warn("Failed to update presence on session disconnect: {}", ex.getMessage());
        }
    }

    @Override
    public void removeSession(Long userId, String sessionId) {
        onSessionDisconnected(userId, null, sessionId);
    }

    @Override
    public void handleSessionDisconnected(Long userId, String sessionId) {
        onSessionDisconnected(userId, null, sessionId);
    }

    @Override
    public boolean isUserOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }

        try {
            String sessionsKey = SESSIONS_PREFIX + userId;
            String userPresenceKey = PRESENCE_PREFIX + userId;

            long now = System.currentTimeMillis();
            redis.opsForZSet().removeRangeByScore(sessionsKey, 0, (double) now);

            Long liveCount = redis.opsForZSet().zCard(sessionsKey);
            if (liveCount != null && liveCount > 0) {
                return true;
            }

            redis.delete(userPresenceKey);
            return false;
        } catch (Exception ex) {
            log.warn("Failed to check presence for user {}: {}", userId, ex.getMessage());
            return false;
        }
    }

    @Override
    public void forceOffline(Long userId) {
        if (userId == null) {
            return;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.delete(PRESENCE_PREFIX + userId);
            redis.delete(SESSIONS_PREFIX + userId);
            log.info("Forced presence OFFLINE for user {}", userId);
        } catch (Exception ex) {
            log.warn("Failed to force presence offline for user {}: {}", userId, ex.getMessage());
        }
    }
}
