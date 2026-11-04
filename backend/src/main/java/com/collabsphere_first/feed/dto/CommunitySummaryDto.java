package com.collabsphere_first.feed.dto;

import java.util.UUID;

public record CommunitySummaryDto(
    UUID id,
    String name,
    String slug
) {
    public CommunitySummaryDto(UUID id, String name) {
        this(id, name, null);
    }
}

