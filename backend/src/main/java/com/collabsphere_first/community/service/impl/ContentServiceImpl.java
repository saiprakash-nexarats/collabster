package com.collabsphere_first.community.service.impl;

import com.collabsphere_first.community.service.AnalyticsService;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import com.collabsphere_first.community.dto.CommentDto;
import com.collabsphere_first.community.dto.CreateCommentRequest;
import com.collabsphere_first.community.dto.CreatePostRequest;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.event.PostCreatedEvent;
import com.collabsphere_first.community.exception.*;
import com.collabsphere_first.community.mapper.CommentMapper;
import com.collabsphere_first.community.mapper.PostMapper;
import com.collabsphere_first.community.entity.Comment;
import com.collabsphere_first.community.enums.PostType;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.repository.CommentRepository;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.repository.PostRepository;
import com.collabsphere_first.community.service.ContentService;
import com.collabsphere_first.community.service.CommunitySettingsService;
import com.collabsphere_first.community.service.ModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final ModerationService moderationService;
    private final CommunitiesRateLimitConfig rateLimitConfig;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalyticsService analyticsService;
    private final CommunityRoleService roleService;
    private final CommunitySettingsService settingsService;
    private final com.collabsphere_first.community.service.CommunityAuditLogService auditLogService;
    private final com.collabsphere_first.community.service.CommunityEventOutboxService outboxService;

    @Value("${collabster.communities.fan-out-threshold:1000}")
    private int fanOutThreshold;

    @Override
    @Transactional
    public PostDto createPost(UUID communityId, CreatePostRequest request, Long authorId) {
        var limit = rateLimitConfig.consumePost(authorId);
        if (!limit.allowed()) {
            throw new CommunityRateLimitException("Rate limit exceeded for creating posts. Please wait before posting again.", limit.retryAfterSeconds());
        }

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));
        if (community.getCommunityStatus() != com.collabsphere_first.community.enums.CommunityStatus.ACTIVE) {
            throw new InsufficientRoleException("Community is not accepting new posts");
        }

        Membership membership = validateActiveMember(communityId, authorId);
        if (!settingsService.getOrDefault(communityId).isMemberPostsEnabled()
                && !membership.getRole().hasAtLeastRank(MembershipRole.MODERATOR)) {
            throw new InsufficientRoleException("Community settings restrict post creation to moderators");
        }

        Post post = postMapper.toEntity(request);

post.setCommunity(community);
post.setAuthorId(authorId);

if (request.getType() == null) {
    post.setType(PostType.DISCUSSION);
} else {
    post.setType(request.getType());
}

post.setModerationStatus(
        community.isRequiresModeration()
                ? ModerationStatus.PENDING_REVIEW
                : ModerationStatus.VISIBLE
);
        Post savedPost = postRepository.save(post);
        outboxService.record(communityId, "POST_CREATED", savedPost.getId().toString(),
                Map.of("authorId", authorId, "postId", savedPost.getId()));

        if (community.getMemberCount() < fanOutThreshold) {
            log.debug("Community {} member count ({}) < threshold ({}): Fan-out on WRITE",
                    communityId, community.getMemberCount(), fanOutThreshold);
        } else {
            log.debug("Community {} member count ({}) >= threshold ({}): Fan-out on READ",
                    communityId, community.getMemberCount(), fanOutThreshold);
        }

        eventPublisher.publishEvent(new PostCreatedEvent(this, savedPost.getId(), communityId, authorId, savedPost.getTitle() + " " + savedPost.getBody()));
        if (community.isRequiresModeration() && settingsService.getOrDefault(communityId).isAiModerationEnabled()) {
            moderationService.triggerAsyncPostModeration(savedPost.getId(), savedPost.getTitle() + " " + savedPost.getBody());
        }

        if (analyticsService != null) {
            analyticsService.trackEvent("post_created", Map.of(
                    "post_id", savedPost.getId().toString(),
                    "community_id", communityId.toString(),
                    "author_id", authorId.toString()
            ));
        }

        return postMapper.toDto(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDto> getCommunityFeed(UUID communityId, Long requestingUserId, Pageable pageable) {
        Community community = communityRepository.findById(communityId).orElseThrow(() -> new CommunityNotFoundException(communityId));
        assertCanRead(community, requestingUserId);
        return postRepository.findVisibleOrAuthorPosts(communityId, ModerationStatus.VISIBLE, requestingUserId, pageable)
                .map(postMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto getPost(UUID postId, Long requestingUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        assertCanRead(post.getCommunity(), requestingUserId);
        if (post.getModerationStatus() != ModerationStatus.VISIBLE && !post.getAuthorId().equals(requestingUserId)) {
            validateRoleRank(post.getCommunity().getId(), requestingUserId, MembershipRole.MODERATOR, "Post is unavailable");
        }
        return postMapper.toDto(post);
    }

    @Override
    @Transactional
    public PostDto pinPost(UUID postId, boolean pin, Long requestingUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        validateRoleRank(post.getCommunity().getId(), requestingUserId, MembershipRole.ADMIN, "Only Admin or Owner can pin/unpin posts");

        post.setPinned(pin);
        Post updated = postRepository.save(post);
        auditLogService.record(post.getCommunity(), requestingUserId, pin ? "POST_PINNED" : "POST_UNPINNED", "POST", postId.toString(), null);
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.PostLifecycleEvent(
                this, post.getId(), post.getCommunity().getId(), post.getAuthorId(), requestingUserId,
                pin ? com.collabsphere_first.community.event.PostLifecycleEvent.Action.PINNED : com.collabsphere_first.community.event.PostLifecycleEvent.Action.UNPINNED,
                post.getTitle()));
        outboxService.record(post.getCommunity().getId(), pin ? "POST_PINNED" : "POST_UNPINNED", post.getId().toString(),
                Map.of("postId", post.getId(), "actorId", requestingUserId));
        return postMapper.toDto(updated);
    }

    @Override
    @Transactional
    public PostDto lockPost(UUID postId, boolean lock, Long requestingUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        validateRoleRank(post.getCommunity().getId(), requestingUserId, MembershipRole.MODERATOR, "Only Moderator or higher can lock/unlock posts");

        post.setLocked(lock);
        Post updated = postRepository.save(post);
        auditLogService.record(post.getCommunity(), requestingUserId, lock ? "POST_LOCKED" : "POST_UNLOCKED", "POST", postId.toString(), null);
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.PostLifecycleEvent(
                this, post.getId(), post.getCommunity().getId(), post.getAuthorId(), requestingUserId,
                lock ? com.collabsphere_first.community.event.PostLifecycleEvent.Action.LOCKED : com.collabsphere_first.community.event.PostLifecycleEvent.Action.UNLOCKED,
                post.getTitle()));
        outboxService.record(post.getCommunity().getId(), lock ? "POST_LOCKED" : "POST_UNLOCKED", post.getId().toString(),
                Map.of("postId", post.getId(), "actorId", requestingUserId));
        return postMapper.toDto(updated);
    }

    @Override
    @Transactional
    public CommentDto createComment(UUID postId, CreateCommentRequest request, Long authorId) {
        var limit = rateLimitConfig.consumeComment(authorId);
        if (!limit.allowed()) {
            throw new CommunityRateLimitException("Rate limit exceeded for comments. Please wait before commenting again.", limit.retryAfterSeconds());
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (post.isLocked()) {
            throw new ModerationException("This post is locked for comments");
        }

        validateActiveMember(post.getCommunity().getId(), authorId);
        if (post.getCommunity().getCommunityStatus() != com.collabsphere_first.community.enums.CommunityStatus.ACTIVE) {
            throw new InsufficientRoleException("Community is not accepting comments");
        }
        if (!settingsService.getOrDefault(post.getCommunity().getId()).isCommentsEnabled()) {
            throw new ModerationException("Comments are disabled for this community");
        }

        Comment comment = commentMapper.toEntity(request);
        Long parentAuthorId = null;
        if (request.getParentCommentId() != null) {
            Comment parent = commentRepository.findByIdAndPostId(request.getParentCommentId(), postId)
                    .orElseThrow(() -> new ModerationException("Parent comment must belong to the same post"));
            comment.setParentCommentId(parent.getId());
            parentAuthorId = parent.getAuthorId();
        }
        comment.setPost(post);
        comment.setCommunityId(post.getCommunity().getId());
        comment.setAuthorId(authorId);
        boolean reqMod = post.getCommunity().isRequiresModeration();
        comment.setModerationStatus(reqMod ? ModerationStatus.PENDING_REVIEW : ModerationStatus.VISIBLE);

        Comment savedComment = commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        eventPublisher.publishEvent(new com.collabsphere_first.community.event.CommentCreatedEvent(
                this, savedComment.getId(), postId, post.getCommunity().getId(), authorId,
                savedComment.getParentCommentId(), parentAuthorId, post.getAuthorId(), savedComment.getBody()));
        outboxService.record(post.getCommunity().getId(), "COMMENT_CREATED", savedComment.getId().toString(),
                Map.of("commentId", savedComment.getId(), "postId", postId, "authorId", authorId));

        if (reqMod && settingsService.getOrDefault(post.getCommunity().getId()).isAiModerationEnabled()) {
            moderationService.triggerAsyncCommentModeration(savedComment.getId(), savedComment.getBody());
        }

        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDto> getPostComments(UUID postId, Long requestingUserId, Pageable pageable) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
        assertCanRead(post.getCommunity(), requestingUserId);
        return commentRepository.findVisibleOrAuthorComments(postId, ModerationStatus.VISIBLE, requestingUserId, pageable)
                .map(commentMapper::toDto);
    }

    private Membership validateActiveMember(UUID communityId, Long userId) {
        if (roleService != null && roleService.isPlatformAdmin(userId)) {
            return Membership.builder().role(MembershipRole.OWNER).status(MembershipStatus.ACTIVE).build();
        }
        Membership membership = membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new InsufficientRoleException("Must be a member of the community to post/comment"));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new InsufficientRoleException("Member status is not ACTIVE (status: " + membership.getStatus() + ")");
        }
        if (!membership.getRole().hasAtLeastRank(MembershipRole.MEMBER)) {
            throw new InsufficientRoleException("Followers have read-only access and cannot post/comment");
        }
        return membership;
    }

    private void assertCanRead(Community community, Long userId) {
        if (roleService != null && roleService.isPlatformAdmin(userId)) return;
        if ((community.getVisibility() != com.collabsphere_first.community.enums.Visibility.PUBLIC
                || community.getCommunityStatus() != com.collabsphere_first.community.enums.CommunityStatus.ACTIVE)
                && membershipRepository.findByCommunityIdAndUserId(community.getId(), userId)
                    .filter(m -> m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.MUTED).isEmpty()) {
            throw new InsufficientRoleException("Community membership is required to read this content");
        }
    }

    private void validateRoleRank(UUID communityId, Long userId, MembershipRole requiredRole, String errorMsg) {
        if (roleService != null && roleService.isPlatformAdmin(userId)) {
            return;
        }
        Membership membership = membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new InsufficientRoleException(errorMsg));
        if (membership.getStatus() != MembershipStatus.ACTIVE || !membership.getRole().hasAtLeastRank(requiredRole)) {
            throw new InsufficientRoleException(errorMsg);
        }
    }
}
