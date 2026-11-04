package com.collabsphere_first.recommendation;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.feed.candidate.FeedCandidate;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.feed.ranking.BaselineFeedRankingStrategy;
import com.collabsphere_first.feed.ranking.FeedRankingContext;
import com.collabsphere_first.recommendation.client.HttpRecommendationClient;
import com.collabsphere_first.recommendation.client.RecommendationCircuitBreaker;
import com.collabsphere_first.recommendation.config.RecommendationProperties;
import com.collabsphere_first.recommendation.dto.RecommendationRankRequest;
import com.collabsphere_first.recommendation.dto.RecommendationRankResponse;
import com.collabsphere_first.recommendation.feature.RecommendationFeatureAssembler;
import com.collabsphere_first.recommendation.strategy.RecommendationFeedRankingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Section 53: Real Java <-> Python HTTP Integration Test.
 * Executes live HTTP calls against the running FastAPI recommendation service.
 */
class LivePythonRecommendationIntegrationTest {

    private RecommendationProperties properties;
    private RecommendationCircuitBreaker circuitBreaker;
    private HttpRecommendationClient httpClient;
    private RecommendationFeatureAssembler featureAssembler;
    private BaselineFeedRankingStrategy baselineStrategy;
    private RecommendationFeedRankingStrategy rankingStrategy;
    private FeedRankingContext rankingContext;

    @BeforeEach
    void setUp() {
        org.junit.jupiter.api.Assumptions.assumeTrue(isServerAvailable("127.0.0.1", 8000),
                "Live Python recommendation service is not running on port 8000; skipping live integration test");

        properties = new RecommendationProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://127.0.0.1:8000");
        String token = System.getenv("COLLOBSTER_RECOMMENDATION_INTERNAL_TOKEN");
        if (token == null || token.isBlank()) {
            token = System.getProperty("collobster.recommendation.internal-token");
        }
        if (token == null || token.isBlank()) {
            token = System.getenv("RECOMMENDATION_INTERNAL_TOKEN");
        }
        if (token == null) {
            token = "";
        }
        properties.setInternalToken(token);
        properties.setFallbackEnabled(true);
        properties.setConnectTimeoutMs(2000);
        properties.setReadTimeoutMs(5000);

        circuitBreaker = new RecommendationCircuitBreaker(properties);
        httpClient = new HttpRecommendationClient(properties, circuitBreaker);
        featureAssembler = new RecommendationFeatureAssembler();
        baselineStrategy = new BaselineFeedRankingStrategy();

        rankingStrategy = new RecommendationFeedRankingStrategy(
                httpClient,
                baselineStrategy,
                featureAssembler,
                properties
        );

        FeedProperties feedProperties = new FeedProperties();
        rankingContext = new FeedRankingContext(feedProperties, Instant.now(), Collections.emptyMap(), 105L);
    }

    @Test
    @DisplayName("Real Java -> Python HTTP call with mixed PLATFORM_POST and COMMUNITY_POST candidates (Section 53)")
    void testLiveHttpRecommendationRanking() {
        UUID sharedUuid = UUID.randomUUID();
        ContentRef platformShared = ContentRef.platformPost(sharedUuid);
        ContentRef communityShared = ContentRef.communityPost(sharedUuid);
        ContentRef platformOnly = ContentRef.platformPost(UUID.randomUUID());

        Instant now = Instant.now();
        List<FeedCandidate> candidates = List.of(
                new FeedCandidate(platformShared, 10L, now.minusSeconds(30), Set.of(FeedSourceType.FOLLOWING)),
                new FeedCandidate(communityShared, 20L, now.minusSeconds(10), Set.of(FeedSourceType.JOINED_COMMUNITY)),
                new FeedCandidate(platformOnly, 30L, now.minusSeconds(60), Set.of(FeedSourceType.CONNECTION))
        );

        // 1. Test direct HTTP client call to FastAPI
        UUID reqId = UUID.randomUUID();
        RecommendationRankRequest rankRequest = featureAssembler.assemble(candidates, rankingContext, reqId);
        RecommendationRankResponse rankResponse = httpClient.rank(rankRequest);

        assertThat(rankResponse).isNotNull();
        assertThat(rankResponse.contractVersion()).isEqualTo("1");
        assertThat(rankResponse.featureContractVersion()).isEqualTo("1");
        assertThat(rankResponse.requestId()).isEqualTo(reqId);
        assertThat(rankResponse.model().name()).isEqualTo("lightgbm-feed-ranker");
        assertThat(rankResponse.model().strategy()).isEqualTo("lightgbm_lambdarank");
        assertThat(rankResponse.items()).hasSize(3);


        // Verify all 3 submitted ContentRefs are present in Python's response
        Set<ContentRef> returnedRefs = new HashSet<>();
        for (var item : rankResponse.items()) {
            returnedRefs.add(item.contentRef());
            assertThat(item.score()).isBetween(0.0, 1.0);
        }
        assertThat(returnedRefs).containsExactlyInAnyOrder(platformShared, communityShared, platformOnly);

        // 2. Test full strategy execution
        List<FeedCandidate> rankedCandidates = rankingStrategy.rank(candidates, rankingContext);

        assertThat(rankedCandidates).hasSize(3);
        List<ContentRef> strategyOutputRefs = rankedCandidates.stream().map(FeedCandidate::contentRef).toList();
        assertThat(strategyOutputRefs).containsExactlyInAnyOrder(platformShared, communityShared, platformOnly);

        // Verify same-UUID cross-type items both exist and are distinct
        assertThat(strategyOutputRefs.stream().filter(r -> r.id().equals(sharedUuid)).count()).isEqualTo(2);
        assertThat(strategyOutputRefs.stream().filter(r -> r.type() == ContentType.PLATFORM_POST && r.id().equals(sharedUuid)).count()).isEqualTo(1);
        assertThat(strategyOutputRefs.stream().filter(r -> r.type() == ContentType.COMMUNITY_POST && r.id().equals(sharedUuid)).count()).isEqualTo(1);
    }

    private boolean isServerAvailable(String host, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 250);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
