package com.collabsphere_first.community.security;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAccessMatrixAndIdorTest {

    @Mock private CommunityRepository communityRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private CommunityRoleService roleService;

    @InjectMocks
    private CommunityAccessService accessService;

    private UUID commAId;
    private UUID commBId;
    private Community commA;

    @BeforeEach
    void setUp() {
        commAId = UUID.randomUUID();
        commBId = UUID.randomUUID();

        commA = Community.builder().id(commAId).communityStatus(CommunityStatus.ACTIVE).visibility(Visibility.PUBLIC).build();
    }

    @ParameterizedTest
    @EnumSource(value = MembershipStatus.class, names = {"BANNED", "PENDING_APPROVAL", "LEFT", "REJECTED"})
    @DisplayName("Non-active/banned membership statuses are rejected on requireMember")
    void nonActiveStatuses_rejected(MembershipStatus status) {
        Long userId = 50L;
        Membership membership = Membership.builder().community(commA).userId(userId).status(status).role(MembershipRole.MEMBER).build();
        when(membershipRepository.findByCommunityIdAndUserId(commAId, userId)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> accessService.requireMember(commAId, userId))
                .isInstanceOf(InsufficientRoleException.class)
                .hasMessageContaining("Active community membership is required");
    }

    @Test
    @DisplayName("MUTED members can read but are blocked from writable actions")
    void mutedMember_blockedFromWriting() {
        Long userId = 51L;
        Membership muted = Membership.builder().community(commA).userId(userId).status(MembershipStatus.MUTED).role(MembershipRole.MEMBER).build();
        when(membershipRepository.findByCommunityIdAndUserId(commAId, userId)).thenReturn(Optional.of(muted));

        // Read succeeds
        assertThat(accessService.requireMember(commAId, userId)).isNotNull();

        // Write fails
        assertThatThrownBy(() -> accessService.requireWritableMember(commAId, userId))
                .isInstanceOf(InsufficientRoleException.class)
                .hasMessageContaining("restricted from creating content");
    }

    @Test
    @DisplayName("Archived community is strictly read-only for writes")
    void archivedCommunity_readOnlyForWrites() {
        Long userId = 52L;
        commA.setArchivedAt(Instant.now());
        Membership active = Membership.builder().community(commA).userId(userId).status(MembershipStatus.ACTIVE).role(MembershipRole.OWNER).build();
        when(membershipRepository.findByCommunityIdAndUserId(commAId, userId)).thenReturn(Optional.of(active));
        when(communityRepository.findById(commAId)).thenReturn(Optional.of(commA));

        assertThatThrownBy(() -> accessService.requireWritableMember(commAId, userId))
                .isInstanceOf(InsufficientRoleException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    @DisplayName("Role hierarchy enforced: MEMBER cannot perform MODERATOR or ADMIN actions")
    void roleHierarchy_memberCannotPerformAdminActions() {
        Long userId = 53L;
        Membership member = Membership.builder().community(commA).userId(userId).status(MembershipStatus.ACTIVE).role(MembershipRole.MEMBER).build();
        when(membershipRepository.findByCommunityIdAndUserId(commAId, userId)).thenReturn(Optional.of(member));
        when(communityRepository.findById(commAId)).thenReturn(Optional.of(commA));
        when(roleService.isPlatformAdmin(userId)).thenReturn(false);

        assertThatThrownBy(() -> accessService.requireRole(commAId, userId, MembershipRole.MODERATOR))
                .isInstanceOf(InsufficientRoleException.class);

        assertThatThrownBy(() -> accessService.requireRole(commAId, userId, MembershipRole.ADMIN))
                .isInstanceOf(InsufficientRoleException.class);
    }

    @Test
    @DisplayName("Cross-community access isolation / IDOR prevention: member of Community A cannot access Community B")
    void crossCommunityIsolation_accessDenied() {
        Long userInA = 54L;
        // User is member of Comm A, but queries Comm B
        when(membershipRepository.findByCommunityIdAndUserId(commBId, userInA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.requireMember(commBId, userInA))
                .isInstanceOf(InsufficientRoleException.class)
                .hasMessageContaining("Active community membership is required");
    }
}
