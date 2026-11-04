package com.collabsphere_first.recommendation;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.recommendation.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationContractDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Serialize and deserialize RecommendationRankRequest with explicit null features")
    void testRankRequestSerializationWithNullFeatures() throws Exception {
        UUID reqId = UUID.randomUUID();
        UUID postUuid = UUID.randomUUID();

        RecommendationFeatures features = new RecommendationFeatures(
                0.85,
                null, // quality missing
                0.32,
                null, // preference missing
                null, // interest missing
                1.0,
                0.0,
                null, // userInteractionCount missing
                40.0,
                null  // creatorReputation missing
        );

        RecommendationCandidatePayload candidate = new RecommendationCandidatePayload(
                ContentRef.platformPost(postUuid),
                42L,
                Instant.parse("2026-09-28T00:00:00Z"),
                List.of("FOLLOWING", "CONNECTION"),
                features
        );

        RecommendationRankRequest request = new RecommendationRankRequest(
                reqId,
                new RecommendationUserContext(105L),
                List.of(candidate)
        );

        String json = objectMapper.writeValueAsString(request);

        // Contract assertions
        assertThat(json).contains("\"contractVersion\":\"1\"");
        assertThat(json).contains("\"featureContractVersion\":\"1\"");
        assertThat(json).contains("\"requestId\":\"" + reqId + "\"");
        assertThat(json).contains("\"type\":\"PLATFORM_POST\"");
        assertThat(json).contains("\"id\":\"" + postUuid + "\"");
        assertThat(json).contains("\"quality\":null");
        assertThat(json).contains("\"preference\":null");
        assertThat(json).contains("\"freshness\":0.85");
        assertThat(json).contains("\"relationship\":1.0");

        // Deserialize back
        RecommendationRankRequest deserialized = objectMapper.readValue(json, RecommendationRankRequest.class);
        assertThat(deserialized.contractVersion()).isEqualTo("1");
        assertThat(deserialized.featureContractVersion()).isEqualTo("1");
        assertThat(deserialized.requestId()).isEqualTo(reqId);
        assertThat(deserialized.candidates()).hasSize(1);

        RecommendationCandidatePayload parsedCandidate = deserialized.candidates().get(0);
        assertThat(parsedCandidate.contentRef().type()).isEqualTo(ContentType.PLATFORM_POST);
        assertThat(parsedCandidate.contentRef().id()).isEqualTo(postUuid);
        assertThat(parsedCandidate.features().freshness()).isEqualTo(0.85);
        assertThat(parsedCandidate.features().quality()).isNull();
    }

    @Test
    @DisplayName("Serialize and deserialize RecommendationRankResponse with model metadata")
    void testRankResponseSerialization() throws Exception {
        UUID reqId = UUID.randomUUID();
        UUID c1Id = UUID.randomUUID();
        UUID c2Id = UUID.randomUUID();

        RecommendationRankResponse response = new RecommendationRankResponse(
                "1",
                "1",
                reqId,
                new RecommendationModelInfo("lightgbm-feed-ranker", "v2", "lightgbm_lambdarank"),
                List.of(
                        new RankedItemResponse(ContentRef.platformPost(c1Id), 1, 0.942),
                        new RankedItemResponse(ContentRef.communityPost(c2Id), 2, 0.715)
                )
        );

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"contractVersion\":\"1\"");
        assertThat(json).contains("\"featureContractVersion\":\"1\"");
        assertThat(json).contains("\"strategy\":\"lightgbm_lambdarank\"");
        assertThat(json).contains("\"PLATFORM_POST\"");
        assertThat(json).contains("\"COMMUNITY_POST\"");

        RecommendationRankResponse deserialized = objectMapper.readValue(json, RecommendationRankResponse.class);
        assertThat(deserialized.contractVersion()).isEqualTo("1");
        assertThat(deserialized.featureContractVersion()).isEqualTo("1");
        assertThat(deserialized.requestId()).isEqualTo(reqId);
        assertThat(deserialized.model().strategy()).isEqualTo("lightgbm_lambdarank");
        assertThat(deserialized.items()).hasSize(2);

        assertThat(deserialized.items().get(0).contentRef().id()).isEqualTo(c1Id);
        assertThat(deserialized.items().get(1).contentRef().type()).isEqualTo(ContentType.COMMUNITY_POST);
    }

    @Test
    @DisplayName("ContentRef cross-type integrity with same UUID")
    void testSameUuidDifferentContentTypeIntegrity() throws Exception {
        UUID sharedUuid = UUID.randomUUID();
        ContentRef platformRef = ContentRef.platformPost(sharedUuid);
        ContentRef communityRef = ContentRef.communityPost(sharedUuid);

        assertThat(platformRef).isNotEqualTo(communityRef);
        assertThat(platformRef.hashCode()).isNotEqualTo(communityRef.hashCode());

        String platformJson = objectMapper.writeValueAsString(platformRef);
        String communityJson = objectMapper.writeValueAsString(communityRef);

        assertThat(platformJson).contains("\"type\":\"PLATFORM_POST\"");
        assertThat(communityJson).contains("\"type\":\"COMMUNITY_POST\"");
    }
}
