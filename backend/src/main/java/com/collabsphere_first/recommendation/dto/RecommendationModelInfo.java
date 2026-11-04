package com.collabsphere_first.recommendation.dto;

/**
 * Model and strategy metadata returned in ranking response.
 */
public record RecommendationModelInfo(
        String name,
        String version,
        String strategy
) {}
