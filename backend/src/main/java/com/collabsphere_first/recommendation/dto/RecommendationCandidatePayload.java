package com.collabsphere_first.recommendation.dto;

import com.collabsphere_first.common.content.ContentRef;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Payload representing an individual candidate deemed eligible by Java.
 */
public record RecommendationCandidatePayload(
        ContentRef contentRef,
        Long authorId,
        Instant createdAt,
        List<String> sources,
        RecommendationFeatures features
) {
    public RecommendationCandidatePayload {
        Objects.requireNonNull(contentRef, "contentRef must not be null");
        sources = sources != null ? sources : List.of();
    }
}
