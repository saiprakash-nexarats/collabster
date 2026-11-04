package com.collabsphere_first.explore.dto;

import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExploreGroupedSearchResponse(
        String query,
        ExploreSection<FeedItemResponse> posts,
        ExploreSection<ExplorePersonSummary> people,
        ExploreSection<ExploreCommunitySummary> communities
) {}
