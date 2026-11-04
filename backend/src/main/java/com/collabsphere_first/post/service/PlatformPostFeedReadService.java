package com.collabsphere_first.post.service;

import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostVisibility;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Decoupled read SPI for Feed candidate collection and batch hydration of Platform Posts.
 * Prevents Feed domain from depending directly on PlatformPostRepository.
 */
public interface PlatformPostFeedReadService {

    List<PlatformPost> findCandidatePostsByAuthors(
            Collection<Long> authorIds,
            Collection<PostVisibility> visibilities,
            int limit
    );

    List<PlatformPost> findCandidatePublicPosts(
            Collection<Long> excludedAuthorIds,
            int limit
    );

    Map<UUID, PlatformPostResponse> batchGetActivePosts(Collection<UUID> postIds);
}
