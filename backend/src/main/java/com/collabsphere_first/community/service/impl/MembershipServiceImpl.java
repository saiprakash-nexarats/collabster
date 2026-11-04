package com.collabsphere_first.community.service.impl;

import com.collabsphere_first.community.enums.*;
import com.collabsphere_first.community.service.AnalyticsService;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.community.dto.MembershipDto;
import com.collabsphere_first.community.dto.MyCommunityDto;
import com.collabsphere_first.community.dto.UpdateMembershipRequest;
import com.collabsphere_first.community.entity.CommunityInvitation;
import com.collabsphere_first.community.event.MemberJoinedEvent;
import com.collabsphere_first.community.exception.CommunityNotFoundException;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.exception.CommunityRateLimitException;
import com.collabsphere_first.community.exception.MembershipNotFoundException;
import com.collabsphere_first.community.mapper.MembershipMapper;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.CommunityInvitationRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.service.MembershipService;
import com.collabsphere_first.community.service.CommunityAuditLogService;
import com.collabsphere_first.community.service.CommunityEventOutboxService;
import com.collabsphere_first.community.util.InvitationTokens;
import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class MembershipServiceImpl implements MembershipService {

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final CommunityInvitationRepository invitationRepository;
    private final CommunityAuditLogService auditLogService;
    private final CommunityEventOutboxService outboxService;
    private final MembershipMapper membershipMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalyticsService analyticsService;
    private final CommunityRoleService roleService;
    private final CommunitiesRateLimitConfig rateLimitConfig;
    private final com.collabsphere_first.community.service.CommunityMembershipCacheService membershipCacheService;

    @org.springframework.beans.factory.annotation.Autowired
    public MembershipServiceImpl(
            CommunityRepository communityRepository,
            MembershipRepository membershipRepository,
            CommunityInvitationRepository invitationRepository,
            CommunityAuditLogService auditLogService,
            CommunityEventOutboxService outboxService,
            MembershipMapper membershipMapper,
            ApplicationEventPublisher eventPublisher,
            AnalyticsService analyticsService,
            CommunityRoleService roleService,
            CommunitiesRateLimitConfig rateLimitConfig,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            com.collabsphere_first.community.service.CommunityMembershipCacheService membershipCacheService
    ) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.auditLogService = auditLogService;
        this.outboxService = outboxService;
        this.membershipMapper = membershipMapper;
        this.eventPublisher = eventPublisher;
        this.analyticsService = analyticsService;
        this.roleService = roleService;
        this.rateLimitConfig = rateLimitConfig;
        this.membershipCacheService = membershipCacheService;
    }

    public MembershipServiceImpl(
            CommunityRepository communityRepository,
            MembershipRepository membershipRepository,
            CommunityInvitationRepository invitationRepository,
            CommunityAuditLogService auditLogService,
            CommunityEventOutboxService outboxService,
            MembershipMapper membershipMapper,
            ApplicationEventPublisher eventPublisher,
            AnalyticsService analyticsService,
            CommunityRoleService roleService,
            CommunitiesRateLimitConfig rateLimitConfig
    ) {
        this(communityRepository, membershipRepository, invitationRepository, auditLogService,
                outboxService, membershipMapper, eventPublisher, analyticsService, roleService,
                rateLimitConfig, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "community-membership-snapshots", key = "#communityId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "community-permissions", allEntries = true)
    })
    public MembershipDto joinCommunity(UUID communityId, Long userId, String inviteToken) {
        if (membershipCacheService != null) {
            membershipCacheService.evictUser(communityId, userId);
        }
        var limit = rateLimitConfig.consumeJoin(userId);
        if (!limit.allowed()) throw new CommunityRateLimitException("Join rate limit exceeded", limit.retryAfterSeconds());
        Community community = communityRepository.findByIdForUpdate(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));

        if (community.getArchivedAt() != null || community.getCommunityStatus() != CommunityStatus.ACTIVE) {
            throw new InvalidJoinException("Archived communities are not accepting membership changes");
        }

        Optional<Membership> existing = membershipRepository.findByCommunityIdAndUserId(communityId, userId);
        boolean wasActive = existing.map(m -> isActive(m.getStatus())).orElse(false);
        if (existing.isPresent()) {
            Membership m = existing.get();
            if (isActive(m.getStatus())) {
                throw new InvalidJoinException("User is already an active member of this community");
            } else if (m.getStatus() == MembershipStatus.BANNED || m.getStatus() == MembershipStatus.SUSPENDED) {
                throw new InvalidJoinException("User is not allowed to join this community");
            } else if (m.getStatus() == MembershipStatus.PENDING_APPROVAL) {
                throw new InvalidJoinException("Membership request is already pending");
            }
        }

        MembershipStatus status;
        MembershipRole role = MembershipRole.MEMBER;

        if (community.getVisibility() == Visibility.PUBLIC) {
            status = MembershipStatus.ACTIVE;
        } else if (community.getVisibility() == Visibility.PRIVATE) {
            status = MembershipStatus.PENDING_APPROVAL;
        } else if (community.getVisibility() == Visibility.INVITE_ONLY) {
            if (inviteToken == null || inviteToken.isBlank()) {
                throw new InvalidJoinException("Invite token required for INVITE_ONLY community");
            }
            CommunityInvitation invitation = invitationRepository.findByTokenHash(InvitationTokens.sha256(inviteToken))
                    .orElseThrow(() -> new InvalidJoinException("Invitation token is invalid"));
            validateInvitation(invitation, communityId, userId);
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setAcceptedAt(Instant.now());
            invitationRepository.save(invitation);
            status = MembershipStatus.ACTIVE;
        } else {
            status = MembershipStatus.ACTIVE;
        }

        Membership membership;
        if (existing.isPresent()) {
            membership = existing.get();
            membership.setStatus(status);
        } else {
            membership = Membership.builder()
                    .community(community)
                    .userId(userId)
                    .role(role)
                    .status(status)
                    .build();
        }

        Membership saved = membershipRepository.save(membership);

        if (status == MembershipStatus.ACTIVE && !wasActive) {
            community.setMemberCount(community.getMemberCount() + 1);
            communityRepository.save(community);
            eventPublisher.publishEvent(new MemberJoinedEvent(this, communityId, userId, role));
            outboxService.record(communityId, "MEMBER_JOINED", userId.toString(), Map.of("userId", userId, "role", role.name()));

            if (analyticsService != null) {
                analyticsService.trackEvent("member_joined", Map.of(
                        "community_id", communityId.toString(),
                        "user_id", userId.toString(),
                        "role", role.name()
                ));
            }
        } else if (status == MembershipStatus.PENDING_APPROVAL) {
            eventPublisher.publishEvent(new com.collabsphere_first.community.event.MembershipLifecycleEvent(
                    this, communityId, userId, userId,
                    com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.JOIN_REQUESTED,
                    role, role, null, status
            ));
            outboxService.record(communityId, "JOIN_REQUESTED", userId.toString(), Map.of("userId", userId));
        }

        return membershipMapper.toDto(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "community-membership-snapshots", key = "#communityId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "community-permissions", allEntries = true)
    })
    public void leaveCommunity(UUID communityId, Long userId) {
        if (membershipCacheService != null) {
            membershipCacheService.evictUser(communityId, userId);
        }
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));

        if (community.getOwnerId().equals(userId)) {
            throw new InvalidJoinException("Owner cannot leave the community without transferring ownership first");
        }

        Membership membership = membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new MembershipNotFoundException(communityId, null));

        boolean counted = isActive(membership.getStatus());
        membership.setStatus(MembershipStatus.LEFT);
        membershipRepository.save(membership);

        if (counted && community.getMemberCount() > 0) {
            community.setMemberCount(community.getMemberCount() - 1);
            communityRepository.save(community);
        }

        eventPublisher.publishEvent(new com.collabsphere_first.community.event.MembershipLifecycleEvent(
                this, communityId, userId, userId,
                com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.MEMBER_LEFT,
                membership.getRole(), membership.getRole(), membership.getStatus(), MembershipStatus.LEFT
        ));
        outboxService.record(communityId, "MEMBER_LEFT", userId.toString(), Map.of("userId", userId));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "community-membership-snapshots", key = "#communityId.toString() + ':' + #targetUserId.toString()"),
            @CacheEvict(value = "community-permissions", allEntries = true)
    })
    public MembershipDto updateMembership(UUID communityId, Long targetUserId, UpdateMembershipRequest request, Long requestingUserId) {
        if (membershipCacheService != null) {
            membershipCacheService.evictUser(communityId, targetUserId);
        }
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));

        Membership targetMembership = membershipRepository.findByCommunityIdAndUserId(communityId, targetUserId)
                .orElseThrow(() -> new MembershipNotFoundException(communityId, null));

        boolean isPlatformAdmin = roleService != null && roleService.isPlatformAdmin(requestingUserId);
        Membership requestingMembership = membershipRepository.findByCommunityIdAndUserId(communityId, requestingUserId)
                .orElse(null);

        if (!isPlatformAdmin) {
            if (requestingMembership == null || requestingMembership.getStatus() != MembershipStatus.ACTIVE || !requestingMembership.getRole().hasAtLeastRank(MembershipRole.ADMIN)) {
                throw new InsufficientRoleException("Only Admins, Owners, or Platform Admins can update membership roles/status");
            }
            if (targetMembership.getRole().getRank() >= requestingMembership.getRole().getRank()) {
                throw new InsufficientRoleException("Cannot modify a member with equal or higher role rank");
            }
            if (request.getRole() == MembershipRole.OWNER && requestingMembership.getRole() != MembershipRole.OWNER) {
                throw new InsufficientRoleException("Only an Owner can promote another user to OWNER");
            }
        }

        MembershipRole oldRole = targetMembership.getRole();
        MembershipStatus oldStatus = targetMembership.getStatus();

        if (request.getRole() != null) {
            if (request.getRole() == MembershipRole.OWNER) {
                transferOwnership(communityId, targetUserId, requestingUserId);
                return membershipMapper.toDto(membershipRepository.findByCommunityIdAndUserId(communityId, targetUserId).orElseThrow());
            } else if (community.getOwnerId().equals(targetUserId)) {
                throw new InvalidJoinException("Cannot demote the community owner. Transfer ownership to another member first.");
            }
            targetMembership.setRole(request.getRole());
        }

        if (request.getStatus() != null) {
            if (community.getOwnerId().equals(targetUserId) && request.getStatus() != MembershipStatus.ACTIVE) {
                throw new InvalidJoinException("Cannot deactivate or ban the community owner. Transfer ownership first.");
            }
            targetMembership.setStatus(request.getStatus());

            if (!isActive(oldStatus) && isActive(request.getStatus())) {
                community.setMemberCount(community.getMemberCount() + 1);
                communityRepository.save(community);
            } else if (isActive(oldStatus) && !isActive(request.getStatus())) {
                community.setMemberCount(Math.max(0, community.getMemberCount() - 1));
                communityRepository.save(community);
            }
        }

        if (request.getNotificationPref() != null) {
            targetMembership.setNotificationPref(NotificationPreference.valueOf(request.getNotificationPref()));
        }

        Membership updated = membershipRepository.save(targetMembership);
        auditLogService.record(community, requestingUserId, "MEMBERSHIP_UPDATED", "USER", targetUserId.toString(),
                "role=" + updated.getRole() + ";status=" + updated.getStatus());
        outboxService.record(communityId, "MEMBERSHIP_UPDATED", targetUserId.toString(),
                Map.of("actorId", requestingUserId, "role", updated.getRole().name(), "status", updated.getStatus().name()));

        if (oldRole != updated.getRole()) {
            eventPublisher.publishEvent(new com.collabsphere_first.community.event.MembershipLifecycleEvent(
                    this, communityId, targetUserId, requestingUserId,
                    com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.ROLE_CHANGED,
                    oldRole, updated.getRole(), oldStatus, updated.getStatus()
            ));
            outboxService.record(communityId, "ROLE_CHANGED", targetUserId.toString(),
                    Map.of("actorId", requestingUserId, "oldRole", oldRole.name(), "newRole", updated.getRole().name()));
        }

        if (oldStatus != updated.getStatus()) {
            com.collabsphere_first.community.event.MembershipLifecycleEvent.Type statusType = switch (updated.getStatus()) {
                case BANNED -> com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.MEMBER_BANNED;
                case MUTED -> com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.MEMBER_MUTED;
                case SUSPENDED -> com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.MEMBER_SUSPENDED;
                case ACTIVE -> oldStatus == MembershipStatus.BANNED
                        ? com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.MEMBER_UNBANNED
                        : (oldStatus == MembershipStatus.MUTED
                        ? com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.MEMBER_UNMUTED
                        : com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.JOIN_APPROVED);
                default -> null;
            };
            if (statusType != null) {
                eventPublisher.publishEvent(new com.collabsphere_first.community.event.MembershipLifecycleEvent(
                        this, communityId, targetUserId, requestingUserId,
                        statusType,
                        updated.getRole(), updated.getRole(), oldStatus, updated.getStatus()
                ));
                outboxService.record(communityId, statusType.name(), targetUserId.toString(),
                        Map.of("actorId", requestingUserId, "oldStatus", oldStatus.name(), "newStatus", updated.getStatus().name()));
            }
        }

        return membershipMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MembershipDto> listMembers(UUID communityId, Pageable pageable) {
        return membershipRepository.findByCommunityId(communityId, pageable)
                .map(membershipMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MembershipDto> listMembers(UUID communityId, MembershipStatus status, Pageable pageable) {
        return status == null ? listMembers(communityId, pageable)
                : membershipRepository.findByCommunityIdAndStatus(communityId, status, pageable).map(membershipMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipDto getMembership(UUID communityId, Long userId) {
        Membership membership = membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new MembershipNotFoundException(communityId, null));
        return membershipMapper.toDto(membership);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MyCommunityDto> listMyCommunities(Long userId, Pageable pageable) {
        return membershipRepository.findByUserIdAndStatusIn(userId,
                        java.util.List.of(MembershipStatus.ACTIVE, MembershipStatus.MUTED, MembershipStatus.PENDING_APPROVAL), pageable)
                .map(membership -> MyCommunityDto.builder()
                        .communityId(membership.getCommunity().getId())
                        .name(membership.getCommunity().getName())
                        .slug(membership.getCommunity().getSlug())
                        .description(membership.getCommunity().getDescription())
                        .visibility(membership.getCommunity().getVisibility())
                        .role(membership.getRole())
                        .membershipStatus(membership.getStatus())
                        .memberCount(membership.getCommunity().getMemberCount())
                        .activityScore(membership.getCommunity().getActivityScore())
                        .build());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "community-membership-snapshots", key = "#communityId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "community-permissions", allEntries = true)
    })
    public MembershipDto activateFromInvitation(UUID communityId, Long userId) {
        if (membershipCacheService != null) {
            membershipCacheService.evictUser(communityId, userId);
        }
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));
        if (community.getArchivedAt() != null || community.getCommunityStatus() != CommunityStatus.ACTIVE) throw new InvalidJoinException("Community is not accepting memberships");
        Membership membership = membershipRepository.findByCommunityIdAndUserId(communityId, userId).orElse(null);
        if (membership != null && (membership.getStatus() == MembershipStatus.BANNED || membership.getStatus() == MembershipStatus.SUSPENDED)) {
            throw new InvalidJoinException("User is not allowed to join this community");
        }
        if (membership == null) {
            membership = Membership.builder().community(community).userId(userId).role(MembershipRole.MEMBER).status(MembershipStatus.ACTIVE).build();
        } else if (isActive(membership.getStatus())) {
            throw new InvalidJoinException("User is already an active member of this community");
        } else {
            membership.setStatus(MembershipStatus.ACTIVE);
        }
        Membership saved = membershipRepository.save(membership);
        community.setMemberCount(community.getMemberCount() + 1);
        communityRepository.save(community);
        eventPublisher.publishEvent(new MemberJoinedEvent(this, communityId, userId, MembershipRole.MEMBER));
        outboxService.record(communityId, "MEMBER_JOINED", userId.toString(), Map.of("userId", userId, "role", MembershipRole.MEMBER.name()));
        return membershipMapper.toDto(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "community-membership-snapshots", key = "#communityId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "community-permissions", allEntries = true)
    })
    public MembershipDto reviewJoinRequest(UUID communityId, Long userId, boolean approve, Long reviewerId) {
        if (membershipCacheService != null) {
            membershipCacheService.evictUser(communityId, userId);
        }
        Membership reviewer = membershipRepository.findByCommunityIdAndUserId(communityId, reviewerId)
                .orElseThrow(() -> new InsufficientRoleException("Reviewer is not a community member"));
        if (reviewer.getStatus() != MembershipStatus.ACTIVE || !reviewer.getRole().hasAtLeastRank(MembershipRole.MODERATOR)) {
            throw new InsufficientRoleException("Only active moderators and administrators can review join requests");
        }
        Membership target = membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new MembershipNotFoundException("Membership not found for user " + userId + " in community " + communityId));
        if (target.getStatus() != MembershipStatus.PENDING_APPROVAL) throw new InvalidJoinException("Membership request is not pending");
        target.setStatus(approve ? MembershipStatus.ACTIVE : MembershipStatus.REJECTED);
        if (approve) {
            Community community = target.getCommunity();
            community.setMemberCount(community.getMemberCount() + 1);
            communityRepository.save(community);
            eventPublisher.publishEvent(new MemberJoinedEvent(this, communityId, userId, target.getRole()));
        }
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.MembershipLifecycleEvent(
                this, communityId, userId, reviewerId,
                approve ? com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.JOIN_APPROVED
                        : com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.JOIN_REJECTED,
                target.getRole(), target.getRole(), MembershipStatus.PENDING_APPROVAL, target.getStatus()
        ));
        auditLogService.record(target.getCommunity(), reviewerId, approve ? "MEMBER_APPROVED" : "MEMBER_REJECTED",
                "USER", userId.toString(), null);
        outboxService.record(communityId, approve ? "MEMBER_APPROVED" : "MEMBER_REJECTED", userId.toString(),
                Map.of("userId", userId, "reviewerId", reviewerId));
        return membershipMapper.toDto(membershipRepository.save(target));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "community-membership-snapshots", key = "#communityId.toString() + ':' + #targetUserId.toString()"),
            @CacheEvict(value = "community-membership-snapshots", key = "#communityId.toString() + ':' + #requestingUserId.toString()"),
            @CacheEvict(value = "community-permissions", allEntries = true)
    })
    public void transferOwnership(UUID communityId, Long targetUserId, Long requestingUserId) {
        if (membershipCacheService != null) {
            membershipCacheService.evictUser(communityId, targetUserId);
            membershipCacheService.evictUser(communityId, requestingUserId);
        }
        Community community = communityRepository.findByIdForUpdate(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));
        if (!community.getOwnerId().equals(requestingUserId)) throw new InsufficientRoleException("Only the community owner can transfer ownership");
        Membership target = membershipRepository.findByCommunityIdAndUserId(communityId, targetUserId)
                .orElseThrow(() -> new MembershipNotFoundException("Membership not found for user " + targetUserId + " in community " + communityId));
        if (target.getStatus() != MembershipStatus.ACTIVE) throw new InvalidJoinException("Ownership can only be transferred to an active member");
        if (targetUserId.equals(requestingUserId)) throw new InvalidJoinException("Target user is already the owner");
        Membership previousOwner = membershipRepository.findByCommunityIdAndUserId(communityId, requestingUserId)
                .orElseThrow(() -> new MembershipNotFoundException("Membership not found for user " + requestingUserId + " in community " + communityId));
        target.setRole(MembershipRole.OWNER);
        previousOwner.setRole(MembershipRole.ADMIN);
        membershipRepository.save(target);
        membershipRepository.save(previousOwner);
        community.setOwnerId(targetUserId);
        communityRepository.save(community);
        auditLogService.record(community, requestingUserId, "OWNERSHIP_TRANSFERRED", "USER", targetUserId.toString(),
                "previousOwnerId=" + requestingUserId);
        outboxService.record(communityId, "OWNERSHIP_TRANSFERRED", targetUserId.toString(),
                Map.of("previousOwnerId", requestingUserId, "newOwnerId", targetUserId));
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.MembershipLifecycleEvent(
                this, communityId, targetUserId, requestingUserId,
                com.collabsphere_first.community.event.MembershipLifecycleEvent.Type.OWNERSHIP_TRANSFERRED,
                MembershipRole.ADMIN, MembershipRole.OWNER, MembershipStatus.ACTIVE, MembershipStatus.ACTIVE
        ));
    }

    private void validateInvitation(CommunityInvitation invitation, UUID communityId, Long userId) {
        if (!invitation.getCommunity().getId().equals(communityId) || !invitation.getInviteeId().equals(userId)) {
            throw new InvalidJoinException("Invitation is not valid for this user or community");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) throw new InvalidJoinException("Invitation is no longer pending");
        if (!invitation.getExpiresAt().isAfter(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new InvalidJoinException("Invitation has expired");
        }
    }

    private boolean isActive(MembershipStatus status) {
        return status == MembershipStatus.ACTIVE || status == MembershipStatus.MUTED;
    }
}
