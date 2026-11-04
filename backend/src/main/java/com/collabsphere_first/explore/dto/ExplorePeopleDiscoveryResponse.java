package com.collabsphere_first.explore.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExplorePeopleDiscoveryResponse(
        List<ExplorePersonSummary> items,
        String nextCursor,
        boolean hasMore
) {}
