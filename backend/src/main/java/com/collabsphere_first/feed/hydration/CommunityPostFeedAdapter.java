package com.collabsphere_first.feed.hydration;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.repository.PostRepository;
import com.collabsphere_first.community.service.CommunityFeedReadService;
import com.collabsphere_first.feed.dto.CommunitySummaryDto;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityPostFeedAdapter implements FeedContentAdapter {

    private final CommunityFeedReadService communityFeedReadService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NetworkAccessService networkAccessService;

    @Override
    public ContentType supportedType() {
        return ContentType.COMMUNITY_POST;
    }

    @Override
    public Map<UUID, HydratedFeedItem> batchHydrate(Collection<UUID> ids, User viewer) {
        if (ids == null || ids.isEmpty() || viewer == null) {
            return Collections.emptyMap();
        }

        // 1. Authoritative community access / eligibility validation
        Map<UUID, PostDto> eligiblePostDtos = communityFeedReadService.batchGetEligiblePosts(ids, viewer);
        if (eligiblePostDtos.isEmpty()) {
            return Collections.emptyMap();
        }

        // 2. Fetch post entities with community to get community name
        List<Post> posts = postRepository.findByIdsWithCommunity(eligiblePostDtos.keySet());
        Map<UUID, Post> postEntityMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));

        // 3. Batch fetch authors
        Set<Long> authorIds = eligiblePostDtos.values().stream()
                .map(PostDto::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Long viewerId = viewer.getId();
        Set<Long> nonViewerAuthorIds = authorIds.stream()
                .filter(id -> !viewerId.equals(id))
                .collect(Collectors.toSet());

        Set<Long> blockedAuthorIds = Collections.emptySet();
        Set<Long> mutedAuthorIds = Collections.emptySet();

        if (!nonViewerAuthorIds.isEmpty()) {
            blockedAuthorIds = networkAccessService.findBlockedOrBlockingUserIdsAmong(viewerId, nonViewerAuthorIds);
            if (blockedAuthorIds == null) {
                blockedAuthorIds = nonViewerAuthorIds.stream()
                        .filter(authorId -> networkAccessService.isBlockedEitherDirection(viewerId, authorId))
                        .collect(Collectors.toSet());
            }

            mutedAuthorIds = networkAccessService.findMutedUserIdsAmong(viewerId, nonViewerAuthorIds);
            if (mutedAuthorIds == null) {
                mutedAuthorIds = nonViewerAuthorIds.stream()
                        .filter(authorId -> networkAccessService.isMuted(viewerId, authorId))
                        .collect(Collectors.toSet());
            }
        }

        Map<UUID, HydratedFeedItem> result = new HashMap<>();

        for (Map.Entry<UUID, PostDto> entry : eligiblePostDtos.entrySet()) {
            UUID postId = entry.getKey();
            PostDto postDto = entry.getValue();
            Long authorId = postDto.getAuthorId();
            boolean isAuthor = viewerId.equals(authorId);

            // Re-check block rules
            if (!isAuthor && authorId != null && blockedAuthorIds.contains(authorId)) {
                log.debug("Skipping community post {} due to block between viewer {} and author {}", postId, viewerId, authorId);
                continue;
            }

            // Re-check mute rules
            if (!isAuthor && authorId != null && mutedAuthorIds.contains(authorId)) {
                log.debug("Skipping community post {} due to mute from viewer {} on author {}", postId, viewerId, authorId);
                continue;
            }

            Post postEntity = postEntityMap.get(postId);
            CommunitySummaryDto communitySummary = null;
            if (postEntity != null && postEntity.getCommunity() != null) {
                communitySummary = new CommunitySummaryDto(
                        postEntity.getCommunity().getId(),
                        postEntity.getCommunity().getName(),
                        postEntity.getCommunity().getSlug()
                );
            }

            AuthorSummaryResponse authorSummary = null;
            if (authorId != null) {
                User authorUser = authorMap.get(authorId);
                if (authorUser != null) {
                    authorSummary = AuthorSummaryResponse.builder()
                            .id(authorUser.getId())
                            .firstName(authorUser.getFirstName())
                            .lastName(authorUser.getLastName())
                            .username(authorUser.getUsername())
                            .profilePicture(authorUser.getProfilePicture())
                            .build();
                } else {
                    authorSummary = AuthorSummaryResponse.builder()
                            .id(authorId)
                            .username("user-" + authorId)
                            .build();
                }
            }

            HydratedFeedItem item = new HydratedFeedItem(
                    ContentRef.communityPost(postId),
                    ContentType.COMMUNITY_POST,
                    authorSummary,
                    postDto.getTitle(),
                    postDto.getBody(),
                    Collections.emptyList(),
                    communitySummary,
                    postDto.getCommentCount(),
                    postDto.getCreatedAt(),
                    postDto.getUpdatedAt()
            );
            result.put(postId, item);
        }

        return result;
    }
}
