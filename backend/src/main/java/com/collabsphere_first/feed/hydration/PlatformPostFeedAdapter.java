package com.collabsphere_first.feed.hydration;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.service.PlatformPostFeedReadService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformPostFeedAdapter implements FeedContentAdapter {

    private final PlatformPostFeedReadService platformPostFeedReadService;
    private final NetworkAccessService networkAccessService;

    @Override
    public ContentType supportedType() {
        return ContentType.PLATFORM_POST;
    }

    @Override
    public Map<UUID, HydratedFeedItem> batchHydrate(Collection<UUID> ids, User viewer) {
        if (ids == null || ids.isEmpty() || viewer == null) {
            return Collections.emptyMap();
        }

        Map<UUID, PlatformPostResponse> postMap = platformPostFeedReadService.batchGetActivePosts(ids);
        Long viewerId = viewer.getId();
        Map<UUID, HydratedFeedItem> result = new HashMap<>();

        Set<Long> candidateAuthorIds = postMap.values().stream()
                .filter(p -> p != null && p.getAuthor() != null && !viewerId.equals(p.getAuthor().getId()))
                .map(p -> p.getAuthor().getId())
                .collect(Collectors.toSet());

        Set<Long> blockedAuthorIds = Collections.emptySet();
        Set<Long> mutedAuthorIds = Collections.emptySet();
        Set<Long> followingAuthorIds = Collections.emptySet();

        if (!candidateAuthorIds.isEmpty()) {
            blockedAuthorIds = networkAccessService.findBlockedOrBlockingUserIdsAmong(viewerId, candidateAuthorIds);
            if (blockedAuthorIds == null) {
                blockedAuthorIds = candidateAuthorIds.stream()
                        .filter(authorId -> networkAccessService.isBlockedEitherDirection(viewerId, authorId))
                        .collect(Collectors.toSet());
            }

            mutedAuthorIds = networkAccessService.findMutedUserIdsAmong(viewerId, candidateAuthorIds);
            if (mutedAuthorIds == null) {
                mutedAuthorIds = candidateAuthorIds.stream()
                        .filter(authorId -> networkAccessService.isMuted(viewerId, authorId))
                        .collect(Collectors.toSet());
            }

            followingAuthorIds = networkAccessService.findFollowingUserIdsAmong(viewerId, candidateAuthorIds);
            if (followingAuthorIds == null) {
                followingAuthorIds = candidateAuthorIds.stream()
                        .filter(authorId -> networkAccessService.isFollowing(viewerId, authorId))
                        .collect(Collectors.toSet());
            }
        }

        for (Map.Entry<UUID, PlatformPostResponse> entry : postMap.entrySet()) {
            PlatformPostResponse post = entry.getValue();
            if (post == null || post.getAuthor() == null) {
                continue;
            }

            Long authorId = post.getAuthor().getId();
            boolean isAuthor = viewerId.equals(authorId);

            // Re-check block rules: if either viewer blocked author or author blocked viewer, skip!
            if (!isAuthor && blockedAuthorIds.contains(authorId)) {
                log.debug("Skipping platform post {} due to block between viewer {} and author {}", post.getId(), viewerId, authorId);
                continue;
            }

            // Re-check mute rules: if viewer muted author, skip!
            if (!isAuthor && mutedAuthorIds.contains(authorId)) {
                log.debug("Skipping platform post {} due to mute from viewer {} on author {}", post.getId(), viewerId, authorId);
                continue;
            }

            // Check visibility rules
            if (post.getVisibility() == PostVisibility.PRIVATE) {
                // Home feed excludes PRIVATE posts per architecture contract
                continue;
            } else if (post.getVisibility() == PostVisibility.FOLLOWERS_ONLY) {
                if (!isAuthor && !followingAuthorIds.contains(authorId)) {
                    log.debug("Skipping FOLLOWERS_ONLY platform post {} because viewer {} does not follow author {}", post.getId(), viewerId, authorId);
                    continue;
                }
            }

            HydratedFeedItem item = new HydratedFeedItem(
                    ContentRef.platformPost(post.getId()),
                    ContentType.PLATFORM_POST,
                    post.getAuthor(),
                    null, // No title for platform post
                    post.getBody(),
                    post.getMedia() != null ? post.getMedia() : Collections.emptyList(),
                    null, // No community
                    0,
                    post.getCreatedAt(),
                    post.getUpdatedAt()
            );
            result.put(post.getId(), item);
        }

        return result;
    }
}
