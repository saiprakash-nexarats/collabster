package com.collabsphere_first.community.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MembershipRoleTest {
    @Test
    void preservesTheCommunityRoleHierarchy() {
        assertTrue(MembershipRole.OWNER.hasAtLeastRank(MembershipRole.ADMIN));
        assertTrue(MembershipRole.ADMIN.hasAtLeastRank(MembershipRole.MODERATOR));
        assertTrue(MembershipRole.MODERATOR.hasAtLeastRank(MembershipRole.MEMBER));
        assertFalse(MembershipRole.FOLLOWER.hasAtLeastRank(MembershipRole.MEMBER));
    }

    @Test
    void membershipStateIsIndependentFromRole() {
        assertNotNull(MembershipStatus.LEFT);
        assertNotNull(MembershipStatus.REJECTED);
        assertNotNull(MembershipStatus.SUSPENDED);
        assertEquals(MembershipRole.MEMBER, MembershipRole.valueOf("MEMBER"));
    }
}
