package com.collabsphere_first.community.service;

import com.collabsphere_first.community.config.CommunityMembershipSnapshot;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.repository.MembershipRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityMembershipCacheService {

    private final MembershipRepository membershipRepository;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "community-membership-snapshots",
            key = "#communityId.toString() + ':' + #userId.toString()",
            unless = "#result == null"
    )
    public CommunityMembershipSnapshot find(
            UUID communityId,
            Long userId
    ) {
        if (communityId == null || userId == null) {
            return null;
        }

        return membershipRepository.findByCommunityIdAndUserId(
                        communityId,
                        userId
                )
                .map(membership -> new CommunityMembershipSnapshot(
                        membership.getId(),
                        membership.getCommunity().getId(),
                        membership.getUserId(),
                        membership.getRole(),
                        membership.getStatus(),
                        membership.getNotificationPref()
                ))
                .orElse(null);
    }

    public boolean isActiveMember(UUID communityId, Long userId) {
        CommunityMembershipSnapshot membership = find(communityId, userId);

        return membership != null && membership.isActive();
    }

    public boolean hasAtLeastRole(
            UUID communityId,
            Long userId,
            MembershipRole requiredRole
    ) {
        CommunityMembershipSnapshot membership = find(communityId, userId);

        return membership != null
                && membership.hasAtLeastRole(requiredRole);
    }

    @Cacheable(
            value = "community-permissions",
            key = "#communityId.toString() + ':' + #userId.toString() + ':' + #requiredRoleStr.toUpperCase()",
            unless = "#result == null"
    )
    public boolean hasPermission(UUID communityId, Long userId, String requiredRoleStr) {
        if (communityId == null || userId == null || requiredRoleStr == null) {
            return false;
        }

        MembershipRole requiredRole;
        try {
            requiredRole = MembershipRole.valueOf(requiredRoleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }

        CommunityMembershipSnapshot membership = find(communityId, userId);
        return membership != null && membership.isActive() && membership.hasAtLeastRole(requiredRole);
    }

    public void evictUser(UUID communityId, Long userId) {
        if (communityId == null || userId == null) {
            return;
        }
        if (cacheManager != null) {
            Cache snapshotCache = cacheManager.getCache("community-membership-snapshots");
            if (snapshotCache != null) {
                snapshotCache.evict(communityId.toString() + ":" + userId.toString());
            }
            Cache permissionCache = cacheManager.getCache("community-permissions");
            if (permissionCache != null) {
                for (MembershipRole role : MembershipRole.values()) {
                    permissionCache.evict(communityId.toString() + ":" + userId.toString() + ":" + role.name());
                }
            }
        }
    }

    public void evictCommunity(UUID communityId) {
        if (communityId == null) {
            return;
        }
        if (cacheManager != null) {
            Cache snapshotCache = cacheManager.getCache("community-membership-snapshots");
            if (snapshotCache != null) {
                snapshotCache.clear();
            }
            Cache permissionCache = cacheManager.getCache("community-permissions");
            if (permissionCache != null) {
                permissionCache.clear();
            }
        }
    }

    @org.springframework.context.event.EventListener
    public void onMembershipLifecycle(com.collabsphere_first.community.event.MembershipLifecycleEvent event) {
        if (event != null && event.getCommunityId() != null && event.getUserId() != null) {
            evictUser(event.getCommunityId(), event.getUserId());
        }
    }
}
