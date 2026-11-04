package com.collabsphere_first.explore.cursor;

import com.collabsphere_first.common.content.ContentType;

import java.time.Instant;
import java.util.UUID;

public record ExploreSearchCursor(
        String queryHash,
        double rank,
        Instant createdAt,
        UUID id,
        ContentType contentType,
        int position
) {}
