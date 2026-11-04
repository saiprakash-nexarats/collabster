package com.collabsphere_first.recommendation.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Typed request payload for POST /api/v1/internal/recommend/feed/rank.
 */
public record RecommendationRankRequest(
        String contractVersion,
        String featureContractVersion,
        UUID requestId,
        RecommendationUserContext user,
        List<RecommendationCandidatePayload> candidates
) {
    public static final String CURRENT_CONTRACT_VERSION = "1";
    public static final String CURRENT_FEATURE_CONTRACT_VERSION = "1";

    public RecommendationRankRequest {
        Objects.requireNonNull(contractVersion, "contractVersion must not be null");
        featureContractVersion = featureContractVersion != null ? featureContractVersion : CURRENT_FEATURE_CONTRACT_VERSION;
        Objects.requireNonNull(requestId, "requestId must not be null");
        candidates = candidates != null ? candidates : List.of();
    }

    public RecommendationRankRequest(UUID requestId, RecommendationUserContext user, List<RecommendationCandidatePayload> candidates) {
        this(CURRENT_CONTRACT_VERSION, CURRENT_FEATURE_CONTRACT_VERSION, requestId, user, candidates);
    }

    public RecommendationRankRequest(String contractVersion, UUID requestId, RecommendationUserContext user, List<RecommendationCandidatePayload> candidates) {
        this(contractVersion, CURRENT_FEATURE_CONTRACT_VERSION, requestId, user, candidates);
    }
}

