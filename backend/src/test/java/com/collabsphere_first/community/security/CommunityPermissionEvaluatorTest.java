package com.collabsphere_first.community.security;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.repository.MembershipRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommunityPermissionEvaluatorTest {
    @Test
    void permissionRequiresActiveMembershipAndSufficientRank() {
        MembershipRepository repository = mock(MembershipRepository.class);
        CommunityRoleService roleService = mock(CommunityRoleService.class);
        CommunityPermissionEvaluator evaluator = new CommunityPermissionEvaluator(repository, roleService);
        UUID communityId = UUID.randomUUID();
        when(roleService.isPlatformAdmin(55L)).thenReturn(false);
        Membership membership = Membership.builder().community(Community.builder().id(communityId).build())
                .userId(55L).role(MembershipRole.MEMBER).status(MembershipStatus.ACTIVE).build();
        when(repository.findByCommunityIdAndUserId(communityId, 55L)).thenReturn(Optional.of(membership));

        assertTrue(evaluator.hasCommunityRole(communityId, 55L, "FOLLOWER"));
        assertFalse(evaluator.hasCommunityRole(communityId, 55L, "MODERATOR"));

        membership.setStatus(MembershipStatus.BANNED);
        assertFalse(evaluator.hasCommunityRole(communityId, 55L, "MEMBER"));
    }
}
