package com.collabsphere_first.report.service;

import com.collabsphere_first.common.ratelimit.PlatformRateLimiter;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.repository.PostRepository;
import com.collabsphere_first.messaging.entity.DirectMessage;
import com.collabsphere_first.messaging.repository.ConversationParticipantRepository;
import com.collabsphere_first.messaging.repository.DirectMessageRepository;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.notification.service.NotificationDeviceService;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.refreshtoken.repository.RefreshTokenRepository;
import com.collabsphere_first.report.dto.CreatePlatformReportRequest;
import com.collabsphere_first.report.dto.PlatformReportResponse;
import com.collabsphere_first.report.dto.ResolvePlatformReportRequest;
import com.collabsphere_first.report.entity.PlatformReport;
import com.collabsphere_first.report.enums.PlatformModerationAction;
import com.collabsphere_first.report.enums.PlatformReportStatus;
import com.collabsphere_first.report.enums.PlatformReportTargetType;
import com.collabsphere_first.report.exception.ReportAccessDeniedException;
import com.collabsphere_first.report.exception.ReportConflictException;
import com.collabsphere_first.report.exception.ReportNotFoundException;
import com.collabsphere_first.report.repository.PlatformReportRepository;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformReportServiceImpl implements PlatformReportService {

    private final PlatformReportRepository reportRepository;
    private final PlatformAuditLogService auditLogService;
    private final PlatformRateLimiter rateLimiter;
    private final UserRepository userRepository;
    private final PlatformPostRepository platformPostRepository;
    private final PostRepository communityPostRepository;
    private final MembershipRepository membershipRepository;
    private final DirectMessageRepository directMessageRepository;
    private final ConversationParticipantRepository participantRepository;
    private final NetworkAccessService networkAccessService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationDeviceService notificationDeviceService;

    @Override
    @Transactional
    public PlatformReportResponse fileReport(CreatePlatformReportRequest request, Long reporterUserId) {
        if (reporterUserId == null) {
            throw new ReportAccessDeniedException("Authenticated user required to file a report");
        }

        // Rate limit report filing to prevent flooding
        rateLimiter.checkReportCreation(reporterUserId);

        // Verify target access & legitimacy
        validateTargetAccess(request.getTargetType(), request.getTargetId(), reporterUserId);

        // Anti-spam duplicate check: prevent multiple active reports on the same target by the same reporter
        reportRepository.findByReporterUserIdAndTargetTypeAndTargetIdAndStatus(
                reporterUserId,
                request.getTargetType(),
                request.getTargetId(),
                PlatformReportStatus.PENDING
        ).ifPresent(existing -> {
            throw new ReportConflictException("You have already submitted an active report for this content.");
        });

        PlatformReport report = PlatformReport.builder()
                .id(UUID.randomUUID())
                .reporterUserId(reporterUserId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .details(request.getDetails())
                .status(PlatformReportStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PlatformReport saved = reportRepository.save(report);
        log.info("Platform report filed: id={}, type={}, targetId={} by reporter={}",
                saved.getId(), saved.getTargetType(), saved.getTargetId(), reporterUserId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlatformReportResponse> getReportQueue(PlatformReportStatus status, Pageable pageable) {
        Page<PlatformReport> page = (status != null)
                ? reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : reportRepository.findAllByOrderByCreatedAtDesc(pageable);

        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public PlatformReportResponse resolveReport(UUID reportId, ResolvePlatformReportRequest request, Long adminId) {
        if (adminId == null) {
            throw new ReportAccessDeniedException("Authenticated administrator required to resolve reports");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ReportAccessDeniedException("Admin user not found"));

        boolean isPlatformAdmin = admin.getRoles() != null && admin.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        if (!isPlatformAdmin) {
            throw new ReportAccessDeniedException("Only platform administrators (ROLE_ADMIN) can resolve platform reports");
        }

        PlatformReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found with ID: " + reportId));

        if (report.getStatus() == PlatformReportStatus.RESOLVED || report.getStatus() == PlatformReportStatus.DISMISSED) {
            throw new ReportConflictException("Report has already been resolved or dismissed.");
        }

        PlatformModerationAction action = request.getAction();
        executeModerationAction(report, action, adminId, request.getResolutionNotes());

        report.setStatus(action == PlatformModerationAction.DISMISS_REPORT ? PlatformReportStatus.DISMISSED : PlatformReportStatus.RESOLVED);
        report.setResolution(action);
        report.setResolutionNotes(request.getResolutionNotes());
        report.setResolvedBy(adminId);
        report.setResolvedAt(Instant.now());
        report.setUpdatedAt(Instant.now());

        PlatformReport saved = reportRepository.save(report);

        // Audit log privileged moderation action
        auditLogService.logAction(
                adminId,
                "RESOLVE_REPORT_" + action.name(),
                report.getTargetType().name(),
                report.getTargetId(),
                "reportId=" + report.getId() + "; notes=" + request.getResolutionNotes()
        );

        return mapToResponse(saved);
    }

    private void validateTargetAccess(PlatformReportTargetType targetType, String targetId, Long reporterUserId) {
        switch (targetType) {
            case USER -> {
                Long targetUserId;
                try {
                    targetUserId = Long.parseLong(targetId);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid user ID format for report: " + targetId);
                }
                if (Objects.equals(reporterUserId, targetUserId)) {
                    throw new IllegalArgumentException("Cannot report your own account");
                }
                User targetUser = userRepository.findById(targetUserId)
                        .orElseThrow(() -> new ReportNotFoundException("Target user not found: " + targetUserId));
                if (Boolean.TRUE.equals(targetUser.getDeleted())) {
                    throw new ReportNotFoundException("Target user is already deleted.");
                }
            }
            case PLATFORM_POST -> {
                UUID postId;
                try {
                    postId = UUID.fromString(targetId);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Invalid platform post UUID format: " + targetId);
                }
                PlatformPost post = platformPostRepository.findById(postId)
                        .orElseThrow(() -> new ReportNotFoundException("Platform post not found: " + postId));
                if (post.isDeleted()) {
                    throw new ReportNotFoundException("Platform post is deleted: " + postId);
                }
                // Verify reporter is authorized to view this post
                if (post.getVisibility() == PostVisibility.PRIVATE) {
                    if (!post.getAuthor().getId().equals(reporterUserId)) {
                        throw new ReportAccessDeniedException("You do not have access to view this private post");
                    }
                } else if (post.getVisibility() == PostVisibility.FOLLOWERS_ONLY) {
                    Long authorId = post.getAuthor().getId();
                    if (!authorId.equals(reporterUserId)) {
                        if (networkAccessService.isBlockedEitherDirection(reporterUserId, authorId)
                                || !networkAccessService.isFollowing(reporterUserId, authorId)) {
                            throw new ReportAccessDeniedException("You do not have access to view this followers-only post");
                        }
                    }
                } else if (post.getVisibility() == PostVisibility.PUBLIC) {
                    Long authorId = post.getAuthor().getId();
                    if (!authorId.equals(reporterUserId) && networkAccessService.isBlockedEitherDirection(reporterUserId, authorId)) {
                        throw new ReportAccessDeniedException("You do not have access to view content from this user");
                    }
                }
            }
            case COMMUNITY_POST -> {
                UUID postId;
                try {
                    postId = UUID.fromString(targetId);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Invalid community post UUID format: " + targetId);
                }
                Post post = communityPostRepository.findById(postId)
                        .orElseThrow(() -> new ReportNotFoundException("Community post not found: " + postId));
                Community community = post.getCommunity();
                if (community.getVisibility() != Visibility.PUBLIC) {
                    boolean isMember = membershipRepository.findByCommunityIdAndUserId(community.getId(), reporterUserId)
                            .filter(m -> m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.MUTED)
                            .isPresent();
                    if (!isMember) {
                        throw new ReportAccessDeniedException("You must be a member of the community to access this post");
                    }
                }
            }
            case DIRECT_MESSAGE -> {
                UUID messageId;
                try {
                    messageId = UUID.fromString(targetId);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Invalid direct message UUID format: " + targetId);
                }
                DirectMessage message = directMessageRepository.findById(messageId)
                        .orElseThrow(() -> new ReportNotFoundException("Direct message not found: " + messageId));
                boolean isParticipant = participantRepository.existsByIdConversationIdAndIdUserId(
                        message.getConversationId(),
                        reporterUserId
                );
                if (!isParticipant) {
                    throw new ReportAccessDeniedException("You are not a participant in this conversation");
                }
            }
        }
    }

    private void executeModerationAction(PlatformReport report, PlatformModerationAction action, Long adminId, String notes) {
        switch (action) {
            case DISMISS_REPORT -> log.info("Report {} dismissed by admin {}", report.getId(), adminId);
            case REMOVE_PLATFORM_POST -> {
                if (report.getTargetType() == PlatformReportTargetType.PLATFORM_POST) {
                    UUID postId = UUID.fromString(report.getTargetId());
                    platformPostRepository.findById(postId).ifPresent(post -> {
                        post.softDelete();
                        platformPostRepository.save(post);
                        log.info("PlatformPost {} soft-deleted by admin {} via report resolution", postId, adminId);
                    });
                }
            }
            case SUSPEND_ACCOUNT -> {
                Long targetUserId = extractTargetUserId(report);
                if (targetUserId != null) {
                    userRepository.findById(targetUserId).ifPresent(user -> {
                        user.setDeactivated(true);
                        user.setAccountEnabled(false);
                        user.setDeactivatedAt(java.time.LocalDateTime.now());
                        userRepository.save(user);
                        refreshTokenRepository.deleteByUser(user);
                        notificationDeviceService.revokeAllForUser(user.getId());
                        log.info("User {} suspended by admin {} via report resolution", targetUserId, adminId);
                    });
                }
            }
            case RESTRICT_ACCOUNT -> {
                Long targetUserId = extractTargetUserId(report);
                if (targetUserId != null) {
                    log.info("User {} restricted by admin {} via report resolution: {}", targetUserId, adminId, notes);
                }
            }
        }
    }

    private Long extractTargetUserId(PlatformReport report) {
        if (report.getTargetType() == PlatformReportTargetType.USER) {
            try {
                return Long.parseLong(report.getTargetId());
            } catch (Exception e) {
                return null;
            }
        }
        if (report.getTargetType() == PlatformReportTargetType.PLATFORM_POST) {
            try {
                return platformPostRepository.findById(UUID.fromString(report.getTargetId()))
                        .map(p -> p.getAuthor().getId())
                        .orElse(null);
            } catch (Exception e) {
                return null;
            }
        }
        if (report.getTargetType() == PlatformReportTargetType.DIRECT_MESSAGE) {
            try {
                return directMessageRepository.findById(UUID.fromString(report.getTargetId()))
                        .map(DirectMessage::getSenderId)
                        .orElse(null);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private PlatformReportResponse mapToResponse(PlatformReport report) {
        return PlatformReportResponse.builder()
                .id(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .details(report.getDetails())
                .status(report.getStatus())
                .resolution(report.getResolution())
                .resolutionNotes(report.getResolutionNotes())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}
