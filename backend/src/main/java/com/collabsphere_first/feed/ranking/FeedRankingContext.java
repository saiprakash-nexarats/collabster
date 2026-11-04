package com.collabsphere_first.feed.ranking;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.feed.config.FeedProperties;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record FeedRankingContext(
    FeedProperties properties,
    Instant now,
    Map<ContentRef, Long> likeCounts,
    Long viewerUserId
) {
    public FeedRankingContext {
        now = now != null ? now : Instant.now();
        likeCounts = likeCounts != null ? likeCounts : Collections.emptyMap();
    }

    public FeedRankingContext(FeedProperties properties, Instant now, Map<ContentRef, Long> likeCounts) {
        this(properties, now, likeCounts, null);
    }
}
