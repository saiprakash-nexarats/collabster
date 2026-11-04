package com.collabsphere_first.feed.config;

import com.collabsphere_first.feed.session.FeedSessionStore;
import com.collabsphere_first.feed.session.InMemoryFeedSessionStore;
import com.collabsphere_first.feed.session.RedisFeedSessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class FeedSessionConfig {

    /**
     * In-memory session store is STRICTLY forbidden in production.
     * It can only be activated when explicitly configured with "in-memory"
     * (e.g. for unit tests or explicit local/test profiles).
     */
    @Bean
    @ConditionalOnProperty(name = "collobster.feed.session-store", havingValue = "in-memory")
    public FeedSessionStore inMemoryFeedSessionStore(FeedProperties properties) {
        return new InMemoryFeedSessionStore(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "collobster.feed.session.store", havingValue = "in-memory")
    public FeedSessionStore inMemoryFeedSessionStoreDot(FeedProperties properties) {
        return new InMemoryFeedSessionStore(properties);
    }

    /**
     * Production default store: RedisFeedSessionStore.
     * If Redis is unavailable, it throws FeedSessionStorageException (mapped to HTTP 503)
     * and NEVER silently falls back to in-memory store.
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(FeedSessionStore.class)
    public FeedSessionStore redisFeedSessionStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            FeedProperties properties
    ) {
        return new RedisFeedSessionStore(redisTemplate, objectMapper, properties);
    }
}
