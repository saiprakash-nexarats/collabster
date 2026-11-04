package com.collabsphere_first.explore.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExplorePersonSummary(
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        String bio,
        boolean following,
        boolean connected,
        boolean connectionPending
) {}
