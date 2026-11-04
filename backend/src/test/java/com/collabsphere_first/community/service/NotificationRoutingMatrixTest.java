package com.collabsphere_first.community.service;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.CommunityNotification;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.NotificationPreference;
import com.collabsphere_first.community.event.*;
import com.collabsphere_first.community.repository.CommunityNotificationRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRoutingMatrixTest {

    @Mock private CommunityNotificationRepository notificationRepository;
    @Mock private MembershipRepository membershipRepository;

    private CommunityNotificationService notificationService;
    private CommunityNotificationListener listener;

    private UUID communityId;
    private Long userAlice = 101L; // Creator/Admin
    private Long userBob = 102L;   // Regular Member
    private Long userCharlie = 103L; // Non-member/Invitee

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        notificationService = new CommunityNotificationService(notificationRepository, membershipRepository);
        listener = new CommunityNotificationListener(notificationService, membershipRepository);
    }

    @Test
    @DisplayName("Join requested notifies community moderators/admins")
    void joinRequested_notifiesModerators() {
        Membership admin = Membership.builder().community(Community.builder().id(communityId).build())
                .userId(userAlice).role(MembershipRole.ADMIN).status(MembershipStatus.ACTIVE)
                .notificationPref(NotificationPreference.ALL).build();

        when(membershipRepository.findByCommunityIdAndStatus(eq(communityId), eq(MembershipStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(admin)));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, userAlice))
                .thenReturn(Optional.of(admin));

        MembershipLifecycleEvent event = new MembershipLifecycleEvent(
                this, communityId, userBob, userBob,
                MembershipLifecycleEvent.Type.JOIN_REQUESTED, null, null, null, null);

        listener.onMembershipLifecycle(event);

        ArgumentCaptor<CommunityNotification> captor = ArgumentCaptor.forClass(CommunityNotification.class);
        verify(notificationRepository).save(captor.capture());
        CommunityNotification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userAlice);
        assertThat(saved.getType()).isEqualTo("JOIN_REQUESTED");
    }

    @Test
    @DisplayName("Join approved notifies user directly")
    void joinApproved_notifiesUserDirectly() {
        MembershipLifecycleEvent event = new MembershipLifecycleEvent(
                this, communityId, userBob, userAlice,
                MembershipLifecycleEvent.Type.JOIN_APPROVED, null, null, null, null);

        listener.onMembershipLifecycle(event);

        ArgumentCaptor<CommunityNotification> captor = ArgumentCaptor.forClass(CommunityNotification.class);
        verify(notificationRepository).save(captor.capture());
        CommunityNotification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userBob);
        assertThat(saved.getType()).isEqualTo("JOIN_APPROVED");
    }

    @Test
    @DisplayName("Comment created notifies post author without self-notification")
    void commentCreated_notifiesPostAuthor_noSelfNotification() {
        // Bob comments on Alice's post -> Alice notified
        CommentCreatedEvent event = new CommentCreatedEvent(
                this, UUID.randomUUID(), UUID.randomUUID(), communityId,
                userBob, null, null, userAlice, "Nice post!");

        listener.onCommentCreated(event);

        ArgumentCaptor<CommunityNotification> captor = ArgumentCaptor.forClass(CommunityNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userAlice);
        assertThat(captor.getValue().getType()).isEqualTo("COMMENT_CREATED");

        // Alice comments on her own post -> no notification
        reset(notificationRepository);
        CommentCreatedEvent selfEvent = new CommentCreatedEvent(
                this, UUID.randomUUID(), UUID.randomUUID(), communityId,
                userAlice, null, null, userAlice, "My own reply");
        listener.onCommentCreated(selfEvent);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nested reply notifies parent comment author")
    void nestedReply_notifiesParentAuthor() {
        CommentCreatedEvent event = new CommentCreatedEvent(
                this, UUID.randomUUID(), UUID.randomUUID(), communityId,
                userCharlie, UUID.randomUUID(), userBob, userAlice, "Replying to Bob!");

        listener.onCommentCreated(event);

        ArgumentCaptor<CommunityNotification> captor = ArgumentCaptor.forClass(CommunityNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userBob);
        assertThat(captor.getValue().getType()).isEqualTo("REPLY_CREATED");
    }

    @Test
    @DisplayName("Invitation lifecycle notifies recipient and inviter appropriately")
    void invitationLifecycle_routing() {
        // Received
        listener.onInvitationCreated(new CommunityInvitationCreatedEvent(this, UUID.randomUUID(), communityId, userCharlie));
        ArgumentCaptor<CommunityNotification> captor = ArgumentCaptor.forClass(CommunityNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userCharlie);
        assertThat(captor.getValue().getType()).isEqualTo("INVITATION_RECEIVED");

        // Accepted
        reset(notificationRepository);
        listener.onInvitationLifecycle(new InvitationLifecycleEvent(
                this, UUID.randomUUID(), communityId, userAlice, userCharlie,
                InvitationLifecycleEvent.Type.INVITATION_ACCEPTED));
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userAlice);
        assertThat(captor.getValue().getType()).isEqualTo("INVITATION_ACCEPTED");
    }

    @Test
    @DisplayName("Notifications are not delivered to banned members for community broadcast")
    void bannedMember_doesNotReceiveBroadcast() {
        Membership active = Membership.builder().community(Community.builder().id(communityId).build())
                .userId(userBob).role(MembershipRole.MEMBER).status(MembershipStatus.ACTIVE)
                .notificationPref(NotificationPreference.ALL).build();

        when(membershipRepository.findByCommunityIdAndStatus(eq(communityId), eq(MembershipStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(active)));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, userBob))
                .thenReturn(Optional.of(active));

        notificationService.notifyActiveMembers(communityId, userAlice, "POLL_CREATED", "Poll", "Body", "POLL", "1");

        ArgumentCaptor<CommunityNotification> captor = ArgumentCaptor.forClass(CommunityNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userBob);
    }
}
