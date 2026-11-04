package com.collabsphere_first.community.event;

import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.service.CommunityNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityNotificationListener {

    private final CommunityNotificationService notificationService;
    private final MembershipRepository membershipRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberJoined(MemberJoinedEvent event) {
        notifyRoles(
                event.getCommunityId(),
                event.getUserId(),
                MembershipRole.ADMIN,
                "MEMBER_JOINED",
                "New community member",
                "A new member joined your community",
                "USER",
                event.getUserId().toString()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostCreated(PostCreatedEvent event) {
        notificationService.notifyActiveMembers(
                event.getCommunityId(),
                event.getAuthorId(),
                "NEW_POST",
                "New community post",
                preview(event.getContentText()),
                "POST",
                event.getPostId().toString()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportFiled(ReportFiledEvent event) {
        notifyRoles(
                event.getCommunityId(),
                event.getReporterId(),
                MembershipRole.MODERATOR,
                "REPORT_FILED",
                "New report to review",
                "A community member reported content",
                "REPORT",
                event.getReportId().toString()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvitationCreated(CommunityInvitationCreatedEvent event) {
        notificationService.createDirect(
                event.getInviteeId(),
                event.getCommunityId(),
                "INVITATION_RECEIVED",
                "Community invitation",
                "You have been invited to join a community",
                "INVITATION",
                event.getInvitationId().toString()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommunityEventCreated(CommunityEventCreatedEvent event) {
        notificationService.notifyActiveMembers(
                event.getCommunityId(),
                event.getActorId(),
                "EVENT_CREATED",
                "New community event",
                event.getTitle(),
                "EVENT",
                event.getEventId().toString()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventRsvpUpdated(CommunityEventRsvpUpdatedEvent event) {
        if (event.getEventCreatorId().equals(event.getActorId())) {
            return;
        }

        notificationService.createDirect(
                event.getEventCreatorId(),
                event.getCommunityId(),
                "EVENT_RSVP_UPDATED",
                "Event RSVP updated",
                "A member updated their RSVP: " + event.getStatus(),
                "EVENT",
                event.getEventId().toString()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMembershipLifecycle(MembershipLifecycleEvent event) {
        UUID communityId = event.getCommunityId();
        Long userId = event.getUserId();

        switch (event.getType()) {
            case JOIN_REQUESTED -> notifyRoles(
                    communityId,
                    userId,
                    MembershipRole.MODERATOR,
                    "JOIN_REQUESTED",
                    "New membership request",
                    "A user requested to join the community",
                    "USER",
                    userId.toString()
            );
            case JOIN_APPROVED -> notificationService.createDirect(
                    userId,
                    communityId,
                    "JOIN_APPROVED",
                    "Membership approved",
                    "Your request to join the community has been approved",
                    "COMMUNITY",
                    communityId.toString()
            );
            case JOIN_REJECTED -> notificationService.createDirect(
                    userId,
                    communityId,
                    "JOIN_REJECTED",
                    "Membership request declined",
                    "Your request to join the community was declined",
                    "COMMUNITY",
                    communityId.toString()
            );
            case MEMBER_LEFT -> notifyRoles(
                    communityId,
                    userId,
                    MembershipRole.ADMIN,
                    "MEMBER_LEFT",
                    "Member left community",
                    "A member has left the community",
                    "USER",
                    userId.toString()
            );
            case MEMBER_BANNED -> notificationService.createDirect(
                    userId,
                    communityId,
                    "MEMBER_BANNED",
                    "Account banned",
                    "You have been banned from the community",
                    "COMMUNITY",
                    communityId.toString()
            );
            case MEMBER_UNBANNED -> notificationService.createDirect(
                    userId,
                    communityId,
                    "MEMBER_UNBANNED",
                    "Account unbanned",
                    "Your ban in the community has been lifted",
                    "COMMUNITY",
                    communityId.toString()
            );
            case MEMBER_MUTED -> notificationService.createDirect(
                    userId,
                    communityId,
                    "MEMBER_MUTED",
                    "Account muted",
                    "You have been temporarily muted in the community",
                    "COMMUNITY",
                    communityId.toString()
            );
            case MEMBER_UNMUTED -> notificationService.createDirect(
                    userId,
                    communityId,
                    "MEMBER_UNMUTED",
                    "Account unmuted",
                    "You may now post and comment in the community again",
                    "COMMUNITY",
                    communityId.toString()
            );
            case MEMBER_SUSPENDED -> notificationService.createDirect(
                    userId,
                    communityId,
                    "MEMBER_SUSPENDED",
                    "Account suspended",
                    "Your community membership has been suspended",
                    "COMMUNITY",
                    communityId.toString()
            );
            case ROLE_CHANGED -> notificationService.createDirect(
                    userId,
                    communityId,
                    "ROLE_CHANGED",
                    "Role updated",
                    "Your role was updated to " + event.getNewRole(),
                    "COMMUNITY",
                    communityId.toString()
            );
            case OWNERSHIP_TRANSFERRED -> {
                notificationService.createDirect(
                        userId,
                        communityId,
                        "OWNERSHIP_TRANSFERRED",
                        "Ownership transferred",
                        "You are now the owner of the community",
                        "COMMUNITY",
                        communityId.toString()
                );
                if (event.getActorId() != null && !event.getActorId().equals(userId)) {
                    notificationService.createDirect(
                            event.getActorId(),
                            communityId,
                            "OWNERSHIP_TRANSFERRED",
                            "Ownership transferred",
                            "Community ownership has been successfully transferred",
                            "COMMUNITY",
                            communityId.toString()
                    );
                }
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvitationLifecycle(InvitationLifecycleEvent event) {
        switch (event.getType()) {
            case INVITATION_ACCEPTED -> notificationService.createDirect(
                    event.getInviterId(),
                    event.getCommunityId(),
                    "INVITATION_ACCEPTED",
                    "Invitation accepted",
                    "A user accepted your community invitation",
                    "INVITATION",
                    event.getInvitationId().toString()
            );
            case INVITATION_DECLINED -> notificationService.createDirect(
                    event.getInviterId(),
                    event.getCommunityId(),
                    "INVITATION_DECLINED",
                    "Invitation declined",
                    "A user declined your community invitation",
                    "INVITATION",
                    event.getInvitationId().toString()
            );
            case INVITATION_REVOKED -> notificationService.createDirect(
                    event.getInviteeId(),
                    event.getCommunityId(),
                    "INVITATION_REVOKED",
                    "Invitation revoked",
                    "Your invitation to join the community was revoked",
                    "INVITATION",
                    event.getInvitationId().toString()
            );
            case INVITATION_EXPIRED -> log.debug("Invitation {} expired", event.getInvitationId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContentModeration(ContentModerationEvent event) {
        if (event.getAuthorId() == null || event.getAuthorId().equals(event.getModeratorId())) {
            return;
        }

        String title;
        String body;
        switch (event.getAction()) {
            case POST_APPROVED -> {
                title = "Post approved";
                body = "Your post has been approved and is now visible";
            }
            case POST_REJECTED -> {
                title = "Post rejected";
                body = "Your post was rejected by moderators" + (event.getReason() != null ? ": " + event.getReason() : "");
            }
            case COMMENT_APPROVED -> {
                title = "Comment approved";
                body = "Your comment has been approved and is now visible";
            }
            case COMMENT_REJECTED -> {
                title = "Comment rejected";
                body = "Your comment was rejected by moderators" + (event.getReason() != null ? ": " + event.getReason() : "");
            }
            case RESOURCE_APPROVED -> {
                title = "Resource approved";
                body = "Your resource has been approved and is now visible";
            }
            case RESOURCE_REJECTED -> {
                title = "Resource rejected";
                body = "Your resource was rejected by moderators" + (event.getReason() != null ? ": " + event.getReason() : "");
            }
            default -> {
                title = "Content moderation update";
                body = "A moderation action was taken on your content";
            }
        }

        notificationService.createDirect(
                event.getAuthorId(),
                event.getCommunityId(),
                event.getAction().name(),
                title,
                body,
                event.getTargetType(),
                event.getTargetId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        // If it is a nested reply to another comment, notify the parent comment's author
        if (event.getParentAuthorId() != null && !event.getParentAuthorId().equals(event.getAuthorId())) {
            notificationService.createDirect(
                    event.getParentAuthorId(),
                    event.getCommunityId(),
                    "REPLY_CREATED",
                    "New reply to your comment",
                    preview(event.getBody()),
                    "COMMENT",
                    event.getCommentId().toString()
            );
        } else if (event.getPostAuthorId() != null && !event.getPostAuthorId().equals(event.getAuthorId())) {
            // Otherwise, notify the post author
            notificationService.createDirect(
                    event.getPostAuthorId(),
                    event.getCommunityId(),
                    "COMMENT_CREATED",
                    "New comment on your post",
                    preview(event.getBody()),
                    "POST",
                    event.getPostId().toString()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostLifecycle(PostLifecycleEvent event) {
        if (event.getAuthorId() == null || event.getAuthorId().equals(event.getActorId())) {
            return;
        }

        if (event.getAction() == PostLifecycleEvent.Action.PINNED) {
            notificationService.createDirect(
                    event.getAuthorId(),
                    event.getCommunityId(),
                    "POST_PINNED",
                    "Post pinned",
                    "Your post '" + preview(event.getTitle()) + "' was pinned by a moderator",
                    "POST",
                    event.getPostId().toString()
            );
        } else if (event.getAction() == PostLifecycleEvent.Action.LOCKED) {
            notificationService.createDirect(
                    event.getAuthorId(),
                    event.getCommunityId(),
                    "POST_LOCKED",
                    "Post locked",
                    "Your post '" + preview(event.getTitle()) + "' was locked for new comments",
                    "POST",
                    event.getPostId().toString()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportLifecycle(ReportLifecycleEvent event) {
        if (event.getReporterId() == null) {
            return;
        }

        if (event.getAction() == ReportLifecycleEvent.Action.RESOLVED) {
            notificationService.createDirect(
                    event.getReporterId(),
                    event.getCommunityId(),
                    "REPORT_RESOLVED",
                    "Report resolved",
                    "Your report has been reviewed and resolved by community moderators",
                    "REPORT",
                    event.getReportId().toString()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPollLifecycle(PollLifecycleEvent event) {
        if (event.getCreatorId() == null) {
            return;
        }

        if (event.getAction() == PollLifecycleEvent.Action.CLOSED) {
            notificationService.createDirect(
                    event.getCreatorId(),
                    event.getCommunityId(),
                    "POLL_CLOSED",
                    "Poll closed",
                    "Your poll '" + preview(event.getQuestion()) + "' has been closed",
                    "POLL",
                    event.getPollId().toString()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResourceLifecycle(ResourceLifecycleEvent event) {
        if (event.getAction() == ResourceLifecycleEvent.Action.CREATED) {
            notificationService.notifyActiveMembers(
                    event.getCommunityId(),
                    event.getUploaderId(),
                    "RESOURCE_CREATED",
                    "New community resource",
                    preview(event.getTitle()),
                    "RESOURCE",
                    event.getResourceId().toString()
            );
        } else if (event.getUploaderId() != null && !event.getUploaderId().equals(event.getActorId())) {
            String title = event.getAction() == ResourceLifecycleEvent.Action.APPROVED ? "Resource approved" : "Resource rejected";
            String body = event.getAction() == ResourceLifecycleEvent.Action.APPROVED
                    ? "Your resource '" + preview(event.getTitle()) + "' was approved by moderators"
                    : "Your resource '" + preview(event.getTitle()) + "' was rejected by moderators";
            notificationService.createDirect(
                    event.getUploaderId(),
                    event.getCommunityId(),
                    event.getAction().name(),
                    title,
                    body,
                    "RESOURCE",
                    event.getResourceId().toString()
            );
        }
    }

    private void notifyRoles(
            UUID communityId,
            Long excludedUserId,
            MembershipRole minimumRole,
            String type,
            String title,
            String body,
            String targetType,
            String targetId
    ) {
        int page = 0;

        while (true) {
            Page<Membership> memberships =
                    membershipRepository.findByCommunityIdAndStatus(
                            communityId,
                            MembershipStatus.ACTIVE,
                            PageRequest.of(page, 500)
                    );

            memberships.getContent()
                    .stream()
                    .filter(m -> !m.getUserId().equals(excludedUserId))
                    .filter(m -> m.getRole().hasAtLeastRank(minimumRole))
                    .forEach(m ->
                            notificationService.create(
                                    m.getUserId(),
                                    communityId,
                                    type,
                                    title,
                                    body,
                                    targetType,
                                    targetId
                            )
                    );

            if (!memberships.hasNext()) {
                break;
            }

            page++;
        }
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }

        String clean = content
                .replaceAll("\\s+", " ")
                .trim();

        return clean.length() <= 160
                ? clean
                : clean.substring(0, 157) + "...";
    }
}
