package com.collabsphere_first.recommendation.dto;

import com.collabsphere_first.common.content.ContentRef;

import java.util.Objects;

/**
 * Individual ranked item returned by recommendation ranker.
 */
public record RankedItemResponse(
        ContentRef contentRef,
        int rank,
        double score
) {
    public RankedItemResponse {
        Objects.requireNonNull(contentRef, "contentRef must not be null");
    }
}
