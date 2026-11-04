package com.collabsphere_first.community.config;

import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.NotificationPreference;

import java.util.UUID;

public record CommunityMembershipSnapshot(
        UUID membershipId,
        UUID communityId,
        Long userId,
        MembershipRole role,
        MembershipStatus status,
        NotificationPreference notificationPreference
) {

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE
                || status == MembershipStatus.MUTED;
    }

    public boolean hasAtLeastRole(MembershipRole requiredRole) {
        return isActive() && role.hasAtLeastRank(requiredRole);
    }


}