package com.collabsphere_first.community.service;

import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import com.collabsphere_first.community.dto.MembershipDto;
import com.collabsphere_first.community.dto.UpdateMembershipRequest;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.*;
import com.collabsphere_first.community.event.MembershipLifecycleEvent;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.mapper.MembershipMapper;
import com.collabsphere_first.community.repository.CommunityInvitationRepository;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.community.service.impl.MembershipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MembershipServiceLifecycleTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private CommunityInvitationRepository invitationRepository;
    @Mock private MembershipMapper membershipMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AnalyticsService analyticsService;
    @Mock private CommunityRoleService roleService;
    @Mock private CommunityAuditLogService auditLogService;
    @Mock private CommunityEventOutboxService outboxService;
    @Mock private CommunitiesRateLimitConfig rateLimitConfig;

    @InjectMocks
    private MembershipServiceImpl membershipService;

    private UUID communityId;
    private Community community;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        community = Community.builder()
                .id(communityId)
                .name("Tech Pioneers")
                .visibility(Visibility.PUBLIC)
                .communityStatus(CommunityStatus.ACTIVE)
                .ownerId(1L)
                .memberCount(5)
                .build();
    }

    @Test
    @DisplayName("Join OPEN public community creates ACTIVE member")
    void joinOpenCommunity_createsActiveMembership() {
        when(rateLimitConfig.consumeJoin(2L)).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
        when(communityRepository.findByIdForUpdate(communityId)).thenReturn(Optional.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, 2L)).thenReturn(Optional.empty());
        when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> {
            Membership m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(membershipMapper.toDto(any(Membership.class))).thenReturn(MembershipDto.builder().userId(2L).role(MembershipRole.MEMBER).status(MembershipStatus.ACTIVE).build());

        MembershipDto dto = membershipService.joinCommunity(communityId, 2L, null);

        assertThat(dto.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        verify(outboxService).record(eq(communityId), eq("MEMBER_JOINED"), anyString(), anyMap());
    }

    @Test
    @DisplayName("Join INVITE_ONLY community without token throws InvalidJoinException")
    void joinInviteOnly_throwsException() {
        when(rateLimitConfig.consumeJoin(2L)).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
        community.setVisibility(Visibility.INVITE_ONLY);
        when(communityRepository.findByIdForUpdate(communityId)).thenReturn(Optional.of(community));

        assertThatThrownBy(() -> membershipService.joinCommunity(communityId, 2L, null))
                .isInstanceOf(InvalidJoinException.class);
    }

    @Test
    @DisplayName("Owner cannot leave community without transferring ownership")
    void ownerLeave_throwsException() {
        Membership ownerMembership = Membership.builder()
                .community(community)
                .userId(1L)
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, 1L)).thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> membershipService.leaveCommunity(communityId, 1L))
                .isInstanceOf(InvalidJoinException.class)
                .hasMessageContaining("transferring ownership");
    }

    @Test
    @DisplayName("Regular member leave changes status to LEFT")
    void memberLeave_setsStatusLeft() {
        Membership member = Membership.builder()
                .community(community)
                .userId(2L)
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, 2L)).thenReturn(Optional.of(member));

        membershipService.leaveCommunity(communityId, 2L);

        assertThat(member.getStatus()).isEqualTo(MembershipStatus.LEFT);
        verify(eventPublisher).publishEvent(any(MembershipLifecycleEvent.class));
        verify(outboxService).record(eq(communityId), eq("MEMBER_LEFT"), anyString(), anyMap());
    }

    @Test
    @DisplayName("Banning member updates status to BANNED")
    void banMember_updatesStatus() {
        Membership admin = Membership.builder().community(community).userId(1L).role(MembershipRole.ADMIN).status(MembershipStatus.ACTIVE).build();
        Membership target = Membership.builder().community(community).userId(2L).role(MembershipRole.MEMBER).status(MembershipStatus.ACTIVE).build();

        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, 1L)).thenReturn(Optional.of(admin));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, 2L)).thenReturn(Optional.of(target));
        when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> i.getArgument(0));
        when(membershipMapper.toDto(any(Membership.class))).thenReturn(MembershipDto.builder().userId(2L).status(MembershipStatus.BANNED).build());

        UpdateMembershipRequest req = new UpdateMembershipRequest();
        req.setStatus(MembershipStatus.BANNED);

        MembershipDto result = membershipService.updateMembership(communityId, 2L, req, 1L);

        assertThat(result.getStatus()).isEqualTo(MembershipStatus.BANNED);
        verify(outboxService).record(eq(communityId), eq("MEMBER_BANNED"), anyString(), anyMap());
    }

    @Test
    @DisplayName("Role change to higher rank without permission throws InsufficientRoleException")
    void roleEscalation_throwsException() {
        Membership mod = Membership.builder().community(community).userId(3L).role(MembershipRole.MODERATOR).status(MembershipStatus.ACTIVE).build();
        Membership target = Membership.builder().community(community).userId(2L).role(MembershipRole.MEMBER).status(MembershipStatus.ACTIVE).build();

        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, 3L)).thenReturn(Optional.of(mod));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, 2L)).thenReturn(Optional.of(target));

        UpdateMembershipRequest req = new UpdateMembershipRequest();
        req.setRole(MembershipRole.ADMIN);

        assertThatThrownBy(() -> membershipService.updateMembership(communityId, 2L, req, 3L))
                .isInstanceOf(InsufficientRoleException.class);
    }
}
