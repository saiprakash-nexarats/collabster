package com.collabsphere_first.explore.dto;

import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExploreRecentPostsResponse(
        List<FeedItemResponse> items,
        String nextCursor,
        boolean hasMore
) {}
