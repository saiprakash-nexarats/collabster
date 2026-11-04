package com.collabsphere_first.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Numerical feature signals assembled by Java for Python LightGBM ranker.
 * Null values represent unavailable/missing signals and are transmitted explicitly.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RecommendationFeatures(
        Double freshness,
        Double quality,
        Double popularity,
        Double preference,
        Double interest,
        Double relationship,
        Double community,
        Double userInteractionCount,
        Double postInteractionCount,
        Double creatorReputation
) {}
