package com.collabsphere_first.feed;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.feed.session.FeedSession;
import com.collabsphere_first.feed.session.InMemoryFeedSessionStore;
import com.collabsphere_first.feed.session.RedisFeedSessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedSessionStoreTest {

    private FeedProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new FeedProperties();
        properties.setSessionTtl(Duration.ofMinutes(15));
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("InMemoryFeedSessionStore saves, retrieves, and deletes sessions")
    void testInMemoryFeedSessionStore() {
        InMemoryFeedSessionStore store = new InMemoryFeedSessionStore(properties);

        String sessionId = UUID.randomUUID().toString();
        List<ContentRef> refs = List.of(ContentRef.platformPost(UUID.randomUUID()), ContentRef.communityPost(UUID.randomUUID()));
        Map<ContentRef, Set<FeedSourceType>> sources = Map.of(refs.get(0), Set.of(FeedSourceType.FOLLOWING));

        FeedSession session = FeedSession.create(sessionId, 100L, Instant.now(), refs, sources);

        store.saveSession(session, Duration.ofMinutes(10));

        Optional<FeedSession> retrieved = store.getSession(sessionId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().sessionId()).isEqualTo(sessionId);
        assertThat(retrieved.get().userId()).isEqualTo(100L);
        assertThat(retrieved.get().orderedContentRefs()).hasSize(2);
        assertThat(retrieved.get().getSources(refs.get(0))).containsExactly(FeedSourceType.FOLLOWING);

        store.deleteSession(sessionId);
        assertThat(store.getSession(sessionId)).isEmpty();
    }

    @Test
    @DisplayName("InMemoryFeedSessionStore respects expiration TTL")
    void testInMemoryExpiration() throws InterruptedException {
        InMemoryFeedSessionStore store = new InMemoryFeedSessionStore(properties);

        String sessionId = UUID.randomUUID().toString();
        FeedSession session = FeedSession.create(sessionId, 100L, Instant.now(), Collections.emptyList(), Collections.emptyMap());

        // Save with 10 ms TTL
        store.saveSession(session, Duration.ofMillis(10));
        Thread.sleep(25);

        assertThat(store.getSession(sessionId)).isEmpty();
    }

    @Test
    @DisplayName("RedisFeedSessionStore saves, retrieves, serializes JSON, and deletes sessions against live Redis container")
    void testRedisFeedSessionStoreLive() {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);

        RedisFeedSessionStore store = new RedisFeedSessionStore(redisTemplate, objectMapper, properties);

        String sessionId = UUID.randomUUID().toString();
        ContentRef ref1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef ref2 = ContentRef.communityPost(UUID.randomUUID());
        List<ContentRef> refs = List.of(ref1, ref2);
        Map<ContentRef, Set<FeedSourceType>> sources = Map.of(
                ref1, Set.of(FeedSourceType.FOLLOWING, FeedSourceType.CONNECTION),
                ref2, Set.of(FeedSourceType.JOINED_COMMUNITY)
        );

        FeedSession session = FeedSession.create(sessionId, 200L, Instant.now(), refs, sources);

        try {
            store.saveSession(session, Duration.ofMinutes(5));

            Optional<FeedSession> retrieved = store.getSession(sessionId);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().sessionId()).isEqualTo(sessionId);
            assertThat(retrieved.get().userId()).isEqualTo(200L);
            assertThat(retrieved.get().orderedContentRefs()).containsExactly(ref1, ref2);
            assertThat(retrieved.get().getSources(ref1)).contains(FeedSourceType.FOLLOWING, FeedSourceType.CONNECTION);
            assertThat(retrieved.get().getSources(ref2)).containsExactly(FeedSourceType.JOINED_COMMUNITY);

            // Clean up
            store.deleteSession(sessionId);
            assertThat(store.getSession(sessionId)).isEmpty();
        } finally {
            connectionFactory.destroy();
        }
    }

    @Test
    @DisplayName("RedisFeedSessionStore handles identical UUID across different ContentTypes with independent source metadata")
    void testRedisContentRefCollisionSafety() {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);

        RedisFeedSessionStore store = new RedisFeedSessionStore(redisTemplate, objectMapper, properties);

        String sessionId = UUID.randomUUID().toString();
        UUID sharedId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        ContentRef platformRef = ContentRef.platformPost(sharedId);
        ContentRef communityRef = ContentRef.communityPost(sharedId);

        List<ContentRef> orderedRefs = List.of(platformRef, communityRef);
        Map<ContentRef, Set<FeedSourceType>> sources = Map.of(
                platformRef, Set.of(FeedSourceType.FOLLOWING),
                communityRef, Set.of(FeedSourceType.JOINED_COMMUNITY)
        );

        FeedSession session = FeedSession.create(sessionId, 300L, Instant.now(), orderedRefs, sources);

        try {
            store.saveSession(session, Duration.ofMinutes(5));

            Optional<FeedSession> retrieved = store.getSession(sessionId);
            assertThat(retrieved).isPresent();

            FeedSession loaded = retrieved.get();
            assertThat(loaded.orderedContentRefs()).hasSize(2);
            assertThat(loaded.orderedContentRefs().get(0)).isEqualTo(platformRef);
            assertThat(loaded.orderedContentRefs().get(1)).isEqualTo(communityRef);

            // Sources must remain strictly independent
            assertThat(loaded.getSources(platformRef)).containsExactly(FeedSourceType.FOLLOWING);
            assertThat(loaded.getSources(communityRef)).containsExactly(FeedSourceType.JOINED_COMMUNITY);

            // Clean up
            store.deleteSession(sessionId);
        } finally {
            connectionFactory.destroy();
        }
    }

    @Test
    @DisplayName("Redis outage throws FeedSessionStorageException and never silently falls back to in-memory")
    void testRedisOutageThrowsStorageException() {
        // Mock redisTemplate that throws on operation
        StringRedisTemplate brokenRedis = org.mockito.Mockito.mock(StringRedisTemplate.class);
        org.mockito.Mockito.when(brokenRedis.opsForValue()).thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Cannot connect to Redis"));

        RedisFeedSessionStore store = new RedisFeedSessionStore(brokenRedis, objectMapper, properties);

        String sessionId = UUID.randomUUID().toString();
        FeedSession session = FeedSession.create(sessionId, 400L, Instant.now(), Collections.emptyList(), Collections.emptyMap());

        assertThatThrownBy(() -> store.saveSession(session, Duration.ofMinutes(5)))
                .isInstanceOf(com.collabsphere_first.feed.exception.FeedSessionStorageException.class)
                .hasMessageContaining("Unable to persist feed session to Redis");

        assertThatThrownBy(() -> store.getSession(sessionId))
                .isInstanceOf(com.collabsphere_first.feed.exception.FeedSessionStorageException.class)
                .hasMessageContaining("Unable to retrieve feed session from Redis");
    }

    @Test
    @DisplayName("FeedSessionConfig defaults to RedisFeedSessionStore in production and only loads InMemory on explicit configuration")
    void testProductionSessionStoreConfiguration() {
        org.springframework.boot.test.context.runner.ApplicationContextRunner runner =
                new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                        .withUserConfiguration(com.collabsphere_first.feed.config.FeedSessionConfig.class)
                        .withBean(StringRedisTemplate.class, () -> org.mockito.Mockito.mock(StringRedisTemplate.class))
                        .withBean(ObjectMapper.class, () -> objectMapper)
                        .withBean(FeedProperties.class, () -> properties);

        // Default: Redis is loaded, in-memory is NOT loaded
        runner.run(context -> {
            assertThat(context).hasSingleBean(com.collabsphere_first.feed.session.FeedSessionStore.class);
            assertThat(context).hasSingleBean(RedisFeedSessionStore.class);
            assertThat(context).doesNotHaveBean(InMemoryFeedSessionStore.class);
        });

        // Explicit in-memory configuration
        runner.withPropertyValues("collobster.feed.session-store=in-memory").run(context -> {
            assertThat(context).hasSingleBean(com.collabsphere_first.feed.session.FeedSessionStore.class);
            assertThat(context).hasSingleBean(InMemoryFeedSessionStore.class);
            assertThat(context).doesNotHaveBean(RedisFeedSessionStore.class);
        });
    }
}
