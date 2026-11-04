package com.collabsphere_first.feed.dto;

import java.util.List;

public record FeedPageResponse(
    List<FeedItemResponse> items,
    String nextCursor,
    boolean hasMore
) {}
