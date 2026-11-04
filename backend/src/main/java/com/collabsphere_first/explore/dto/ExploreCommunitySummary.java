package com.collabsphere_first.explore.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExploreCommunitySummary(
        UUID id,
        String name,
        String slug,
        String description,
        String coverImageUrl,
        long memberCount,
        String category,
        List<String> tags,
        boolean joined,
        boolean canJoin
) {}
