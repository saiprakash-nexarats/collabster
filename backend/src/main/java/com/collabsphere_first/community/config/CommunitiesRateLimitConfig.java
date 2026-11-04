package com.collabsphere_first.community.config;

import com.collabsphere_first.community.exception.CommunityRateLimitUnavailableException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Redis limits shared by nodes, with Bucket4j fallback when Redis is unavailable.
 */
@Slf4j
@Component
public class CommunitiesRateLimitConfig {

    // private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
    //         "local count = redis.call('INCR', KEYS[1]); if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; return count;", Long.class);

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

    @Value("${collabster.communities.rate-limit.posts-per-minute:5}")
    private int postsPerMinute;

    @Value("${collabster.communities.rate-limit.comments-per-minute:15}")
    private int commentsPerMinute;

    private final ObjectProvider<StringRedisTemplate> redisTemplate;
    private final ConcurrentMap<Key, Entry> buckets = new ConcurrentHashMap<>();

    public CommunitiesRateLimitConfig(ObjectProvider<StringRedisTemplate> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Temporary diagnostic to verify that Spring is loading
     * the local fallback configuration correctly.
     */
    @PostConstruct
    public void logRateLimitConfiguration() {
        log.info(
                "COMMUNITY RATE LIMIT CONFIG: localFallbackEnabled={}, communityCreatesPerHour={}",
                localFallbackEnabled,
                communityCreatesPerHour
        );
    }

    public Decision consumePost(Long userId) {
        return consume(
                "post",
                userId,
                postsPerMinute,
                Duration.ofMinutes(1)
        );
    }

    public Decision consumeComment(Long userId) {
        return consume(
                "comment",
                userId,
                commentsPerMinute,
                Duration.ofMinutes(1)
        );
    }

    // public Decision consumeJoin(Long userId) {
    //     return consume("join", userId, 10, Duration.ofMinutes(1));
    // }

    public Decision consumeJoin(Long userId) {
        return consume(
                "join",
                userId,
                joinsPerMinute,
                Duration.ofMinutes(1)
        );
    }

    // public Decision consumeCommunityCreation(Long userId) {
    //     return consume("community-create", userId, 3, Duration.ofHours(1));
    // }

    public Decision consumeCommunityCreation(Long userId) {
        return consume(
                "community-create",
                userId,
                communityCreatesPerHour,
                Duration.ofHours(1)
        );
    }

    // public Decision consumeInvitation(Long userId) {
    //     return consume("invitation", userId, 10, Duration.ofHours(1));
    // }

    public Decision consumeInvitation(Long userId) {
        return consume(
                "invitation",
                userId,
                invitationsPerHour,
                Duration.ofHours(1)
        );
    }

    // public Decision consumeMessage(Long userId) {
    //     return consume("message", userId, 30, Duration.ofMinutes(1));
    // }

    public Decision consumeMessage(Long userId) {
        return consume(
                "message",
                userId,
                messagesPerMinute,
                Duration.ofMinutes(1)
        );
    }

    public Decision consumePoll(Long userId) {
        return consume(
                "poll",
                userId,
                pollsPerMinute,
                Duration.ofMinutes(1)
        );
    }

    public Decision consumeReport(Long userId) {
        return consume(
                "report",
                userId,
                reportsPerMinute,
                Duration.ofMinutes(1)
        );
    }

    private Decision consume(
            String action,
            Long userId,
            int configuredCapacity,
            Duration period
    ) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID is required for rate limiting"
            );
        }

        int capacity = Math.max(1, configuredCapacity);

        String key = keyPrefix + ":" + action + ":" + userId;

        StringRedisTemplate redis = redisTemplate.getIfAvailable();

        if (redis != null) {
            try {
                return consumeUsingRedis(
                        redis,
                        key,
                        capacity,
                        period
                );
            } catch (RuntimeException unavailable) {
                log.warn(
                        "Redis rate limiter unavailable; action={}, using {} behavior",
                        action,
                        localFallbackEnabled
                                ? "local fallback"
                                : "fail closed"
                );

                if (!localFallbackEnabled) {
                    throw new CommunityRateLimitUnavailableException(
                            "Rate limiting is temporarily unavailable"
                    );
                }
            }
        }

        if (!localFallbackEnabled) {
            throw new CommunityRateLimitUnavailableException(
                    "Rate limiting is temporarily unavailable"
            );
        }

        return consumeUsingLocalFallback(
                action,
                userId,
                capacity,
                period
        );
    }

    private Bucket resolve(
            String action,
            Long userId,
            int capacity,
            Duration period
    ) {
        Key key = new Key(action, userId);
        long now = System.currentTimeMillis();

        return buckets.compute(
                key,
                (ignored, current) -> {
                    if (current == null) {
                        return new Entry(
                                Bucket.builder()
                                        .addLimit(
                                                Bandwidth.builder()
                                                        .capacity(capacity)
                                                        .refillGreedy(capacity, period)
                                                        .build()
                                        )
                                        .build(),
                                now
                        );
                    }

                    current.lastAccess = now;
                    return current;
                }
        ).bucket;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void removeIdleBuckets() {
        long cutoff =
                System.currentTimeMillis()
                        - Duration.ofHours(2).toMillis();

        buckets.entrySet().removeIf(
                entry -> entry.getValue().lastAccess < cutoff
        );
    }

    public record Decision(
            boolean allowed,
            long retryAfterSeconds
    ) {
    }

    private record Key(
            String action,
            Long userId
    ) {
    }

    private static final class Entry {

        private final Bucket bucket;
        private volatile long lastAccess;

        private Entry(
                Bucket bucket,
                long lastAccess
        ) {
            this.bucket = bucket;
            this.lastAccess = lastAccess;
        }
    }

    private Decision consumeUsingRedis(
            StringRedisTemplate redis,
            String key,
            int capacity,
            Duration period
    ) {
        List<?> result = redis.execute(
                INCREMENT_WITH_TTL,
                List.of(key),
                Long.toString(period.toMillis())
        );

        if (result == null || result.size() != 2) {
            throw new IllegalStateException(
                    "Redis rate-limit script returned an invalid result"
            );
        }

        long count =
                ((Number) result.get(0)).longValue();

        long ttlMillis =
                ((Number) result.get(1)).longValue();

        boolean allowed = count <= capacity;

        // Redis should always have a TTL because the script sets it.
        // A non-positive result indicates expiry or an unexpected state.
        long retryAfterSeconds = allowed
                ? 0
                : Math.max(
                        1,
                        (long) Math.ceil(
                                ttlMillis / 1000.0
                        )
                );

        return new Decision(
                allowed,
                retryAfterSeconds
        );
    }

    private Decision consumeUsingLocalFallback(
            String action,
            Long userId,
            int capacity,
            Duration period
    ) {
        Bucket bucket =
                resolve(
                        action,
                        userId,
                        capacity,
                        period
                );

        ConsumptionProbe probe =
                bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return new Decision(true, 0);
        }

        long retryAfterSeconds =
                Math.max(
                        1,
                        (long) Math.ceil(
                                probe.getNanosToWaitForRefill()
                                        / 1_000_000_000.0
                        )
                );

        return new Decision(
                false,
                retryAfterSeconds
        );
    }

    @Value("${collabster.communities.rate-limit.key-prefix:collabster:community:rate-limit:v1}")
    private String keyPrefix;

    @Value("${collabster.communities.rate-limit.local-fallback-enabled:false}")
    private boolean localFallbackEnabled;

    @Value("${collabster.communities.rate-limit.join-per-minute:10}")
    private int joinsPerMinute;

    @Value("${collabster.communities.rate-limit.messages-per-minute:30}")
    private int messagesPerMinute;

    @Value("${collabster.communities.rate-limit.polls-per-minute:10}")
    private int pollsPerMinute;

    @Value("${collabster.communities.rate-limit.reports-per-minute:5}")
    private int reportsPerMinute;

    @Value("${collabster.communities.rate-limit.community-creates-per-hour:3}")
    private int communityCreatesPerHour;

    @Value("${collabster.communities.rate-limit.invitations-per-hour:10}")
    private int invitationsPerHour;
}