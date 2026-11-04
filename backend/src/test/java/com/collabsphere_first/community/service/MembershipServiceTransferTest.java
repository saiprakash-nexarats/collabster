package com.collabsphere_first.community.service;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.mapper.MembershipMapper;
import com.collabsphere_first.community.repository.CommunityInvitationRepository;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.community.service.impl.MembershipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipServiceTransferTest {
    private CommunityRepository communityRepository;
    private MembershipRepository membershipRepository;
    private CommunityAuditLogService auditLogService;
    private Community community;
    private Membership previousOwner;
    private Membership target;
    private UUID communityId;

    @BeforeEach
    void setUp() {
        communityRepository = mock(CommunityRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        auditLogService = mock(CommunityAuditLogService.class);
        communityId = UUID.randomUUID();
        community = Community.builder().id(communityId).ownerId(1L).build();
        previousOwner = Membership.builder().community(community).userId(1L).role(MembershipRole.OWNER).status(MembershipStatus.ACTIVE).build();
        target = Membership.builder().community(community).userId(2L).role(MembershipRole.MEMBER).status(MembershipStatus.ACTIVE).build();
        when(communityRepository.findByIdForUpdate(communityId)).thenReturn(Optional.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, 1L)).thenReturn(Optional.of(previousOwner));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, 2L)).thenReturn(Optional.of(target));
    }

    @Test
    void transferAtomicallyChangesOwnerRoleAndWritesAuditRecord() {
        MembershipServiceImpl service = new MembershipServiceImpl(communityRepository, membershipRepository,
                mock(CommunityInvitationRepository.class), auditLogService, mock(com.collabsphere_first.community.service.CommunityEventOutboxService.class), mock(MembershipMapper.class),
                mock(ApplicationEventPublisher.class), mock(AnalyticsService.class), mock(CommunityRoleService.class),
                mock(com.collabsphere_first.community.config.CommunitiesRateLimitConfig.class));

        service.transferOwnership(communityId, 2L, 1L);

        assertEquals(2L, community.getOwnerId());
        assertEquals(MembershipRole.ADMIN, previousOwner.getRole());
        assertEquals(MembershipRole.OWNER, target.getRole());
        verify(membershipRepository).save(target);
        verify(membershipRepository).save(previousOwner);
        verify(communityRepository).save(community);
        verify(auditLogService).record(community, 1L, "OWNERSHIP_TRANSFERRED", "USER", "2", "previousOwnerId=1");
    }

    @Test
    void rejectsInactiveTransferTarget() {
        target.setStatus(MembershipStatus.BANNED);
        MembershipServiceImpl service = new MembershipServiceImpl(communityRepository, membershipRepository,
                mock(CommunityInvitationRepository.class), auditLogService, mock(com.collabsphere_first.community.service.CommunityEventOutboxService.class), mock(MembershipMapper.class),
                mock(ApplicationEventPublisher.class), mock(AnalyticsService.class), mock(CommunityRoleService.class),
                mock(com.collabsphere_first.community.config.CommunitiesRateLimitConfig.class));

        assertThrows(RuntimeException.class, () -> service.transferOwnership(communityId, 2L, 1L));
        assertEquals(1L, community.getOwnerId());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any());
    }
}
