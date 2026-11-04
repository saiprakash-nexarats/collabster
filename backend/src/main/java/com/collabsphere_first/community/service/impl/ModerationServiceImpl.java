package com.collabsphere_first.community.service.impl;

import com.collabsphere_first.community.service.AnalyticsService;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.integration.ai.AiModerationService;
import com.collabsphere_first.community.dto.CreateReportRequest;
import com.collabsphere_first.community.dto.ReportDto;
import com.collabsphere_first.community.dto.ResolveReportRequest;
import com.collabsphere_first.community.event.ReportFiledEvent;
import com.collabsphere_first.community.exception.*;
import com.collabsphere_first.community.mapper.ReportMapper;
import com.collabsphere_first.community.entity.*;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.enums.ReportStatus;
import com.collabsphere_first.community.repository.*;
import com.collabsphere_first.community.service.ModerationService;
import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import com.collabsphere_first.community.exception.CommunityRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.mapper.PostMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationServiceImpl implements ModerationService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ResourceRepository resourceRepository;
    private final ReportRepository reportRepository;
    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final AiModerationService aiModerationService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalyticsService analyticsService;
    private final CommunityRoleService roleService;
    private final com.collabsphere_first.community.service.CommunityAuditLogService auditLogService;
    private final com.collabsphere_first.community.service.CommunityEventOutboxService outboxService;
    private final CommunitiesRateLimitConfig rateLimitConfig;

    @Override
    @Async
    @Transactional
    public void triggerAsyncPostModeration(UUID postId, String contentText) {
        log.info("Starting async AI moderation for Post ID: {}", postId);
        ModerationStatus targetStatus = ModerationStatus.FLAGGED;

        try {
            if (aiModerationService != null) {
                AiModerationService.ModerationResult result = aiModerationService.classifyContentAsync(contentText)
                        .get(2, TimeUnit.SECONDS);

                if (result == AiModerationService.ModerationResult.PASSED) {
                    targetStatus = ModerationStatus.VISIBLE;
                } else if (result == AiModerationService.ModerationResult.VIOLATION) {
                    targetStatus = ModerationStatus.REMOVED;
                } else {
                    targetStatus = ModerationStatus.FLAGGED;
                }
            } else {
                targetStatus = ModerationStatus.VISIBLE;
            }
        } catch (Exception e) {
            log.error("AI Moderation call failed or timed out for post {}. Failing closed to FLAGGED: {}", postId, e.getMessage());
            targetStatus = ModerationStatus.FLAGGED;
        }

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setModerationStatus(targetStatus);
            postRepository.save(post);
            log.info("Post ID: {} moderation status updated to: {}", postId, targetStatus);
            com.collabsphere_first.community.event.ContentModerationEvent.Action action =
                    targetStatus == ModerationStatus.VISIBLE ? com.collabsphere_first.community.event.ContentModerationEvent.Action.POST_APPROVED :
                    (targetStatus == ModerationStatus.REMOVED ? com.collabsphere_first.community.event.ContentModerationEvent.Action.POST_REJECTED :
                    com.collabsphere_first.community.event.ContentModerationEvent.Action.POST_FLAGGED);
            eventPublisher.publishEvent(new com.collabsphere_first.community.event.ContentModerationEvent(
                    this, post.getCommunity().getId(), "POST", post.getId().toString(), post.getAuthorId(), null,
                    action, "AI Moderation result: " + targetStatus));
            outboxService.record(post.getCommunity().getId(), "POST_MODERATION_RESULT", post.getId().toString(),
                    Map.of("postId", post.getId(), "status", targetStatus.name()));
        }
    }

    @Override
    @Async
    @Transactional
    public void triggerAsyncCommentModeration(UUID commentId, String contentText) {
        log.info("Starting async AI moderation for Comment ID: {}", commentId);
        ModerationStatus targetStatus = ModerationStatus.FLAGGED;

        try {
            if (aiModerationService != null) {
                AiModerationService.ModerationResult result = aiModerationService.classifyContentAsync(contentText)
                        .get(2, TimeUnit.SECONDS);

                if (result == AiModerationService.ModerationResult.PASSED) {
                    targetStatus = ModerationStatus.VISIBLE;
                } else if (result == AiModerationService.ModerationResult.VIOLATION) {
                    targetStatus = ModerationStatus.REMOVED;
                } else {
                    targetStatus = ModerationStatus.FLAGGED;
                }
            } else {
                targetStatus = ModerationStatus.VISIBLE;
            }
        } catch (Exception e) {
            log.error("AI Moderation call failed or timed out for comment {}. Failing closed to FLAGGED: {}", commentId, e.getMessage());
            targetStatus = ModerationStatus.FLAGGED;
        }

        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            comment.setModerationStatus(targetStatus);
            commentRepository.save(comment);
            com.collabsphere_first.community.event.ContentModerationEvent.Action action =
                    targetStatus == ModerationStatus.VISIBLE ? com.collabsphere_first.community.event.ContentModerationEvent.Action.COMMENT_APPROVED :
                    (targetStatus == ModerationStatus.REMOVED ? com.collabsphere_first.community.event.ContentModerationEvent.Action.COMMENT_REJECTED :
                    com.collabsphere_first.community.event.ContentModerationEvent.Action.COMMENT_FLAGGED);
            eventPublisher.publishEvent(new com.collabsphere_first.community.event.ContentModerationEvent(
                    this, comment.getCommunityId(), "COMMENT", comment.getId().toString(), comment.getAuthorId(), null,
                    action, "AI Moderation result: " + targetStatus));
            outboxService.record(comment.getCommunityId(), "COMMENT_MODERATION_RESULT", comment.getId().toString(),
                    Map.of("commentId", comment.getId(), "status", targetStatus.name()));
        }
    }

    @Override
    @Async
    @Transactional
    public void triggerAsyncResourceModeration(UUID resourceId, String contentText) {
        log.info("Starting async AI moderation for Resource ID: {}", resourceId);
        ModerationStatus targetStatus = ModerationStatus.FLAGGED;

        try {
            if (aiModerationService != null) {
                AiModerationService.ModerationResult result = aiModerationService.classifyContentAsync(contentText)
                        .get(2, TimeUnit.SECONDS);

                if (result == AiModerationService.ModerationResult.PASSED) {
                    targetStatus = ModerationStatus.VISIBLE;
                } else if (result == AiModerationService.ModerationResult.VIOLATION) {
                    targetStatus = ModerationStatus.REMOVED;
                } else {
                    targetStatus = ModerationStatus.FLAGGED;
                }
            } else {
                targetStatus = ModerationStatus.VISIBLE;
            }
        } catch (Exception e) {
            log.error("AI Moderation call failed or timed out for resource {}. Failing closed to FLAGGED: {}", resourceId, e.getMessage());
            targetStatus = ModerationStatus.FLAGGED;
        }

        Optional<Resource> resourceOpt = resourceRepository.findById(resourceId);
        if (resourceOpt.isPresent()) {
            Resource resource = resourceOpt.get();
            resource.setModerationStatus(targetStatus);
            resourceRepository.save(resource);
            com.collabsphere_first.community.event.ContentModerationEvent.Action action =
                    targetStatus == ModerationStatus.VISIBLE ? com.collabsphere_first.community.event.ContentModerationEvent.Action.RESOURCE_APPROVED :
                    (targetStatus == ModerationStatus.REMOVED ? com.collabsphere_first.community.event.ContentModerationEvent.Action.RESOURCE_REJECTED :
                    com.collabsphere_first.community.event.ContentModerationEvent.Action.RESOURCE_FLAGGED);
            eventPublisher.publishEvent(new com.collabsphere_first.community.event.ContentModerationEvent(
                    this, resource.getCommunity().getId(), "RESOURCE", resource.getId().toString(), resource.getUploadedBy(), null,
                    action, "AI Moderation result: " + targetStatus));
            outboxService.record(resource.getCommunity().getId(), "RESOURCE_MODERATION_RESULT", resource.getId().toString(),
                    Map.of("resourceId", resource.getId(), "status", targetStatus.name()));
        }
    }

    @Override
    @Transactional
    public ReportDto fileReport(CreateReportRequest request, Long reporterId) {
        var limit = rateLimitConfig.consumeReport(reporterId);
        if (!limit.allowed()) throw new CommunityRateLimitException("Report rate limit exceeded", limit.retryAfterSeconds());
        Community community = communityRepository.findById(request.getCommunityId())
                .orElseThrow(() -> new CommunityNotFoundException(request.getCommunityId()));

        String normalizedTargetId = normalizeAndValidateTarget(request, community.getId());
        Report report = reportMapper.toEntity(request);
        report.setTargetId(normalizedTargetId);
        report.setCommunity(community);
        report.setReporterId(reporterId);
        report.setStatus(ReportStatus.PENDING);

        Report savedReport = reportRepository.save(report);
        outboxService.record(community.getId(), "REPORT_FILED", savedReport.getId().toString(),
                Map.of("reportId", savedReport.getId(), "reporterId", reporterId, "targetType", request.getTargetType().name()));

        eventPublisher.publishEvent(new ReportFiledEvent(this, savedReport.getId(), community.getId(), request.getTargetType(), normalizedTargetId, reporterId));

        if (analyticsService != null) {
            analyticsService.trackEvent("report_filed", Map.of(
                    "report_id", savedReport.getId().toString(),
                    "community_id", community.getId().toString(),
                    "target_type", request.getTargetType().name()
            ));
        }

        return reportMapper.toDto(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportDto> getModerationQueue(UUID communityId, Long requestingUserId, Pageable pageable) {
        validateModeratorRole(communityId, requestingUserId);
        return reportRepository.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, ReportStatus.PENDING, pageable)
                .map(reportMapper::toDto);
    }

    @Override
    @Transactional
    public ReportDto resolveReport(UUID reportId, ResolveReportRequest request, Long requestingUserId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        validateModeratorRole(report.getCommunity().getId(), requestingUserId);

        report.setStatus(request.getStatus());
        report.setResolvedBy(requestingUserId);
        report.setResolvedAt(Instant.now());

        if (request.getStatus() == ReportStatus.RESOLVED) {
            switch (report.getTargetType()) {
                case POST -> parseUuid(report.getTargetId()).flatMap(id -> postRepository.findByIdAndCommunity_Id(id, report.getCommunity().getId())).ifPresent(post -> {
                    post.setModerationStatus(ModerationStatus.REMOVED);
                    postRepository.save(post);
                });
                case COMMENT -> parseUuid(report.getTargetId()).flatMap(id -> commentRepository.findByIdAndCommunityId(id, report.getCommunity().getId())).ifPresent(comment -> {
                    comment.setModerationStatus(ModerationStatus.REMOVED);
                    commentRepository.save(comment);
                });
                case RESOURCE -> parseUuid(report.getTargetId()).flatMap(id -> resourceRepository.findByIdAndCommunity_Id(id, report.getCommunity().getId())).ifPresent(resource -> {
                    resource.setModerationStatus(ModerationStatus.REMOVED);
                    resourceRepository.save(resource);
                });
                case USER -> {
                    try {
                        Long targetUserId = Long.parseLong(report.getTargetId());
                        membershipRepository.findByCommunityIdAndUserId(report.getCommunity().getId(), targetUserId)
                                .ifPresent(m -> {
                                    m.setStatus(MembershipStatus.MUTED);
                                    membershipRepository.save(m);
                                });
                    } catch (Exception e) {
                        log.warn("Invalid user target ID on report {}", report.getId());
                    }
                }
            }
        }

        Report updated = reportRepository.save(report);
        auditLogService.record(report.getCommunity(), requestingUserId, "REPORT_" + request.getStatus(),
                "REPORT", reportId.toString(), "targetType=" + report.getTargetType());
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.ReportLifecycleEvent(
                this, report.getId(), report.getCommunity().getId(), report.getReporterId(), requestingUserId,
                request.getStatus() == ReportStatus.RESOLVED ? com.collabsphere_first.community.event.ReportLifecycleEvent.Action.RESOLVED : com.collabsphere_first.community.event.ReportLifecycleEvent.Action.DISMISSED,
                request.getStatus(), report.getTargetType(), report.getTargetId()));
        outboxService.record(report.getCommunity().getId(), "REPORT_" + request.getStatus(), report.getId().toString(),
                Map.of("reportId", report.getId(), "actorId", requestingUserId));
        return reportMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDto> getPendingPostsQueue(UUID communityId, Long requestingUserId, Pageable pageable) {
        validateModeratorRole(communityId, requestingUserId);
        return postRepository.findByCommunityIdAndModerationStatusOrderByPinnedDescCreatedAtDesc(
                communityId, ModerationStatus.PENDING_REVIEW, pageable)
                .map(postMapper::toDto);
    }

    @Override
    @Transactional
    public PostDto approveOrRejectPost(UUID postId, boolean approve, Long requestingUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        validateModeratorRole(post.getCommunity().getId(), requestingUserId);

        post.setModerationStatus(approve ? ModerationStatus.VISIBLE : ModerationStatus.REMOVED);
        Post updated = postRepository.save(post);
        auditLogService.record(post.getCommunity(), requestingUserId, approve ? "POST_APPROVED" : "POST_REJECTED", "POST", postId.toString(), null);
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.ContentModerationEvent(
                this, post.getCommunity().getId(), "POST", post.getId().toString(), post.getAuthorId(), requestingUserId,
                approve ? com.collabsphere_first.community.event.ContentModerationEvent.Action.POST_APPROVED : com.collabsphere_first.community.event.ContentModerationEvent.Action.POST_REJECTED,
                approve ? "Moderator approved" : "Moderator rejected"));
        outboxService.record(post.getCommunity().getId(), approve ? "POST_APPROVED" : "POST_REJECTED", post.getId().toString(),
                Map.of("postId", post.getId(), "actorId", requestingUserId));
        return postMapper.toDto(updated);
    }

    private void validateModeratorRole(UUID communityId, Long userId) {
        if (roleService != null && roleService.isPlatformAdmin(userId)) {
            return;
        }
        Membership membership = membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new InsufficientRoleException("Only Moderators, Admins, or Owners can view/resolve moderation reports"));
        if (membership.getStatus() != MembershipStatus.ACTIVE || !membership.getRole().hasAtLeastRank(MembershipRole.MODERATOR)) {
            throw new InsufficientRoleException("MODERATOR rank or higher required for moderation queue operations");
        }
    }

    private String normalizeAndValidateTarget(CreateReportRequest request, UUID communityId) {
        try {
            return switch (request.getTargetType()) {
                case POST -> {
                    UUID targetId = UUID.fromString(request.getTargetId());
                    if (postRepository.findByIdAndCommunity_Id(targetId, communityId).isEmpty()) throw new IllegalArgumentException();
                    yield targetId.toString();
                }
                case COMMENT -> {
                    UUID targetId = UUID.fromString(request.getTargetId());
                    if (commentRepository.findByIdAndCommunityId(targetId, communityId).isEmpty()) throw new IllegalArgumentException();
                    yield targetId.toString();
                }
                case RESOURCE -> {
                    UUID targetId = UUID.fromString(request.getTargetId());
                    if (resourceRepository.findByIdAndCommunity_Id(targetId, communityId).isEmpty()) throw new IllegalArgumentException();
                    yield targetId.toString();
                }
                case USER -> {
                    long targetId = Long.parseLong(request.getTargetId());
                    if (targetId <= 0 || membershipRepository.findByCommunityIdAndUserId(communityId, targetId).isEmpty()) {
                        throw new IllegalArgumentException();
                    }
                    yield Long.toString(targetId);
                }
            };
        } catch (RuntimeException e) {
            throw new ModerationException("Reported target is invalid or does not belong to this community");
        }
    }

    private Optional<UUID> parseUuid(String rawId) {
        try {
            return Optional.of(UUID.fromString(rawId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

