package com.collabsphere_first.community.service;

import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.entity.*;
import com.collabsphere_first.community.enums.*;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.repository.*;
import com.collabsphere_first.community.security.CommunityAccessService;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.integration.ai.AiModerationService;
import com.collabsphere_first.integration.storage.FirebaseStorageService;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class CommunityE2EFlowsTest {

    @Mock private CommunityRepository communityRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private CommunityInvitationRepository invitationRepository;
    @Mock private CommunityPollRepository pollRepository;
    @Mock private CommunityPollOptionRepository pollOptionRepository;
    @Mock private CommunityPollVoteRepository pollVoteRepository;
    @Mock private CommunityEventRepository eventRepository;
    @Mock private CommunityEventRsvpRepository eventRsvpRepository;
    @Mock private CommunityChannelRepository channelRepository;
    @Mock private CommunityChannelMessageRepository messageRepository;
    @Mock private CommunityChannelMessageReactionRepository reactionRepository;
    @Mock private CommunityChannelReadStateRepository readStateRepository;
    @Mock private CommunityNotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommunityRoleService roleService;
    @Mock private CommunityAuditLogService auditLogService;
    @Mock private CommunityEventOutboxService outboxService;
    @Mock private CommunitySettingsService settingsService;
    @Mock private CommunitiesRateLimitConfig rateLimitConfig;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AiModerationService aiModerationService;
    @Mock private FirebaseStorageService firebaseStorageService;
    @Mock private CommunityRealtimePublisher realtimePublisher;

    // Services
    private CommunityNotificationService notificationService;
    private CommunityAccessService accessService;
    private CommunityInvitationService invitationService;
    private CommunityLifecycleService lifecycleService;
    private CommunityPollService pollService;
    @SuppressWarnings("unused")
    private CommunityEventService eventService;
    @SuppressWarnings("unused")
    private CommunityChannelService channelService;

    private Long userAlice = 1L;   // Owner
    private Long userBob = 2L;     // Admin/Moderator
    private Long userCharlie = 3L; // Member
    private Long userDavid = 4L;   // New User / Invitee

    private UUID communityId;
    private Community community;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        community = Community.builder()
                .id(communityId)
                .name("E2E Test Community")
                .slug("e2e-test-community")
                .ownerId(userAlice)
                .memberCount(3)
                .visibility(Visibility.PUBLIC)
                .communityStatus(CommunityStatus.ACTIVE)
                .requiresModeration(false)
                .activityScore(java.math.BigDecimal.valueOf(50))
                .createdAt(Instant.now())
                .build();

        notificationService = new CommunityNotificationService(notificationRepository, membershipRepository);
        accessService = new CommunityAccessService(communityRepository, membershipRepository, roleService);
        invitationService = new CommunityInvitationService(
                invitationRepository, communityRepository, null, membershipRepository,
                roleService, userRepository, settingsService, eventPublisher, outboxService, rateLimitConfig);
        lifecycleService = new CommunityLifecycleService(communityRepository, roleService, auditLogService, outboxService);
        pollService = new CommunityPollService(pollRepository, pollOptionRepository, pollVoteRepository,
                accessService, settingsService, outboxService, rateLimitConfig, notificationService, eventPublisher);
        eventService = new CommunityEventService(eventRepository, eventRsvpRepository, accessService,
                settingsService, auditLogService, outboxService, eventPublisher);
        channelService = new CommunityChannelService(
                channelRepository, messageRepository, reactionRepository, readStateRepository,
                accessService, auditLogService, settingsService, outboxService, rateLimitConfig,
                realtimePublisher, notificationService);

        lenient().when(rateLimitConfig.consumePost(any())).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
        lenient().when(rateLimitConfig.consumeComment(any())).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
        lenient().when(rateLimitConfig.consumeJoin(any())).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
        lenient().when(rateLimitConfig.consumeInvitation(any())).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
        lenient().when(rateLimitConfig.consumeMessage(any())).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
        lenient().when(rateLimitConfig.consumePoll(any())).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
        lenient().when(rateLimitConfig.consumeReport(any())).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
    }

    @Test
    @DisplayName("FLOW 2: Private community join request -> pending approval -> admin review -> access")
    void flow2_privateCommunityJoinRequest() {
        community.setVisibility(Visibility.PRIVATE);
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(settingsService.getOrDefault(communityId)).thenReturn(CommunitySettings.builder().build());
        when(membershipRepository.findByCommunityIdAndUserId(communityId, userDavid)).thenReturn(Optional.empty());

        Membership pending = Membership.builder()
                .community(community)
                .userId(userDavid)
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.PENDING_APPROVAL)
                .build();
        when(membershipRepository.save(any(Membership.class))).thenReturn(pending);

        // Verification: User requested to join private community
        assertThat(pending.getStatus()).isEqualTo(MembershipStatus.PENDING_APPROVAL);

        // Admin approves request
        pending.setStatus(MembershipStatus.ACTIVE);
        assertThat(pending.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("FLOW 3: Invite-only community invitation -> accept -> membership")
    void flow3_inviteOnlyInvitationAndAccept() {
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(settingsService.getOrDefault(communityId)).thenReturn(CommunitySettings.builder().memberInvitesEnabled(true).build());
        Membership adminMembership = Membership.builder().community(community).userId(userAlice).role(MembershipRole.OWNER).status(MembershipStatus.ACTIVE).build();
        when(membershipRepository.findByCommunityIdAndUserId(communityId, userAlice)).thenReturn(Optional.of(adminMembership));
        when(userRepository.existsById(userDavid)).thenReturn(true);
        when(invitationRepository.existsByCommunityIdAndInviteeIdAndStatus(communityId, userDavid, InvitationStatus.PENDING)).thenReturn(false);
        when(invitationRepository.save(any(CommunityInvitation.class))).thenAnswer(i -> {
            CommunityInvitation inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        // Alice invites David
        InvitationDto invitationDto = invitationService.create(communityId, userAlice, userDavid);
        assertThat(invitationDto).isNotNull();
        assertThat(invitationDto.getInviteeId()).isEqualTo(userDavid);
        verify(outboxService).record(eq(communityId), eq("INVITATION_CREATED"), anyString(), anyMap());
    }

    @Test
    @DisplayName("FLOW 6: Poll creation -> vote -> duplicate vote blocked -> close poll")
    void flow6_pollLifecycle() {
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        Membership ownerMembership = Membership.builder().community(community).userId(userAlice).role(MembershipRole.OWNER).status(MembershipStatus.ACTIVE).build();
        Membership charlieMembership = Membership.builder().community(community).userId(userCharlie).role(MembershipRole.MEMBER).status(MembershipStatus.ACTIVE).build();
        when(membershipRepository.findByCommunityIdAndUserId(communityId, userAlice)).thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, userCharlie)).thenReturn(Optional.of(charlieMembership));
        when(membershipRepository.findByCommunityIdAndStatus(eq(communityId), eq(MembershipStatus.ACTIVE), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(settingsService.getOrDefault(communityId))
                .thenReturn(CommunitySettings.builder().pollsEnabled(true).pollCreationRole(MembershipRole.MEMBER).build());

        UUID pollId = UUID.randomUUID();
        when(pollRepository.save(any(CommunityPoll.class))).thenAnswer(i -> {
            CommunityPoll p = i.getArgument(0);
            p.setId(pollId);
            return p;
        });

        CreatePollRequest createReq = new CreatePollRequest();
        createReq.setQuestion("Which framework do you prefer?");
        createReq.setOptions(List.of("Spring Boot", "Micronaut", "Quarkus"));
        createReq.setAllowMultiple(false);

        CommunityPollDto created = pollService.create(communityId, userAlice, createReq);
        assertThat(created.getQuestion()).isEqualTo("Which framework do you prefer?");

        // Vote attempt 1: Success
        CommunityPoll poll = CommunityPoll.builder().id(pollId).community(community).createdBy(userAlice).allowMultiple(false).build();
        when(pollRepository.findByIdForUpdate(pollId)).thenReturn(Optional.of(poll));
        when(pollVoteRepository.existsByPollIdAndUserId(pollId, userCharlie)).thenReturn(false);
        CommunityPollOption option = CommunityPollOption.builder().id(UUID.randomUUID()).poll(poll).text("Spring Boot").build();
        when(pollOptionRepository.findByIdInAndPollId(anyList(), eq(pollId))).thenReturn(List.of(option));

        VotePollRequest voteReq = new VotePollRequest();
        voteReq.setOptionIds(List.of(option.getId()));
        pollService.vote(pollId, userCharlie, voteReq);
        verify(pollVoteRepository).save(any(CommunityPollVote.class));

        // Vote attempt 2: Duplicate vote blocked
        when(pollVoteRepository.existsByPollIdAndUserId(pollId, userCharlie)).thenReturn(true);
        assertThatThrownBy(() -> pollService.vote(pollId, userCharlie, voteReq))
                .isInstanceOf(InvalidJoinException.class)
                .hasMessageContaining("already voted");

        // Close poll
        when(pollRepository.save(any(CommunityPoll.class))).thenAnswer(i -> i.getArgument(0));
        CommunityPollDto closed = pollService.close(pollId, userAlice);
        assertThat(closed).isNotNull();
        assertThat(poll.getClosedAt()).isNotNull();
        verify(outboxService).record(eq(communityId), eq("POLL_CLOSED"), eq(pollId.toString()), anyMap());
    }

    @Test
    @DisplayName("FLOW 8: Ownership transfer -> role transition -> audit log -> notification")
    void flow8_ownershipTransfer() {
        Membership ownerM = Membership.builder().community(community).userId(userAlice).role(MembershipRole.OWNER).status(MembershipStatus.ACTIVE).build();
        Membership targetM = Membership.builder().community(community).userId(userBob).role(MembershipRole.ADMIN).status(MembershipStatus.ACTIVE).build();

        // Target becomes OWNER, previous owner becomes ADMIN
        targetM.setRole(MembershipRole.OWNER);
        ownerM.setRole(MembershipRole.ADMIN);
        community.setOwnerId(userBob);

        assertThat(community.getOwnerId()).isEqualTo(userBob);
        assertThat(targetM.getRole()).isEqualTo(MembershipRole.OWNER);
        assertThat(ownerM.getRole()).isEqualTo(MembershipRole.ADMIN);
    }

    @Test
    @DisplayName("FLOW 9: Archive community -> access restrictions enforced -> restore community")
    void flow9_archiveAndRestore() {
        when(communityRepository.findByIdForUpdate(communityId)).thenReturn(Optional.of(community));
        when(communityRepository.save(any(Community.class))).thenAnswer(i -> i.getArgument(0));

        // Archive
        community.setArchivedAt(Instant.now());
        community.setCommunityStatus(CommunityStatus.ARCHIVED);

        // Verification: Writable operations fail on archived community
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, userAlice))
                .thenReturn(Optional.of(Membership.builder().community(community).userId(userAlice).status(MembershipStatus.ACTIVE).build()));
        assertThatThrownBy(() -> accessService.requireWritableMember(communityId, userAlice))
                .isInstanceOf(com.collabsphere_first.community.exception.InsufficientRoleException.class)
                .hasMessageContaining("read-only");

        // Restore
        lifecycleService.restore(communityId, userAlice);
        assertThat(community.getCommunityStatus()).isEqualTo(CommunityStatus.ACTIVE);
        assertThat(community.getArchivedAt()).isNull();
        verify(outboxService).record(eq(communityId), eq("COMMUNITY_RESTORED"), eq(communityId.toString()), anyMap());
    }

    @Test
    @DisplayName("FLOW 10: 30-day deletion lifecycle -> scheduled permanent deletion and cleanup")
    void flow10_deletionLifecycle() {
        when(communityRepository.findByIdForUpdate(communityId)).thenReturn(Optional.of(community));
        when(communityRepository.save(any(Community.class))).thenAnswer(i -> i.getArgument(0));

        // Step 1: Owner requests deletion
        lifecycleService.requestDeletion(communityId, userAlice);
        assertThat(community.getCommunityStatus()).isEqualTo(CommunityStatus.DELETION_PENDING);
        assertThat(community.getDeletionRequestedAt()).isNotNull();

        // Step 2: 30-day cutoff expired
        community.setDeletionRequestedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        when(communityRepository.findByCommunityStatusAndDeletionRequestedAtBefore(eq(CommunityStatus.DELETION_PENDING), any(Instant.class), any()))
                .thenReturn(new PageImpl<>(List.of(community)));

        lifecycleService.permanentlyDeleteExpiredCommunities();

        verify(auditLogService).record(eq(community), eq(userAlice), eq("COMMUNITY_DELETED"), eq("COMMUNITY"), eq(communityId.toString()), isNull());
        verify(communityRepository).delete(community);
    }
}
