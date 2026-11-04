package com.collabsphere_first.feed.hydration;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.feed.dto.CommunitySummaryDto;
import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;
import com.collabsphere_first.post.dto.response.PlatformPostMediaResponse;

import java.time.Instant;
import java.util.List;

public record HydratedFeedItem(
    ContentRef contentRef,
    ContentType contentType,
    AuthorSummaryResponse author,
    String title,
    String body,
    List<PlatformPostMediaResponse> media,
    CommunitySummaryDto community,
    int commentCount,
    Instant createdAt,
    Instant updatedAt
) {}
