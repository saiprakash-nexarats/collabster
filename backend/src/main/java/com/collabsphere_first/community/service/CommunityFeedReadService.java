package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CommunityFeedCandidate;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.user.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Narrow Community feed read abstraction.
 * Encapsulates Community membership, visibility, and moderation rules so Feed does not leak them.
 */

public interface CommunityFeedReadService {

    List<CommunityFeedCandidate> findRecentEligiblePosts(
            User currentUser,
            int limit
    );

    Map<UUID, PostDto> batchGetEligiblePosts(
            Collection<UUID> postIds,
            User currentUser
    );
}
