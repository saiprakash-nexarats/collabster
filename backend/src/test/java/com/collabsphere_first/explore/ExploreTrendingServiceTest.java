package com.collabsphere_first.explore;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.dto.ExploreTrendingPostsResponse;
import com.collabsphere_first.explore.hydration.ExploreContentHydrator;
import com.collabsphere_first.explore.repository.ExploreTrendingRepository;
import com.collabsphere_first.explore.trending.ExploreTrendingService;
import com.collabsphere_first.explore.trending.TrendingScoreCalculator;
import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.collabsphere_first.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExploreTrendingServiceTest {

    @Mock
    private ExploreTrendingRepository trendingRepository;

    @Mock
    private ExploreContentHydrator contentHydrator;

    private ExploreProperties exploreProperties;
    private TrendingScoreCalculator scoreCalculator;
    private ExploreCursorCodec cursorCodec;
    private ExploreTrendingService trendingService;
    private User viewer;

    @BeforeEach
    void setUp() {
        exploreProperties = new ExploreProperties();
        scoreCalculator = new TrendingScoreCalculator(exploreProperties);
        cursorCodec = new ExploreCursorCodec(new ObjectMapper());

        trendingService = new ExploreTrendingService(
                exploreProperties,
                trendingRepository,
                scoreCalculator,
                contentHydrator,
                cursorCodec
        );

        viewer = User.builder().id(100L).username("viewer").build();
    }

    @Test
    @DisplayName("Trending aggregates interactions, sorts by score, and hydrates eligible items")
    void testTrendingAggregationAndRanking() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ContentRef ref1 = ContentRef.platformPost(id1);
        ContentRef ref2 = ContentRef.communityPost(id2);

        // ref1 has high engagement: 50 likes, 10 shares
        // ref2 has lower engagement: 5 likes
        ExploreTrendingRepository.TrendingRawStats stat1 =
                new ExploreTrendingRepository.TrendingRawStats(ref1, 50, 2, 10, 200, 100_000);
        ExploreTrendingRepository.TrendingRawStats stat2 =
                new ExploreTrendingRepository.TrendingRawStats(ref2, 5, 0, 0, 20, 10_000);

        when(trendingRepository.aggregateTrendingInteractions(any(Instant.class), anyInt()))
                .thenReturn(List.of(stat1, stat2));

        FeedItemResponse item1 = new FeedItemResponse(
                ref1, ContentType.PLATFORM_POST, List.of(), null, null, "High viral post", List.of(), null, null, null, Instant.now(), Instant.now()
        );
        FeedItemResponse item2 = new FeedItemResponse(
                ref2, ContentType.COMMUNITY_POST, List.of(), null, "Lower viral", "Body", List.of(), null, null, null, Instant.now(), Instant.now()
        );

        when(contentHydrator.hydrateAndEnrich(anyList(), eq(viewer)))
                .thenReturn(List.of(item1, item2));

        ExploreTrendingPostsResponse response = trendingService.getTrendingPage(viewer, null, 10);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).contentRef()).isEqualTo(ref1);
        assertThat(response.items().get(1).contentRef()).isEqualTo(ref2);
    }

    @Test
    @DisplayName("Inaccessible or private items dropped by hydrator do not appear in trending response")
    void testInaccessibleTrendingItemDropped() {
        UUID privateId = UUID.randomUUID();
        UUID publicId = UUID.randomUUID();
        ContentRef privateRef = ContentRef.platformPost(privateId);
        ContentRef publicRef = ContentRef.platformPost(publicId);

        ExploreTrendingRepository.TrendingRawStats statPrivate =
                new ExploreTrendingRepository.TrendingRawStats(privateRef, 100, 10, 20, 500, 300_000);
        ExploreTrendingRepository.TrendingRawStats statPublic =
                new ExploreTrendingRepository.TrendingRawStats(publicRef, 20, 2, 5, 100, 50_000);

        when(trendingRepository.aggregateTrendingInteractions(any(Instant.class), anyInt()))
                .thenReturn(List.of(statPrivate, statPublic));

        // Hydrator drops privateRef because viewer cannot access it
        FeedItemResponse publicItem = new FeedItemResponse(
                publicRef, ContentType.PLATFORM_POST, List.of(), null, null, "Public viral post", List.of(), null, null, null, Instant.now(), Instant.now()
        );

        when(contentHydrator.hydrateAndEnrich(anyList(), eq(viewer)))
                .thenReturn(List.of(publicItem));

        ExploreTrendingPostsResponse response = trendingService.getTrendingPage(viewer, null, 10);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).contentRef()).isEqualTo(publicRef);
    }
}
