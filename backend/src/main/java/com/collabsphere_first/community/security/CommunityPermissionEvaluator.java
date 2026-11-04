package com.collabsphere_first.community.security;

import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class CommunityPermissionEvaluator implements PermissionEvaluator {

    private final MembershipRepository membershipRepository;
    private final CommunityRoleService communityRoleService;
    private final com.collabsphere_first.community.service.CommunityMembershipCacheService membershipCacheService;

    public CommunityPermissionEvaluator(
            MembershipRepository membershipRepository,
            CommunityRoleService communityRoleService) {
        this(membershipRepository, communityRoleService, null);
    }

    public CommunityPermissionEvaluator(
            MembershipRepository membershipRepository,
            CommunityRoleService communityRoleService,
            com.collabsphere_first.community.service.CommunityMembershipCacheService membershipCacheService) {
        this.membershipRepository = membershipRepository;
        this.communityRoleService = communityRoleService;
        this.membershipCacheService = membershipCacheService;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || targetDomainObject == null || !(permission instanceof String)) {
            return false;
        }

        UUID communityId;
        if (targetDomainObject instanceof UUID) {
            communityId = (UUID) targetDomainObject;
        } else {
            return false;
        }

        Long userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }

        return hasCommunityRole(communityId, userId, (String) permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || targetId == null || !(permission instanceof String)) {
            return false;
        }

        UUID communityId;
        if (targetId instanceof UUID) {
            communityId = (UUID) targetId;
        } else {
            try {
                communityId = UUID.fromString(targetId.toString());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        Long userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }

        return hasCommunityRole(communityId, userId, (String) permission);
    }

    public boolean hasCommunityRole(UUID communityId, Long userId, String requiredRoleStr) {
        // Platform Admin Override — uses Madhuri's ROLE_ADMIN check
        if (communityRoleService != null && communityRoleService.isPlatformAdmin(userId)) {
            log.debug("Platform Admin override granted for user {}", userId);
            return true;
        }

        if (membershipCacheService != null) {
            return membershipCacheService.hasPermission(communityId, userId, requiredRoleStr);
        }

        MembershipRole requiredRole;
        try {
            requiredRole = MembershipRole.valueOf(requiredRoleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid required role name: {}", requiredRoleStr);
            return false;
        }

        Optional<Membership> membershipOpt = membershipRepository.findByCommunityIdAndUserId(communityId, userId);
        if (membershipOpt.isEmpty()) {
            return false;
        }

        Membership membership = membershipOpt.get();
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            return false;
        }

        return membership.getRole().getRank() >= requiredRole.getRank();
    }

    /**
     * Extracts userId from Madhuri's JWT-populated UserPrincipal.
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUser().getId();
        }
        log.warn("Could not extract Long user ID from authentication principal type: {}", principal.getClass().getName());
        return null;
    }
}
