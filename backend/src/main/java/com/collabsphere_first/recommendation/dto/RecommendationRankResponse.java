package com.collabsphere_first.recommendation.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Typed response payload returned from POST /api/v1/internal/recommend/feed/rank.
 */
public record RecommendationRankResponse(
        String contractVersion,
        String featureContractVersion,
        UUID requestId,
        RecommendationModelInfo model,
        List<RankedItemResponse> items
) {
    public static final String CURRENT_FEATURE_CONTRACT_VERSION = "1";

    public RecommendationRankResponse {
        Objects.requireNonNull(contractVersion, "contractVersion must not be null");
        featureContractVersion = featureContractVersion != null ? featureContractVersion : CURRENT_FEATURE_CONTRACT_VERSION;
        Objects.requireNonNull(requestId, "requestId must not be null");
        items = items != null ? items : List.of();
    }

    public RecommendationRankResponse(String contractVersion, UUID requestId, RecommendationModelInfo model, List<RankedItemResponse> items) {
        this(contractVersion, CURRENT_FEATURE_CONTRACT_VERSION, requestId, model, items);
    }
}

