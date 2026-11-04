package com.collabsphere_first.feed.dto;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;
import com.collabsphere_first.post.dto.response.PlatformPostMediaResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedItemResponse(
    ContentRef contentRef,
    ContentType contentType,
    List<FeedSourceType> source,
    AuthorSummaryResponse author,
    String title,
    String body,
    List<PlatformPostMediaResponse> media,
    InteractionStateDto interactionState,
    EngagementDto engagement,
    CommunitySummaryDto community,
    Instant createdAt,
    Instant updatedAt
) {}
