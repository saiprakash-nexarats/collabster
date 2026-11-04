package com.collabsphere_first.community.security;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.exception.CommunityNotFoundException;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Central read/write membership and role checks for community-owned resources. */
@Service
@RequiredArgsConstructor
public class CommunityAccessService {
    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final CommunityRoleService roleService;

    public Membership requireMember(UUID communityId, Long userId) {
        return membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.MUTED)
                .orElseThrow(() -> new InsufficientRoleException("Active community membership is required"));
    }

    public Membership requireWritableMember(UUID communityId, Long userId) {
        Membership membership = requireMember(communityId, userId);
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new InsufficientRoleException("This membership is restricted from creating content");
        }
        Community community = requireCommunity(communityId);
        if (community.getArchivedAt() != null || community.getCommunityStatus() != CommunityStatus.ACTIVE) {
            throw new InsufficientRoleException("Archived or inactive communities are read-only");
        }
        return membership;
    }

    public Membership requireRole(UUID communityId, Long userId, MembershipRole role) {
        if (roleService.isPlatformAdmin(userId)) return null;
        Membership membership = requireWritableMember(communityId, userId);
        if (!membership.getRole().hasAtLeastRank(role)) throw new InsufficientRoleException("Requires " + role + " role or higher");
        return membership;
    }

    public Community requireCommunity(UUID communityId) {
        return communityRepository.findById(communityId).orElseThrow(() -> new CommunityNotFoundException(communityId));
    }
}
