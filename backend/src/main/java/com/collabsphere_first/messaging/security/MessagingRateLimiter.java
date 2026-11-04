package com.collabsphere_first.messaging.security;

import com.collabsphere_first.messaging.config.MessagingProperties;
import com.collabsphere_first.messaging.exception.MessagingRateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagingRateLimiter {

    private static final String KEY_PREFIX = "collobster:messages:ratelimit:";

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> INCREMENT_WITH_TTL =
            new DefaultRedisScript<>(
                    """
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                        redis.call('PEXPIRE', KEYS[1], ARGV[1])
                    end
                    local ttl = redis.call('PTTL', KEYS[1])
                    return {count, ttl}
                    """,
                    List.class
            );

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final MessagingProperties properties;
    private final ConcurrentMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    public void checkConversationCreation(Long userId) {
        int limit = properties.getRateLimit().getConversationsPerHour();
        Duration period = Duration.ofHours(1);
        consume("conversation-create", userId, limit, period);
    }

    public void checkMessageSend(Long userId) {
        int limit = properties.getRateLimit().getMessagesPerMinute();
        Duration period = Duration.ofMinutes(1);
        consume("message-send", userId, limit, period);
    }

    public void checkReaction(Long userId) {
        int limit = properties.getRateLimit().getReactionsPerMinute();
        Duration period = Duration.ofMinutes(1);
        consume("reaction", userId, limit, period);
    }

    public void checkTyping(Long userId) {
        int limit = properties.getRateLimit().getTypingPerMinute();
        Duration period = Duration.ofMinutes(1);
        consume("typing", userId, limit, period);
    }

    private void consume(String action, Long userId, int capacity, Duration period) {
        if (userId == null) {
            return;
        }

        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                String key = KEY_PREFIX + action + ":" + userId;
                List<?> result = redis.execute(
                        INCREMENT_WITH_TTL,
                        List.of(key),
                        Long.toString(period.toMillis())
                );

                if (result != null && result.size() == 2) {
                    long count = ((Number) result.get(0)).longValue();
                    if (count > capacity) {
                        long ttlMillis = ((Number) result.get(1)).longValue();
                        long retryAfter = Math.max(1, ttlMillis / 1000);
                        log.warn("Messaging rate limit exceeded for user {} on action {}. Retry after {}s", userId, action, retryAfter);
                        throw new MessagingRateLimitExceededException("Rate limit exceeded for " + action + ". Try again in " + retryAfter + " seconds.");
                    }
                    return;
                }
            } catch (MessagingRateLimitExceededException e) {
                throw e;
            } catch (Exception ex) {
                log.warn("Redis messaging rate limiter error; falling back to local bucket: {}", ex.getMessage());
            }
        }

        // Local Bucket4j fallback
        consumeLocal(action, userId, capacity, period);
    }

    private void consumeLocal(String action, Long userId, int capacity, Duration period) {
        String key = action + ":" + userId;
        Bucket bucket = localBuckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, period).build())
                .build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfter = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            log.warn("Local messaging rate limit exceeded for user {} on action {}. Retry after {}s", userId, action, retryAfter);
            throw new MessagingRateLimitExceededException("Rate limit exceeded for " + action + ". Try again in " + retryAfter + " seconds.");
        }
    }
}
