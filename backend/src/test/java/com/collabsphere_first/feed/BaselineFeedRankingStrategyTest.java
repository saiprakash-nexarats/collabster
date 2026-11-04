package com.collabsphere_first.feed;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.feed.candidate.FeedCandidate;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.feed.ranking.BaselineFeedRankingStrategy;
import com.collabsphere_first.feed.ranking.FeedRankingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class BaselineFeedRankingStrategyTest {

    private BaselineFeedRankingStrategy rankingStrategy;
    private FeedProperties feedProperties;
    private Instant now;

    @BeforeEach
    void setUp() {
        rankingStrategy = new BaselineFeedRankingStrategy();
        feedProperties = new FeedProperties();
        now = Instant.now();
    }

    @Test
    @DisplayName("Ranking produces deterministic ordering on identical inputs across multiple runs")
    void testDeterministicRanking() {
        FeedCandidate c1 = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                1L,
                now.minus(1, ChronoUnit.HOURS),
                Set.of(FeedSourceType.FOLLOWING)
        );
        FeedCandidate c2 = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                2L,
                now.minus(2, ChronoUnit.HOURS),
                Set.of(FeedSourceType.CONNECTION)
        );
        FeedCandidate c3 = new FeedCandidate(
                ContentRef.communityPost(UUID.randomUUID()),
                3L,
                now.minus(30, ChronoUnit.MINUTES),
                Set.of(FeedSourceType.JOINED_COMMUNITY)
        );

        List<FeedCandidate> candidates = List.of(c1, c2, c3);
        FeedRankingContext context = new FeedRankingContext(feedProperties, now, Collections.emptyMap());

        List<FeedCandidate> run1 = rankingStrategy.rank(candidates, context);
        List<FeedCandidate> run2 = rankingStrategy.rank(candidates, context);

        assertThat(run1).isEqualTo(run2);
        assertThat(run1).hasSize(3);
    }

    @Test
    @DisplayName("Newer content outranks older content when other signals are equal")
    void testRecencyDecay() {
        FeedCandidate newer = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                1L,
                now.minus(10, ChronoUnit.MINUTES),
                Set.of(FeedSourceType.FOLLOWING)
        );
        FeedCandidate older = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                2L,
                now.minus(5, ChronoUnit.DAYS),
                Set.of(FeedSourceType.FOLLOWING)
        );

        FeedRankingContext context = new FeedRankingContext(feedProperties, now, Collections.emptyMap());
        List<FeedCandidate> ranked = rankingStrategy.rank(List.of(older, newer), context);

        assertThat(ranked.get(0)).isEqualTo(newer);
        assertThat(ranked.get(1)).isEqualTo(older);
    }

    @Test
    @DisplayName("Relationship hierarchy prioritizes CONNECTION over FOLLOWING and PUBLIC_FALLBACK")
    void testRelationshipSignal() {
        Instant sameTime = now.minus(1, ChronoUnit.HOURS);
        FeedCandidate connectionPost = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                1L,
                sameTime,
                Set.of(FeedSourceType.CONNECTION)
        );
        FeedCandidate followingPost = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                2L,
                sameTime,
                Set.of(FeedSourceType.FOLLOWING)
        );
        FeedCandidate fallbackPost = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                3L,
                sameTime,
                Set.of(FeedSourceType.PUBLIC_FALLBACK)
        );

        FeedRankingContext context = new FeedRankingContext(feedProperties, now, Collections.emptyMap());
        List<FeedCandidate> ranked = rankingStrategy.rank(List.of(fallbackPost, followingPost, connectionPost), context);

        assertThat(ranked.get(0)).isEqualTo(connectionPost);
        assertThat(ranked.get(1)).isEqualTo(followingPost);
        assertThat(ranked.get(2)).isEqualTo(fallbackPost);
    }

    @Test
    @DisplayName("Engagement signal boosts candidate score with higher like counts")
    void testEngagementSignal() {
        Instant sameTime = now.minus(2, ChronoUnit.HOURS);
        FeedCandidate popularPost = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                1L,
                sameTime,
                Set.of(FeedSourceType.FOLLOWING)
        );
        FeedCandidate unlikedPost = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                2L,
                sameTime,
                Set.of(FeedSourceType.FOLLOWING)
        );

        Map<ContentRef, Long> likeCounts = Map.of(popularPost.contentRef(), 500L, unlikedPost.contentRef(), 0L);
        FeedRankingContext context = new FeedRankingContext(feedProperties, now, likeCounts);

        List<FeedCandidate> ranked = rankingStrategy.rank(List.of(unlikedPost, popularPost), context);
        assertThat(ranked.get(0)).isEqualTo(popularPost);
        assertThat(ranked.get(1)).isEqualTo(unlikedPost);
    }

    @Test
    @DisplayName("Future timestamps are safely handled without throwing exception or creating NaN")
    void testFutureTimestampSafety() {
        FeedCandidate futurePost = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                1L,
                now.plus(1, ChronoUnit.DAYS), // clock skew into the future
                Set.of(FeedSourceType.FOLLOWING)
        );
        FeedCandidate normalPost = new FeedCandidate(
                ContentRef.platformPost(UUID.randomUUID()),
                2L,
                now.minus(1, ChronoUnit.HOURS),
                Set.of(FeedSourceType.FOLLOWING)
        );

        FeedRankingContext context = new FeedRankingContext(feedProperties, now, Collections.emptyMap());
        List<FeedCandidate> ranked = rankingStrategy.rank(List.of(normalPost, futurePost), context);

        assertThat(ranked).hasSize(2);
        assertThat(ranked).contains(futurePost, normalPost);
    }
}
