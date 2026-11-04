package com.collabsphere_first.community.service;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.exception.CommunityNotFoundException;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.security.CommunityRoleService;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class CommunityLifecycleService {
    private final CommunityRepository communityRepository;
    private final CommunityRoleService roleService;
    private final CommunityAuditLogService auditLogService;
    private final CommunityEventOutboxService outboxService;
    private final CommunityMembershipCacheService cacheService;

    @org.springframework.beans.factory.annotation.Autowired
    public CommunityLifecycleService(
            CommunityRepository communityRepository,
            CommunityRoleService roleService,
            CommunityAuditLogService auditLogService,
            CommunityEventOutboxService outboxService,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            CommunityMembershipCacheService cacheService
    ) {
        this.communityRepository = communityRepository;
        this.roleService = roleService;
        this.auditLogService = auditLogService;
        this.outboxService = outboxService;
        this.cacheService = cacheService;
    }

    public CommunityLifecycleService(
            CommunityRepository communityRepository,
            CommunityRoleService roleService,
            CommunityAuditLogService auditLogService,
            CommunityEventOutboxService outboxService
    ) {
        this(communityRepository, roleService, auditLogService, outboxService, null);
    }

    @Transactional
    public void restore(UUID communityId, Long requesterId) {
        Community community = lockCommunity(communityId);
        requireOwnerOrPlatformAdmin(community, requesterId);
        if (community.getCommunityStatus() != CommunityStatus.ARCHIVED) throw new InvalidJoinException("Only archived communities can be restored");
        community.setCommunityStatus(CommunityStatus.ACTIVE);
        community.setArchivedAt(null);
        communityRepository.save(community);
        if (cacheService != null) cacheService.evictCommunity(communityId);
        auditLogService.record(community, requesterId, "COMMUNITY_RESTORED", "COMMUNITY", communityId.toString(), null);
        outboxService.record(communityId, "COMMUNITY_RESTORED", communityId.toString(), java.util.Map.of("actorId", requesterId));
    }

    @Transactional
    public void requestDeletion(UUID communityId, Long requesterId) {
        Community community = lockCommunity(communityId);
        requireOwnerOrPlatformAdmin(community, requesterId);
        if (community.getCommunityStatus() != CommunityStatus.ACTIVE && community.getCommunityStatus() != CommunityStatus.ARCHIVED) {
            throw new InvalidJoinException("Community is already pending deletion or inactive");
        }
        community.setCommunityStatus(CommunityStatus.DELETION_PENDING);
        community.setDeletionRequestedAt(Instant.now());
        communityRepository.save(community);
        if (cacheService != null) cacheService.evictCommunity(communityId);
        auditLogService.record(community, requesterId, "DELETION_SCHEDULED", "COMMUNITY", communityId.toString(), "recoveryDays=30");
        outboxService.record(communityId, "DELETION_SCHEDULED", communityId.toString(), java.util.Map.of("actorId", requesterId));
    }

    @Transactional
    public void cancelDeletion(UUID communityId, Long requesterId) {
        Community community = lockCommunity(communityId);
        requireOwnerOrPlatformAdmin(community, requesterId);
        if (community.getCommunityStatus() != CommunityStatus.DELETION_PENDING) throw new InvalidJoinException("Community has no pending deletion");
        community.setCommunityStatus(community.getArchivedAt() == null ? CommunityStatus.ACTIVE : CommunityStatus.ARCHIVED);
        community.setDeletionRequestedAt(null);
        communityRepository.save(community);
        if (cacheService != null) cacheService.evictCommunity(communityId);
        auditLogService.record(community, requesterId, "DELETION_CANCELLED", "COMMUNITY", communityId.toString(), null);
        outboxService.record(communityId, "DELETION_CANCELLED", communityId.toString(), java.util.Map.of("actorId", requesterId));
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void permanentlyDeleteExpiredCommunities() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        var expired = communityRepository.findByCommunityStatusAndDeletionRequestedAtBefore(
                CommunityStatus.DELETION_PENDING, cutoff, PageRequest.of(0, 100));
        for (Community community : expired.getContent()) {
            auditLogService.record(community, community.getOwnerId(), "COMMUNITY_DELETED", "COMMUNITY", community.getId().toString(), null);
            outboxService.record(community.getId(), "COMMUNITY_DELETED", community.getId().toString(), java.util.Map.of("ownerId", community.getOwnerId()));
            if (cacheService != null) cacheService.evictCommunity(community.getId());
            communityRepository.delete(community);
        }
    }

    private Community lockCommunity(UUID communityId) {
        return communityRepository.findByIdForUpdate(communityId).orElseThrow(() -> new CommunityNotFoundException(communityId));
    }

    private void requireOwnerOrPlatformAdmin(Community community, Long requesterId) {
        if (!community.getOwnerId().equals(requesterId) && !roleService.isPlatformAdmin(requesterId)) {
            throw new InsufficientRoleException("Only the community owner or platform administrator can change its lifecycle");
        }
    }
}
