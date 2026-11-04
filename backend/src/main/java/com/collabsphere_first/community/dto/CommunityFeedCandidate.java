package com.collabsphere_first.community.dto;

import java.time.Instant;
import java.util.UUID;

public record CommunityFeedCandidate(
    UUID postId,
    Long authorId,
    Instant createdAt,
    UUID communityId,
    String communityName,
    String communitySlug
) {
    public CommunityFeedCandidate(UUID postId, Long authorId, Instant createdAt, UUID communityId, String communityName) {
        this(postId, authorId, createdAt, communityId, communityName, null);
    }
}
