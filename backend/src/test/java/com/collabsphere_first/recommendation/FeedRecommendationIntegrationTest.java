package com.collabsphere_first.recommendation;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.feed.candidate.FeedCandidate;
import com.collabsphere_first.feed.candidate.FeedCandidateProvider;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.feed.cursor.FeedCursorCodec;
import com.collabsphere_first.feed.dto.FeedPageResponse;
import com.collabsphere_first.feed.hydration.FeedContentHydrator;
import com.collabsphere_first.feed.hydration.HydratedFeedItem;
import com.collabsphere_first.feed.ranking.BaselineFeedRankingStrategy;
import com.collabsphere_first.feed.service.impl.FeedServiceImpl;
import com.collabsphere_first.feed.session.FeedSession;
import com.collabsphere_first.feed.session.FeedSessionStore;
import com.collabsphere_first.feed.session.InMemoryFeedSessionStore;
import com.collabsphere_first.interaction.dto.InteractionSummary;
import com.collabsphere_first.interaction.service.InteractionService;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.recommendation.client.HttpRecommendationClient;
import com.collabsphere_first.recommendation.client.RecommendationCircuitBreaker;
import com.collabsphere_first.recommendation.client.RecommendationClient;
import com.collabsphere_first.recommendation.config.RecommendationProperties;
import com.collabsphere_first.recommendation.dto.*;
import com.collabsphere_first.recommendation.exception.RecommendationException;
import com.collabsphere_first.recommendation.feature.RecommendationFeatureAssembler;
import com.collabsphere_first.recommendation.strategy.RecommendationFeedRankingStrategy;
import com.collabsphere_first.user.entity.User;
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

import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;

@ExtendWith(MockitoExtension.class)
class FeedRecommendationIntegrationTest {

    @Mock
    private FeedCandidateProvider candidateProvider;

    @Mock
    private FeedContentHydrator contentHydrator;

    @Mock
    private NetworkAccessService networkAccessService;

    @Mock
    private InteractionService interactionService;

    @Mock
    private RecommendationClient recommendationClient;

    private FeedSessionStore sessionStore;
    private FeedCursorCodec cursorCodec;
    private FeedProperties feedProperties;
    private RecommendationProperties recommendationProperties;
    private BaselineFeedRankingStrategy baselineStrategy;
    private RecommendationFeatureAssembler featureAssembler;
    private RecommendationFeedRankingStrategy recommendationRankingStrategy;
    private FeedServiceImpl feedService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        feedProperties = new FeedProperties();
        feedProperties.setDefaultPageSize(10);
        feedProperties.setMaxPageSize(20);
        sessionStore = new InMemoryFeedSessionStore(feedProperties);
        cursorCodec = new FeedCursorCodec();

        recommendationProperties = new RecommendationProperties();
        recommendationProperties.setEnabled(true);
        recommendationProperties.setFallbackEnabled(true);

        baselineStrategy = new BaselineFeedRankingStrategy();
        featureAssembler = new RecommendationFeatureAssembler();
        recommendationRankingStrategy = new RecommendationFeedRankingStrategy(
                recommendationClient,
                baselineStrategy,
                featureAssembler,
                recommendationProperties
        );

        feedService = new FeedServiceImpl(
                List.of(candidateProvider),
                recommendationRankingStrategy,
                sessionStore,
                cursorCodec,
                contentHydrator,
                networkAccessService,
                interactionService,
                feedProperties
        );

        currentUser = User.builder().id(100L).username("viewerUser").build();

        when(networkAccessService.findFollowingUserIds(eq(100L), anyInt())).thenReturn(List.of(200L));
        when(networkAccessService.findConnectedUserIds(eq(100L), anyInt())).thenReturn(List.of(300L));
        when(networkAccessService.findMutedUserIds(100L)).thenReturn(Collections.emptySet());
        when(networkAccessService.findBlockedOrBlockingUserIds(100L)).thenReturn(Collections.emptySet());
        when(interactionService.findSuppressedContentRefs(100L)).thenReturn(Collections.emptySet());
    }

    private HydratedFeedItem mockHydratedItem(ContentRef ref, Long authorId) {
        Instant now = Instant.now();
        return new HydratedFeedItem(
                ref,
                ref.type(),
                authorId != null ? AuthorSummaryResponse.builder().id(authorId).username("author" + authorId).build() : null,
                "Post title",
                "Post body",
                Collections.emptyList(),
                null,
                0,
                now,
                now
        );
    }

    @Test
    @DisplayName("Feed pipeline orders candidates by Python ML response and returns them via FeedPageResponse (Section 54)")
    void testFeedPipelineUsesPythonRankingOrder() {
        UUID post1Id = UUID.randomUUID();
        UUID post2Id = UUID.randomUUID();
        UUID post3Id = UUID.randomUUID();

        ContentRef ref1 = ContentRef.platformPost(post1Id);
        ContentRef ref2 = ContentRef.platformPost(post2Id);
        ContentRef ref3 = ContentRef.communityPost(post3Id);

        Instant now = Instant.now();
        FeedCandidate cand1 = new FeedCandidate(ref1, 200L, now.minusSeconds(100), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate cand2 = new FeedCandidate(ref2, 300L, now.minusSeconds(200), Set.of(FeedSourceType.CONNECTION));
        FeedCandidate cand3 = new FeedCandidate(ref3, 400L, now.minusSeconds(300), Set.of(FeedSourceType.JOINED_COMMUNITY));

        when(candidateProvider.collect(any())).thenReturn(List.of(cand1, cand2, cand3));

        // Mock interaction summaries
        when(interactionService.getInteractionSummaries(eq(100L), anyCollection()))
                .thenReturn(Map.of(
                        ref1, new InteractionSummary(false, false, 5L),
                        ref2, new InteractionSummary(false, false, 20L),
                        ref3, new InteractionSummary(false, false, 50L)
                ));

        // Python ranker decides custom ML order: ref3 (rank 1), ref1 (rank 2), ref2 (rank 3)
        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenAnswer(invocation -> {
                    RecommendationRankRequest req = invocation.getArgument(0);
                    return new RecommendationRankResponse(
                            "1",
                            req.requestId(),
                            new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                            List.of(
                                    new RankedItemResponse(ref3, 1, 0.98),
                                    new RankedItemResponse(ref1, 2, 0.82),
                                    new RankedItemResponse(ref2, 3, 0.65)
                            )
                    );
                });

        // Mock hydrator
        when(contentHydrator.hydrate(anyCollection(), eq(currentUser)))
                .thenReturn(Map.of(
                        ref3, mockHydratedItem(ref3, 400L),
                        ref1, mockHydratedItem(ref1, 200L),
                        ref2, mockHydratedItem(ref2, 300L)
                ));

        FeedPageResponse page = feedService.getFeed(currentUser, null, 10);

        assertThat(page).isNotNull();
        assertThat(page.items()).hasSize(3);
        // Verify items returned in Python-ranked order: ref3, ref1, ref2
        assertThat(page.items().get(0).contentRef()).isEqualTo(ref3);
        assertThat(page.items().get(1).contentRef()).isEqualTo(ref1);
        assertThat(page.items().get(2).contentRef()).isEqualTo(ref2);
    }

    @Test
    @DisplayName("Feed pipeline falls back to BaselineFeedRankingStrategy when Python recommendation fails (Section 54)")
    void testFeedPipelineFallsBackToBaselineWhenPythonFails() {
        UUID post1Id = UUID.randomUUID();
        UUID post2Id = UUID.randomUUID();

        ContentRef ref1 = ContentRef.platformPost(post1Id);
        ContentRef ref2 = ContentRef.platformPost(post2Id);

        Instant now = Instant.now();
        FeedCandidate cand1 = new FeedCandidate(ref1, 200L, now.minusSeconds(100), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate cand2 = new FeedCandidate(ref2, 300L, now.minusSeconds(10), Set.of(FeedSourceType.CONNECTION));

        when(candidateProvider.collect(any())).thenReturn(List.of(cand1, cand2));

        when(interactionService.getInteractionSummaries(eq(100L), anyCollection()))
                .thenReturn(Map.of(
                        ref1, new InteractionSummary(false, false, 0L),
                        ref2, new InteractionSummary(false, false, 0L)
                ));

        // Python service is down / throws timeout
        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenThrow(new RecommendationException("Read timed out: Python ranker exceeded 1000ms"));

        // Hydration returns items
        when(contentHydrator.hydrate(anyCollection(), eq(currentUser)))
                .thenReturn(Map.of(
                        ref2, mockHydratedItem(ref2, 300L),
                        ref1, mockHydratedItem(ref1, 200L)
                ));

        // Feed request must succeed (HTTP 200 contract) using baseline fallback
        FeedPageResponse page = feedService.getFeed(currentUser, null, 10);

        assertThat(page).isNotNull();
        assertThat(page.items()).hasSize(2);
        // In baseline, CONNECTION has higher weight than FOLLOWING -> ref2 before ref1
        assertThat(page.items().get(0).contentRef()).isEqualTo(ref2);
        assertThat(page.items().get(1).contentRef()).isEqualTo(ref1);
    }

    @Test
    @DisplayName("SECURITY INVARIANT: Python cannot inject blocked/unauthorized content into Feed (Section 55)")
    void testFeedSecurityInjectedCandidateNeverAppears() {
        UUID eligible1Id = UUID.randomUUID();
        UUID eligible2Id = UUID.randomUUID();
        UUID injectedId = UUID.randomUUID();

        ContentRef eligibleRef1 = ContentRef.platformPost(eligible1Id);
        ContentRef eligibleRef2 = ContentRef.platformPost(eligible2Id);
        ContentRef injectedRef = ContentRef.platformPost(injectedId);

        FeedCandidate eligibleCand1 = new FeedCandidate(eligibleRef1, 200L, Instant.now().minusSeconds(50), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate eligibleCand2 = new FeedCandidate(eligibleRef2, 300L, Instant.now().minusSeconds(10), Set.of(FeedSourceType.CONNECTION));
        when(candidateProvider.collect(any())).thenReturn(List.of(eligibleCand1, eligibleCand2));

        when(interactionService.getInteractionSummaries(eq(100L), anyCollection()))
                .thenReturn(Map.of(
                        eligibleRef1, new InteractionSummary(false, false, 0L),
                        eligibleRef2, new InteractionSummary(false, false, 0L)
                ));

        // Python attempts to inject unauthorized post as rank 1
        when(recommendationClient.rank(any(RecommendationRankRequest.class)))
                .thenAnswer(invocation -> {
                    RecommendationRankRequest req = invocation.getArgument(0);
                    return new RecommendationRankResponse(
                            "1",
                            req.requestId(),
                            new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                            List.of(
                                    new RankedItemResponse(injectedRef, 1, 0.999),
                                    new RankedItemResponse(eligibleRef1, 2, 0.800),
                                    new RankedItemResponse(eligibleRef2, 3, 0.700)
                            )
                    );
                });

        when(contentHydrator.hydrate(anyCollection(), eq(currentUser)))
                .thenReturn(Map.of(
                        eligibleRef1, mockHydratedItem(eligibleRef1, 200L),
                        eligibleRef2, mockHydratedItem(eligibleRef2, 300L)
                ));

        FeedPageResponse page = feedService.getFeed(currentUser, null, 10);

        assertThat(page).isNotNull();
        assertThat(page.items()).hasSize(2);
        // Injected ref is completely excluded
        List<ContentRef> returnedRefs = page.items().stream().map(com.collabsphere_first.feed.dto.FeedItemResponse::contentRef).toList();
        assertThat(returnedRefs).doesNotContain(injectedRef);
        assertThat(returnedRefs).containsExactly(eligibleRef1, eligibleRef2);
    }

    @Test
    @DisplayName("FAILURE INTEGRATION: Feed pipeline with Python unavailable returns HTTP 200, uses baseline fallback, opens circuit, and preserves uncorrupted FeedSession")
    void testFeedPipelineWithCircuitBreakerOpeningUnderRepeatedDownFailures() {
        RecommendationProperties downProperties = new RecommendationProperties();
        downProperties.setEnabled(true);
        downProperties.setBaseUrl("http://127.0.0.1:59999"); // Unavailable port
        downProperties.setConnectTimeoutMs(100);
        downProperties.setReadTimeoutMs(100);
        downProperties.setFallbackEnabled(true);
        downProperties.getCircuitBreaker().setEnabled(true);
        downProperties.getCircuitBreaker().setMinimumNumberOfCalls(3);
        downProperties.getCircuitBreaker().setSlidingWindowSize(5);
        downProperties.getCircuitBreaker().setFailureRateThreshold(50.0f);

        RecommendationCircuitBreaker liveCircuitBreaker = new RecommendationCircuitBreaker(downProperties);
        HttpRecommendationClient downClient = new HttpRecommendationClient(downProperties, liveCircuitBreaker);

        RecommendationFeedRankingStrategy liveStrategy = new RecommendationFeedRankingStrategy(
                downClient,
                baselineStrategy,
                featureAssembler,
                downProperties
        );

        FeedServiceImpl liveFeedService = new FeedServiceImpl(
                List.of(candidateProvider),
                liveStrategy,
                sessionStore,
                cursorCodec,
                contentHydrator,
                networkAccessService,
                interactionService,
                feedProperties
        );

        UUID post1Id = UUID.randomUUID();
        UUID post2Id = UUID.randomUUID();
        ContentRef ref1 = ContentRef.platformPost(post1Id);
        ContentRef ref2 = ContentRef.platformPost(post2Id);

        Instant now = Instant.now();
        FeedCandidate cand1 = new FeedCandidate(ref1, 200L, now.minusSeconds(100), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate cand2 = new FeedCandidate(ref2, 300L, now.minusSeconds(10), Set.of(FeedSourceType.CONNECTION));

        when(candidateProvider.collect(any())).thenReturn(List.of(cand1, cand2));
        when(interactionService.getInteractionSummaries(eq(100L), anyCollection()))
                .thenReturn(Map.of(ref1, new InteractionSummary(false, false, 0L),
                        ref2, new InteractionSummary(false, false, 0L)));
        when(contentHydrator.hydrate(anyCollection(), eq(currentUser)))
                .thenReturn(Map.of(ref2, mockHydratedItem(ref2, 300L),
                        ref1, mockHydratedItem(ref1, 200L)));

        // Execute 3 calls with Python down with limit=1 to produce nextCursor with sessionId
        for (int i = 0; i < 3; i++) {
            FeedPageResponse page = liveFeedService.getFeed(currentUser, null, 1);
            assertThat(page).isNotNull();
            assertThat(page.items()).hasSize(1);
            assertThat(page.hasMore()).isTrue();
            assertThat(page.nextCursor()).isNotNull();

            // Baseline ranking prioritizes CONNECTION over FOLLOWING
            assertThat(page.items().get(0).contentRef()).isEqualTo(ref2);

            // Verify session store contains valid uncorrupted session with both ordered candidates
            var cursor = cursorCodec.decode(page.nextCursor());
            FeedSession session = sessionStore.getSession(cursor.sessionId()).orElse(null);
            assertThat(session).isNotNull();
            assertThat(session.orderedContentRefs()).hasSize(2);
            assertThat(session.orderedContentRefs().get(0)).isEqualTo(ref2);
            assertThat(session.orderedContentRefs().get(1)).isEqualTo(ref1);
        }

        // Circuit breaker must now be OPEN
        assertThat(liveCircuitBreaker.isOpen()).isTrue();

        // 4th call: circuit breaker is OPEN, call fails fast and Feed still returns HTTP 200 with baseline ranking
        FeedPageResponse fastFailPage = liveFeedService.getFeed(currentUser, null, 1);
        assertThat(fastFailPage).isNotNull();
        assertThat(fastFailPage.items()).hasSize(1);
        assertThat(fastFailPage.items().get(0).contentRef()).isEqualTo(ref2);

        var fastFailCursor = cursorCodec.decode(fastFailPage.nextCursor());
        FeedSession fastFailSession = sessionStore.getSession(fastFailCursor.sessionId()).orElse(null);
        assertThat(fastFailSession).isNotNull();
        assertThat(fastFailSession.orderedContentRefs()).hasSize(2);
    }
}
