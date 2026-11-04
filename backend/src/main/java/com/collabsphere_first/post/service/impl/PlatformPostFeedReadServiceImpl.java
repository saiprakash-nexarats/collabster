package com.collabsphere_first.post.service.impl;

import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.mapper.PlatformPostMapper;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.post.service.PlatformPostFeedReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformPostFeedReadServiceImpl implements PlatformPostFeedReadService {

    private final PlatformPostRepository platformPostRepository;
    private final PlatformPostMapper platformPostMapper;

    @Override
    public List<PlatformPost> findCandidatePostsByAuthors(
            Collection<Long> authorIds,
            Collection<PostVisibility> visibilities,
            int limit
    ) {
        if (authorIds == null || authorIds.isEmpty() || visibilities == null || visibilities.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        int boundedLimit = Math.min(limit, 500);
        return platformPostRepository.findCandidatePostsByAuthors(
                authorIds,
                PostStatus.ACTIVE,
                visibilities,
                PageRequest.of(0, boundedLimit)
        );
    }

    @Override
    public List<PlatformPost> findCandidatePublicPosts(
            Collection<Long> excludedAuthorIds,
            int limit
    ) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        int boundedLimit = Math.min(limit, 500);
        if (excludedAuthorIds == null || excludedAuthorIds.isEmpty()) {
            return platformPostRepository.findPublicPosts(
                    PostStatus.ACTIVE,
                    PostVisibility.PUBLIC,
                    PageRequest.of(0, boundedLimit)
            );
        } else {
            return platformPostRepository.findCandidatePublicPostsExcludingAuthors(
                    excludedAuthorIds,
                    PageRequest.of(0, boundedLimit)
            );
        }
    }

    @Override
    public Map<UUID, PlatformPostResponse> batchGetActivePosts(Collection<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PlatformPost> posts = platformPostRepository.findByIdsWithMedia(postIds);
        Map<UUID, PlatformPostResponse> result = new HashMap<>();
        for (PlatformPost post : posts) {
            if (post.getStatus() == PostStatus.ACTIVE) {
                result.put(post.getId(), platformPostMapper.toResponse(post));
            }
        }
        return result;
    }
}
