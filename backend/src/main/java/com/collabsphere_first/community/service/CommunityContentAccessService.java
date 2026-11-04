package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.feed.dto.CommunitySummaryDto;
import com.collabsphere_first.interaction.dto.InteractionSummary;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared, read-only facade for authoritative Community post access,
 * visibility rules, metadata and interaction summaries.
 * Provides a single source of truth across Community direct retrieval,
 * Feed candidate hydration, Explore search/discovery, and Notifications.
 */
public interface CommunityContentAccessService {

    /**
     * Evaluates whether the specified user has permission to view the community post.
     * Enforces active community state, public/private visibility, post moderation status,
     * and moderator/author overrides.
     */
    boolean canViewPost(Long userId, UUID postId);

    /**
     * Retrieves the post DTO if accessible by userId, otherwise returns Optional.empty().
     */
    Optional<PostDto> getPostIfAccessible(UUID postId, Long userId);

    /**
     * Batch retrieves accessible posts for userId, avoiding N+1 queries.
     */
    Map<UUID, PostDto> getVisiblePosts(Collection<UUID> postIds, Long userId);

    /**
     * Retrieves community summary metadata (id, name, slug) for navigation.
     */
    Optional<CommunitySummaryDto> getCommunitySummary(UUID communityId);

    /**
     * Retrieves authoritative interaction summary (liked, saved, likeCount)
     * for a community post.
     */
    InteractionSummary getAuthoritativeReactionSummary(UUID postId, Long userId);
}
