package com.collabsphere_first.community.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisRateLimitingAndConcurrencyTest {

    private CommunitiesRateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> redisProvider = mock(ObjectProvider.class);
        when(redisProvider.getIfAvailable()).thenReturn(null); // Local Bucket4j fallback mode

        rateLimitConfig = new CommunitiesRateLimitConfig(redisProvider);
        ReflectionTestUtils.setField(rateLimitConfig, "localFallbackEnabled", true);
        ReflectionTestUtils.setField(rateLimitConfig, "postsPerMinute", 3);
        ReflectionTestUtils.setField(rateLimitConfig, "commentsPerMinute", 5);
        ReflectionTestUtils.setField(rateLimitConfig, "joinsPerMinute", 2);
        ReflectionTestUtils.setField(rateLimitConfig, "invitationsPerHour", 2);
        ReflectionTestUtils.setField(rateLimitConfig, "messagesPerMinute", 5);
        ReflectionTestUtils.setField(rateLimitConfig, "pollsPerMinute", 2);
        ReflectionTestUtils.setField(rateLimitConfig, "reportsPerMinute", 2);
        ReflectionTestUtils.setField(rateLimitConfig, "communityCreatesPerHour", 2);
    }

    @Test
    @DisplayName("Rate limit allows up to capacity and blocks subsequent requests")
    void rateLimit_blocksAfterLimitReached() {
        Long userId = 99L;

        // Post limit set to 3 per minute
        assertThat(rateLimitConfig.consumePost(userId).allowed()).isTrue();
        assertThat(rateLimitConfig.consumePost(userId).allowed()).isTrue();
        assertThat(rateLimitConfig.consumePost(userId).allowed()).isTrue();

        // 4th attempt must be rejected
        var fourth = rateLimitConfig.consumePost(userId);
        assertThat(fourth.allowed()).isFalse();
        assertThat(fourth.retryAfterSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Different users have isolated rate limit buckets")
    void userIsolation_bucketsSeparate() {
        Long userA = 101L;
        Long userB = 102L;

        // Exhaust userA
        rateLimitConfig.consumeJoin(userA);
        rateLimitConfig.consumeJoin(userA);
        assertThat(rateLimitConfig.consumeJoin(userA).allowed()).isFalse();

        // userB must still be allowed
        assertThat(rateLimitConfig.consumeJoin(userB).allowed()).isTrue();
    }

    @Test
    @DisplayName("Concurrent consumption handles requests atomically")
    void concurrentConsumption_atomic() throws InterruptedException {
        Long userId = 200L;
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    var decision = rateLimitConfig.consumeComment(userId);
                    if (decision.allowed()) {
                        allowedCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Limit was configured to 5 comments per minute
        assertThat(allowedCount.get()).isEqualTo(5);
        assertThat(rejectedCount.get()).isEqualTo(5);
    }
}
