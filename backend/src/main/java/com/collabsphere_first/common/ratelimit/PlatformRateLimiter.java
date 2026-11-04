package com.collabsphere_first.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
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
public class PlatformRateLimiter {

    private static final String KEY_PREFIX = "collobster:ratelimit:";

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
    private final ClientIpResolver clientIpResolver;
    private final ConcurrentMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public PlatformRateLimiter(ObjectProvider<StringRedisTemplate> redisTemplateProvider, ClientIpResolver clientIpResolver) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.clientIpResolver = clientIpResolver != null ? clientIpResolver : new ClientIpResolver("127.0.0.1,::1");
    }

    public PlatformRateLimiter(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this(redisTemplateProvider, new ClientIpResolver("127.0.0.1,::1"));
    }

    /**
     * Resolves abuse key with top priority on authenticated user ID.
     * Prevents IP rotation from bypassing rate limits for authenticated users.
     */
    public String resolveAbuseKey(jakarta.servlet.http.HttpServletRequest request, Long userId) {
        if (userId != null) {
            return "user:" + userId;
        }
        return "ip:" + clientIpResolver.resolveClientIp(request);
    }

    public String resolveClientIp(jakarta.servlet.http.HttpServletRequest request) {
        return clientIpResolver.resolveClientIp(request);
    }

    public void checkRequestRate(String action, jakarta.servlet.http.HttpServletRequest request, Long userId, int maxRequests, Duration period) {
        String abuseKey = resolveAbuseKey(request, userId);
        checkLimit(action, abuseKey, maxRequests, period);
    }

    public void checkLimit(String action, String key, int maxRequests, Duration period) {
        if (!tryAcquire(action, key, maxRequests, period)) {
            long retryAfterSeconds = Math.max(1, period.toSeconds());
            throw new PlatformRateLimitExceededException(
                    "Rate limit exceeded for " + action + ". Please try again later.",
                    retryAfterSeconds
            );
        }
    }

    public boolean tryAcquire(String action, String key, int maxRequests, Duration period) {
        if (key == null || key.isBlank() || action == null) {
            return true;
        }

        String redisKey = KEY_PREFIX + action + ":" + key;
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();

        if (redis != null) {
            try {
                List<?> result = redis.execute(
                        INCREMENT_WITH_TTL,
                        List.of(redisKey),
                        String.valueOf(period.toMillis())
                );

                if (result != null && result.size() >= 2) {
                    long count = ((Number) result.get(0)).longValue();
                    return count <= maxRequests;
                }
            } catch (Exception ex) {
                log.warn("Redis rate limiter failed for action {}: {}. Falling back to in-memory rate limiting.", action, ex.getMessage());
            }
        }

        // In-memory fallback
        Bucket bucket = localBuckets.computeIfAbsent(redisKey, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder().capacity(maxRequests).refillGreedy(maxRequests, period).build())
                        .build()
        );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        return probe.isConsumed();
    }

    // Convenience domain rate checks
    public void checkReportCreation(Long userId) {
        checkLimit("report-create", String.valueOf(userId), 10, Duration.ofMinutes(5));
    }

    public void checkPlatformPostCreation(Long userId) {
        checkLimit("post-create", String.valueOf(userId), 20, Duration.ofMinutes(1));
    }

    public void checkFollowAction(Long userId) {
        checkLimit("follow-user", String.valueOf(userId), 60, Duration.ofMinutes(1));
    }

    public void checkConnectionRequest(Long userId) {
        checkLimit("connection-request", String.valueOf(userId), 30, Duration.ofMinutes(1));
    }

    public void checkInteraction(String interactionType, Long userId) {
        checkLimit("interaction-" + interactionType, String.valueOf(userId), 120, Duration.ofMinutes(1));
    }

    public void checkSearch(Long userId) {
        checkLimit("search-query", String.valueOf(userId), 60, Duration.ofMinutes(1));
    }

    public void checkMediaUpload(Long userId) {
        checkLimit("media-upload", String.valueOf(userId), 20, Duration.ofMinutes(1));
    }
}
