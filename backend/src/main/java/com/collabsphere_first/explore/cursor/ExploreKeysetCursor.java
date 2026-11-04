package com.collabsphere_first.explore.cursor;

import com.collabsphere_first.common.content.ContentType;

import java.time.Instant;
import java.util.UUID;

public record ExploreKeysetCursor(
        Instant createdAt,
        UUID id,
        Long numericId,
        ContentType contentType,
        Double score,
        int position
) {}
