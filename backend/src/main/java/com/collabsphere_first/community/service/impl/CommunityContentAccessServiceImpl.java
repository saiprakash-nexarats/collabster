package com.collabsphere_first.community.service.impl;

import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.mapper.PostMapper;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.repository.PostRepository;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.community.service.CommunityContentAccessService;
import com.collabsphere_first.feed.dto.CommunitySummaryDto;
import com.collabsphere_first.interaction.dto.InteractionSummary;
import com.collabsphere_first.interaction.repository.ContentLikeRepository;
import com.collabsphere_first.interaction.repository.ContentSaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityContentAccessServiceImpl implements CommunityContentAccessService {

    private final PostRepository postRepository;
    private final MembershipRepository membershipRepository;
    private final CommunityRepository communityRepository;
    private final CommunityRoleService roleService;
    private final PostMapper postMapper;
    private final ContentLikeRepository contentLikeRepository;
    private final ContentSaveRepository contentSaveRepository;

    @Override
    public boolean canViewPost(Long userId, UUID postId) {
        if (postId == null) {
            return false;
        }

        Optional<Post> postOpt = postRepository.findByIdWithCommunity(postId);
        if (postOpt.isEmpty()) {
            return false;
        }

        Post post = postOpt.get();
        Community community = post.getCommunity();
        if (community == null) {
            return false;
        }

        // Platform admin override
        if (userId != null && roleService != null && roleService.isPlatformAdmin(userId)) {
            return true;
        }

        // Author can view their own post unless community is strictly gone
        if (userId != null && userId.equals(post.getAuthorId())) {
            return true;
        }

        // Community must be active and not deleted
        if (community.getCommunityStatus() != CommunityStatus.ACTIVE) {
            return false;
        }

        // Check moderation status: non-visible posts require moderator rank
        boolean isModerator = false;
        Membership membership = null;
        if (userId != null) {
            membership = membershipRepository.findByCommunityIdAndUserId(community.getId(), userId)
                    .filter(m -> m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.MUTED)
                    .orElse(null);
            if (membership != null && membership.getRole() != null && membership.getRole().hasAtLeastRank(MembershipRole.MODERATOR)) {
                isModerator = true;
            }
        }

        if (post.getModerationStatus() != ModerationStatus.VISIBLE && !isModerator) {
            return false;
        }

        // Public community: anyone can read visible posts
        if (community.getVisibility() == Visibility.PUBLIC) {
            return true;
        }

        // Private community: active membership required
        return membership != null;
    }

    @Override
    public Optional<PostDto> getPostIfAccessible(UUID postId, Long userId) {
        if (postId == null) {
            return Optional.empty();
        }

        Optional<Post> postOpt = postRepository.findByIdWithCommunity(postId);
        if (postOpt.isEmpty()) {
            return Optional.empty();
        }

        Post post = postOpt.get();
        if (!canViewPost(userId, postId)) {
            return Optional.empty();
        }

        PostDto dto = postMapper.toDto(post);
        enrichInteractionState(dto, userId);
        return Optional.of(dto);
    }

    @Override
    public Map<UUID, PostDto> getVisiblePosts(Collection<UUID> postIds, Long userId) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Post> posts = postRepository.findByIdsWithCommunity(postIds);
        if (posts.isEmpty()) {
            return Collections.emptyMap();
        }

        boolean isPlatformAdmin = userId != null && roleService != null && roleService.isPlatformAdmin(userId);

        Set<UUID> communityIds = posts.stream()
                .map(p -> p.getCommunity() != null ? p.getCommunity().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Membership> membershipByCommunityId = new HashMap<>();
        if (userId != null && !communityIds.isEmpty()) {
            List<Membership> memberships = membershipRepository.findByUserIdAndCommunityIdIn(userId, communityIds);
            for (Membership m : memberships) {
                if (m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.MUTED) {
                    membershipByCommunityId.put(m.getCommunity().getId(), m);
                }
            }
        }

        Map<UUID, PostDto> result = new HashMap<>();
        List<UUID> visiblePostIds = new ArrayList<>();

        for (Post post : posts) {
            Community community = post.getCommunity();
            if (community == null) {
                continue;
            }

            boolean isAuthor = userId != null && userId.equals(post.getAuthorId());
            if (isPlatformAdmin || isAuthor) {
                PostDto dto = postMapper.toDto(post);
                result.put(post.getId(), dto);
                visiblePostIds.add(post.getId());
                continue;
            }

            if (community.getCommunityStatus() != CommunityStatus.ACTIVE) {
                continue;
            }

            Membership membership = membershipByCommunityId.get(community.getId());
            boolean isModerator = membership != null && membership.getRole() != null && membership.getRole().hasAtLeastRank(MembershipRole.MODERATOR);

            if (post.getModerationStatus() != ModerationStatus.VISIBLE && !isModerator) {
                continue;
            }

            if (community.getVisibility() == Visibility.PUBLIC || membership != null) {
                PostDto dto = postMapper.toDto(post);
                result.put(post.getId(), dto);
                visiblePostIds.add(post.getId());
            }
        }

        if (!visiblePostIds.isEmpty()) {
            batchEnrichInteractionStates(result, visiblePostIds, userId);
        }

        return result;
    }

    @Override
    public Optional<CommunitySummaryDto> getCommunitySummary(UUID communityId) {
        if (communityId == null) {
            return Optional.empty();
        }
        return communityRepository.findById(communityId)
                .map(c -> new CommunitySummaryDto(c.getId(), c.getName(), c.getSlug()));
    }

    @Override
    public InteractionSummary getAuthoritativeReactionSummary(UUID postId, Long userId) {
        if (postId == null) {
            return new InteractionSummary(false, false, 0L);
        }

        long likeCount = contentLikeRepository.countByContentTypeAndContentId(ContentType.COMMUNITY_POST, postId);
        boolean liked = userId != null && contentLikeRepository.existsByUserIdAndContentTypeAndContentId(userId, ContentType.COMMUNITY_POST, postId);
        boolean saved = userId != null && contentSaveRepository.existsByUserIdAndContentTypeAndContentId(userId, ContentType.COMMUNITY_POST, postId);

        return new InteractionSummary(liked, saved, likeCount);
    }

    private void enrichInteractionState(PostDto dto, Long userId) {
        if (dto == null || dto.getId() == null) {
            return;
        }
        long likeCount = contentLikeRepository.countByContentTypeAndContentId(ContentType.COMMUNITY_POST, dto.getId());
        dto.setLikeCount(likeCount);

        if (userId != null) {
            dto.setLiked(contentLikeRepository.existsByUserIdAndContentTypeAndContentId(userId, ContentType.COMMUNITY_POST, dto.getId()));
            dto.setSaved(contentSaveRepository.existsByUserIdAndContentTypeAndContentId(userId, ContentType.COMMUNITY_POST, dto.getId()));
        } else {
            dto.setLiked(false);
            dto.setSaved(false);
        }
    }

    private void batchEnrichInteractionStates(Map<UUID, PostDto> dtos, List<UUID> postIds, Long userId) {
        List<Object[]> countRows = contentLikeRepository.countLikesByContentTypeAndIds(ContentType.COMMUNITY_POST, postIds);
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : countRows) {
            UUID id = (UUID) row[1];
            Long count = (Long) row[2];
            counts.put(id, count != null ? count : 0L);
        }

        Set<UUID> likedIds = new HashSet<>();
        Set<UUID> savedIds = new HashSet<>();
        if (userId != null) {
            List<Object[]> likedRows = contentLikeRepository.findLikedContent(userId, ContentType.COMMUNITY_POST, postIds);
            for (Object[] row : likedRows) {
                likedIds.add((UUID) row[1]);
            }
            List<Object[]> savedRows = contentSaveRepository.findSavedContent(userId, ContentType.COMMUNITY_POST, postIds);
            for (Object[] row : savedRows) {
                savedIds.add((UUID) row[1]);
            }
        }

        for (Map.Entry<UUID, PostDto> entry : dtos.entrySet()) {
            PostDto dto = entry.getValue();
            dto.setLikeCount(counts.getOrDefault(entry.getKey(), 0L));
            dto.setLiked(likedIds.contains(entry.getKey()));
            dto.setSaved(savedIds.contains(entry.getKey()));
        }
    }
}
