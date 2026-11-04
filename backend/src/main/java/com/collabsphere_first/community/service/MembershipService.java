package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.MembershipDto;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.dto.UpdateMembershipRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MembershipService {
    MembershipDto joinCommunity(UUID communityId, Long userId, String inviteToken);
    void leaveCommunity(UUID communityId, Long userId);
    MembershipDto updateMembership(UUID communityId, Long targetUserId, UpdateMembershipRequest request, Long requestingUserId);
    Page<MembershipDto> listMembers(UUID communityId, Pageable pageable);
    Page<MembershipDto> listMembers(UUID communityId, MembershipStatus status, Pageable pageable);
    MembershipDto getMembership(UUID communityId, Long userId);
    Page<com.collabsphere_first.community.dto.MyCommunityDto> listMyCommunities(Long userId, Pageable pageable);
    MembershipDto activateFromInvitation(UUID communityId, Long userId);
    MembershipDto reviewJoinRequest(UUID communityId, Long userId, boolean approve, Long reviewerId);
    void transferOwnership(UUID communityId, Long targetUserId, Long requestingUserId);
}
