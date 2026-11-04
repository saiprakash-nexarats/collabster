package com.collabsphere_first.feed.candidate;

import com.collabsphere_first.common.content.ContentRef;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record FeedCandidate(
    ContentRef contentRef,
    Long authorId,
    Instant createdAt,
    Set<FeedSourceType> sources
) {
    public FeedCandidate {
        Objects.requireNonNull(contentRef, "contentRef must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(sources, "sources must not be null");
    }
}
