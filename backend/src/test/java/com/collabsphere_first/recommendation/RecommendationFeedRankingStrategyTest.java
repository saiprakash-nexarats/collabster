package com.collabsphere_first.recommendation;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.feed.candidate.FeedCandidate;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.feed.ranking.BaselineFeedRankingStrategy;
import com.collabsphere_first.feed.ranking.FeedRankingContext;
import com.collabsphere_first.recommendation.client.RecommendationClient;
import com.collabsphere_first.recommendation.config.RecommendationProperties;
import com.collabsphere_first.recommendation.dto.*;
import com.collabsphere_first.recommendation.exception.RecommendationCircuitBreakerOpenException;
import com.collabsphere_first.recommendation.exception.RecommendationException;
import com.collabsphere_first.recommendation.feature.RecommendationFeatureAssembler;
import com.collabsphere_first.recommendation.strategy.RecommendationFeedRankingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationFeedRankingStrategyTest {

    @Mock
    private RecommendationClient recommendationClient;

    private BaselineFeedRankingStrategy baselineStrategy;
    private RecommendationFeatureAssembler featureAssembler;
    private RecommendationProperties properties;
    private RecommendationFeedRankingStrategy rankingStrategy;
    private FeedProperties feedProperties;
    private FeedRankingContext rankingContext;

    @BeforeEach
    void setUp() {
        baselineStrategy = new BaselineFeedRankingStrategy();
        featureAssembler = new RecommendationFeatureAssembler();
        properties = new RecommendationProperties();
        properties.setEnabled(true);
        properties.setFallbackEnabled(true);

        rankingStrategy = new RecommendationFeedRankingStrategy(
                recommendationClient,
                baselineStrategy,
                featureAssembler,
                properties
        );

        feedProperties = new FeedProperties();
        rankingContext = new FeedRankingContext(feedProperties, Instant.now(), Collections.emptyMap(), 100L);
    }

    private FeedCandidate createCandidate(ContentRef ref, Long authorId, Instant createdAt, Set<FeedSourceType> sources) {
        return new FeedCandidate(ref, authorId, createdAt, sources);
    }

    @Test
    @DisplayName("When recommendation is disabled, immediately delegate to BaselineFeedRankingStrategy")
    void testDisabledModeUsesBaselineStrategy() {
        properties.setEnabled(false);

        ContentRef c1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c2 = ContentRef.platformPost(UUID.randomUUID());
        List<FeedCandidate> candidates = List.of(
                createCandidate(c1, 1L, Instant.now().minusSeconds(100), Set.of(FeedSourceType.FOLLOWING)),
                createCandidate(c2, 2L, Instant.now().minusSeconds(10), Set.of(FeedSourceType.CONNECTION))
        );

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        assertThat(result).hasSize(2);
        // Verify recommendationClient was never invoked
        verifyNoInteractions(recommendationClient);
    }

    @Test
    @DisplayName("Successful Python recommendation ranking produces desired candidate order")
    void testSuccessfulRankingOrder() {
        ContentRef c1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c2 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c3 = ContentRef.communityPost(UUID.randomUUID());

        FeedCandidate cand1 = createCandidate(c1, 1L, Instant.now().minusSeconds(50), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate cand2 = createCandidate(c2, 2L, Instant.now().minusSeconds(40), Set.of(FeedSourceType.CONNECTION));
        FeedCandidate cand3 = createCandidate(c3, 3L, Instant.now().minusSeconds(30), Set.of(FeedSourceType.JOINED_COMMUNITY));

        List<FeedCandidate> candidates = List.of(cand1, cand2, cand3);

        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenAnswer(invocation -> {
                    RecommendationRankRequest req = invocation.getArgument(0);
                    return new RecommendationRankResponse(
                            "1",
                            req.requestId(),
                            new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                            List.of(
                                    new RankedItemResponse(c3, 1, 0.98),
                                    new RankedItemResponse(c1, 2, 0.85),
                                    new RankedItemResponse(c2, 3, 0.62)
                            )
                    );
                });

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).contentRef()).isEqualTo(c3);
        assertThat(result.get(1).contentRef()).isEqualTo(c1);
        assertThat(result.get(2).contentRef()).isEqualTo(c2);
    }

    @Test
    @DisplayName("SECURITY INVARIANT: Python cannot inject unsubmitted / unauthorized candidates (Section 55)")
    void testSecurityRejectionOfInjectedCandidate() {
        ContentRef eligible1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef eligible2 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef injectedUnauthorized = ContentRef.platformPost(UUID.randomUUID());

        FeedCandidate cand1 = createCandidate(eligible1, 1L, Instant.now(), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate cand2 = createCandidate(eligible2, 2L, Instant.now(), Set.of(FeedSourceType.CONNECTION));
        List<FeedCandidate> candidates = List.of(cand1, cand2);

        // Python attempts to return the unauthorized candidate as rank 1
        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenAnswer(invocation -> {
                    RecommendationRankRequest req = invocation.getArgument(0);
                    return new RecommendationRankResponse(
                            "1",
                            req.requestId(),
                            new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                            List.of(
                                    new RankedItemResponse(injectedUnauthorized, 1, 0.999),
                                    new RankedItemResponse(eligible1, 2, 0.850),
                                    new RankedItemResponse(eligible2, 3, 0.700)
                            )
                    );
                });

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        // Verify injected unauthorized ref is NOT present
        assertThat(result).hasSize(2);
        List<ContentRef> resultRefs = result.stream().map(FeedCandidate::contentRef).toList();
        assertThat(resultRefs).doesNotContain(injectedUnauthorized);
        assertThat(resultRefs).containsExactly(eligible1, eligible2);
    }

    @Test
    @DisplayName("Partial ranking response appends remaining candidates using baseline strategy (Section 56)")
    void testPartialResponseHandling() {
        ContentRef a = ContentRef.platformPost(UUID.randomUUID());
        ContentRef b = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c = ContentRef.platformPost(UUID.randomUUID());
        ContentRef d = ContentRef.platformPost(UUID.randomUUID());

        Instant now = Instant.now();
        FeedCandidate candA = createCandidate(a, 1L, now.minusSeconds(100), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate candB = createCandidate(b, 2L, now.minusSeconds(20), Set.of(FeedSourceType.CONNECTION));
        FeedCandidate candC = createCandidate(c, 3L, now.minusSeconds(30), Set.of(FeedSourceType.CONNECTION));
        FeedCandidate candD = createCandidate(d, 4L, now.minusSeconds(200), Set.of(FeedSourceType.PUBLIC_FALLBACK));

        List<FeedCandidate> candidates = List.of(candA, candB, candC, candD);

        // Python returns only C and A
        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenAnswer(invocation -> {
                    RecommendationRankRequest req = invocation.getArgument(0);
                    return new RecommendationRankResponse(
                            "1",
                            req.requestId(),
                            new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                            List.of(
                                    new RankedItemResponse(c, 1, 0.95),
                                    new RankedItemResponse(a, 2, 0.90)
                            )
                    );
                });

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        assertThat(result).hasSize(4);
        // Python-ranked first
        assertThat(result.get(0).contentRef()).isEqualTo(c);
        assertThat(result.get(1).contentRef()).isEqualTo(a);
        // Remaining unranked (B, D) appended in deterministic baseline order
        Set<ContentRef> remaining = Set.of(result.get(2).contentRef(), result.get(3).contentRef());
        assertThat(remaining).containsExactlyInAnyOrder(b, d);
    }

    @Test
    @DisplayName("Same UUID across PLATFORM_POST and COMMUNITY_POST preserved independently (Section 57)")
    void testSameUuidCrossTypePreserved() {
        UUID sharedId = UUID.randomUUID();
        ContentRef platformRef = ContentRef.platformPost(sharedId);
        ContentRef communityRef = ContentRef.communityPost(sharedId);

        FeedCandidate platformCand = createCandidate(platformRef, 1L, Instant.now().minusSeconds(50), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate communityCand = createCandidate(communityRef, 2L, Instant.now().minusSeconds(10), Set.of(FeedSourceType.JOINED_COMMUNITY));

        List<FeedCandidate> candidates = List.of(platformCand, communityCand);

        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenAnswer(invocation -> {
                    RecommendationRankRequest req = invocation.getArgument(0);
                    return new RecommendationRankResponse(
                            "1",
                            req.requestId(),
                            new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                            List.of(
                                    new RankedItemResponse(communityRef, 1, 0.92),
                                    new RankedItemResponse(platformRef, 2, 0.81)
                            )
                    );
                });

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).contentRef()).isEqualTo(communityRef);
        assertThat(result.get(1).contentRef()).isEqualTo(platformRef);
    }

    @Test
    @DisplayName("Recommendation failure gracefully falls back to BaselineFeedRankingStrategy (Section 19, 32)")
    void testFailureFallsBackToBaseline() {
        ContentRef c1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c2 = ContentRef.platformPost(UUID.randomUUID());
        List<FeedCandidate> candidates = List.of(
                createCandidate(c1, 1L, Instant.now().minusSeconds(100), Set.of(FeedSourceType.FOLLOWING)),
                createCandidate(c2, 2L, Instant.now().minusSeconds(10), Set.of(FeedSourceType.CONNECTION))
        );

        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenThrow(new RecommendationException("Connection refused: Python recommendation service down"));

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        // Successfully produced baseline ranking without throwing exception
        assertThat(result).hasSize(2);
        assertThat(result.get(0).contentRef()).isEqualTo(c2); // CONNECTION has higher weight in baseline
        assertThat(result.get(1).contentRef()).isEqualTo(c1);
    }

    @Test
    @DisplayName("Duplicate ContentRef in Python response is deduplicated")
    void testDuplicateResponseDeduplication() {
        ContentRef c1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c2 = ContentRef.platformPost(UUID.randomUUID());
        List<FeedCandidate> candidates = List.of(
                createCandidate(c1, 1L, Instant.now(), Set.of(FeedSourceType.FOLLOWING)),
                createCandidate(c2, 2L, Instant.now(), Set.of(FeedSourceType.CONNECTION))
        );

        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenAnswer(invocation -> {
                    RecommendationRankRequest req = invocation.getArgument(0);
                    return new RecommendationRankResponse(
                            "1",
                            req.requestId(),
                            new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                            List.of(
                                    new RankedItemResponse(c1, 1, 0.95),
                                    new RankedItemResponse(c1, 2, 0.94), // duplicate c1
                                    new RankedItemResponse(c2, 3, 0.80)
                            )
                    );
                });

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).contentRef()).isEqualTo(c1);
        assertThat(result.get(1).contentRef()).isEqualTo(c2);
    }

    @Test
    @DisplayName("When circuit breaker is OPEN, strategy falls back gracefully to BaselineFeedRankingStrategy")
    void testCircuitBreakerOpenFallsBackToBaseline() {
        ContentRef c1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c2 = ContentRef.platformPost(UUID.randomUUID());
        List<FeedCandidate> candidates = List.of(
                createCandidate(c1, 1L, Instant.now().minusSeconds(100), Set.of(FeedSourceType.FOLLOWING)),
                createCandidate(c2, 2L, Instant.now().minusSeconds(10), Set.of(FeedSourceType.CONNECTION))
        );

        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenThrow(new RecommendationCircuitBreakerOpenException("Circuit breaker is OPEN"));

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        assertThat(result).hasSize(2);
        // Baseline ranking prioritizes CONNECTION over FOLLOWING
        assertThat(result.get(0).contentRef()).isEqualTo(c2);
        assertThat(result.get(1).contentRef()).isEqualTo(c1);
    }

    @Test
    @DisplayName("Wrong contract version triggers safe fallback to BaselineFeedRankingStrategy")
    void testWrongContractVersionFallsBackToBaseline() {
        ContentRef c1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c2 = ContentRef.platformPost(UUID.randomUUID());
        List<FeedCandidate> candidates = List.of(
                createCandidate(c1, 1L, Instant.now().minusSeconds(100), Set.of(FeedSourceType.FOLLOWING)),
                createCandidate(c2, 2L, Instant.now().minusSeconds(10), Set.of(FeedSourceType.CONNECTION))
        );

        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenAnswer(invocation -> {
                    RecommendationRankRequest req = invocation.getArgument(0);
                    return new RecommendationRankResponse(
                            "99.0", // unsupported contract version
                            req.requestId(),
                            new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                            List.of(new RankedItemResponse(c1, 1, 0.95), new RankedItemResponse(c2, 2, 0.80))
                    );
                });

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).contentRef()).isEqualTo(c2); // baseline fallback order
        assertThat(result.get(1).contentRef()).isEqualTo(c1);
    }

    @Test
    @DisplayName("RequestId mismatch triggers safe fallback to BaselineFeedRankingStrategy")
    void testWrongRequestIdFallsBackToBaseline() {
        ContentRef c1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c2 = ContentRef.platformPost(UUID.randomUUID());
        List<FeedCandidate> candidates = List.of(
                createCandidate(c1, 1L, Instant.now().minusSeconds(100), Set.of(FeedSourceType.FOLLOWING)),
                createCandidate(c2, 2L, Instant.now().minusSeconds(10), Set.of(FeedSourceType.CONNECTION))
        );

        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenReturn(new RecommendationRankResponse(
                        "1",
                        UUID.randomUUID(), // mismatch requestId
                        new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                        List.of(new RankedItemResponse(c1, 1, 0.95), new RankedItemResponse(c2, 2, 0.80))
                ));

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).contentRef()).isEqualTo(c2); // baseline fallback order
        assertThat(result.get(1).contentRef()).isEqualTo(c1);
    }

    @Test
    @DisplayName("Null response body triggers safe fallback to BaselineFeedRankingStrategy")
    void testNullResponseBodyFallsBackToBaseline() {
        ContentRef c1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c2 = ContentRef.platformPost(UUID.randomUUID());
        List<FeedCandidate> candidates = List.of(
                createCandidate(c1, 1L, Instant.now().minusSeconds(100), Set.of(FeedSourceType.FOLLOWING)),
                createCandidate(c2, 2L, Instant.now().minusSeconds(10), Set.of(FeedSourceType.CONNECTION))
        );

        when(recommendationClient.rank(any(RecommendationRankRequest.class))).thenReturn(null);

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).contentRef()).isEqualTo(c2); // baseline fallback order
        assertThat(result.get(1).contentRef()).isEqualTo(c1);
    }

    @Test
    @DisplayName("Bulkhead saturation throws RecommendationBulkheadFullException and triggers safe fallback to BaselineFeedRankingStrategy")
    void testBulkheadSaturationFallsBackToBaseline() {
        ContentRef c1 = ContentRef.platformPost(UUID.randomUUID());
        ContentRef c2 = ContentRef.platformPost(UUID.randomUUID());
        List<FeedCandidate> candidates = List.of(
                createCandidate(c1, 1L, Instant.now().minusSeconds(100), Set.of(FeedSourceType.FOLLOWING)),
                createCandidate(c2, 2L, Instant.now().minusSeconds(10), Set.of(FeedSourceType.CONNECTION))
        );

        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenThrow(new com.collabsphere_first.recommendation.exception.RecommendationBulkheadFullException("bulkhead capacity saturated (20)"));

        List<FeedCandidate> result = rankingStrategy.rank(candidates, rankingContext);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).contentRef()).isEqualTo(c2); // baseline fallback order
        assertThat(result.get(1).contentRef()).isEqualTo(c1);
    }
}
