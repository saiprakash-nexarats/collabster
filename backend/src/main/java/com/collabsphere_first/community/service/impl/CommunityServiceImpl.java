package com.collabsphere_first.community.service.impl;

import com.collabsphere_first.community.config.CommunityMembershipSnapshot;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.service.*;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.community.dto.CommunityDto;
import com.collabsphere_first.community.dto.CreateCommunityRequest;
import com.collabsphere_first.community.dto.UpdateCommunityRequest;
import com.collabsphere_first.community.event.CommunityCreatedEvent;
import com.collabsphere_first.community.event.MemberJoinedEvent;
import com.collabsphere_first.community.exception.CommunityNotFoundException;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.mapper.CommunityMapper;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.CommunityChannelRepository;
import com.collabsphere_first.community.entity.CommunityChannel;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import com.collabsphere_first.community.exception.CommunityRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final CommunityChannelRepository channelRepository;
    private final CommunityAuditLogService auditLogService;
    private final CommunityEventOutboxService outboxService;
    private final CommunityMapper communityMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalyticsService analyticsService;
    private final CommunityRoleService roleService;
    private final CommunitiesRateLimitConfig rateLimitConfig;
    private final CommunityMembershipCacheService membershipCacheService;

    @Override
    @Transactional
    public CommunityDto createCommunity(CreateCommunityRequest request, Long creatorUserId) {
        var limit = rateLimitConfig.consumeCommunityCreation(creatorUserId);
        if (!limit.allowed()) {
            throw new CommunityRateLimitException("Community creation rate limit exceeded", limit.retryAfterSeconds());
        }
        if (communityRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Community slug '" + request.getSlug() + "' is already taken");
        }

        Community community = communityMapper.toEntity(request);
        community.setOwnerId(creatorUserId);
        community.setMemberCount(1);
        if (request.getRequiresModeration() != null) {
            community.setRequiresModeration(request.getRequiresModeration());
        }

        Community savedCommunity = communityRepository.save(community);

        channelRepository.save(CommunityChannel.builder().community(savedCommunity).name("general")
                .description("Community-wide discussion").createdBy(creatorUserId).build());

        // Add creator as OWNER
        Membership ownerMembership = Membership.builder()
                .community(savedCommunity)
                .userId(creatorUserId)
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .contributionScore(100)
                .build();
        membershipRepository.save(ownerMembership);
        outboxService.record(savedCommunity.getId(), "COMMUNITY_CREATED", savedCommunity.getId().toString(),
                Map.of("ownerId", creatorUserId, "slug", savedCommunity.getSlug(), "name", savedCommunity.getName()));
        outboxService.record(savedCommunity.getId(), "MEMBER_JOINED", creatorUserId.toString(),
                Map.of("userId", creatorUserId, "role", MembershipRole.OWNER.name()));

        // Publish events & track metrics
        eventPublisher.publishEvent(new CommunityCreatedEvent(this, savedCommunity.getId(), savedCommunity.getName(), savedCommunity.getType(), creatorUserId));
        eventPublisher.publishEvent(new MemberJoinedEvent(this, savedCommunity.getId(), creatorUserId, MembershipRole.OWNER));

        if (analyticsService != null) {
            analyticsService.trackEvent("community_created", Map.of(
                    "community_id", savedCommunity.getId().toString(),
                    "owner_id", creatorUserId.toString(),
                    "type", savedCommunity.getType().name()
            ));
        }

        return communityMapper.toDto(savedCommunity);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityDto getCommunity(UUID communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));
        return communityMapper.toDto(community);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityDto getCommunity(UUID communityId, Long requestingUserId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));
        assertVisibleTo(community, requestingUserId);
        return communityMapper.toDto(community);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityDto getCommunityBySlug(String slug) {
        Community community = communityRepository.findBySlug(slug)
                .orElseThrow(() -> new CommunityNotFoundException("Community not found with slug: " + slug));
        return communityMapper.toDto(community);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityDto getCommunityBySlug(String slug, Long requestingUserId) {
        Community community = communityRepository.findBySlug(slug)
                .orElseThrow(() -> new CommunityNotFoundException("Community not found with slug: " + slug));
        assertVisibleTo(community, requestingUserId);
        return communityMapper.toDto(community);
    }

    @Override
    @Transactional
    public CommunityDto updateCommunity(UUID communityId, UpdateCommunityRequest request, Long requestingUserId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));

        validateAdminRole(communityId, requestingUserId);

        if (request.getName() != null) community.setName(request.getName());
        if (request.getDescription() != null) community.setDescription(request.getDescription());
        if (request.getLogoUrl() != null) community.setLogoUrl(request.getLogoUrl());
        if (request.getCoverUrl() != null) community.setCoverUrl(request.getCoverUrl());
        if (request.getVisibility() != null) community.setVisibility(request.getVisibility());
        if (request.getLifecycle() != null) community.setLifecycle(request.getLifecycle());
        if (request.getCategory() != null) community.setCategory(request.getCategory());
        if (request.getTags() != null) community.setTags(request.getTags());
        if (request.getRequiresModeration() != null) community.setRequiresModeration(request.getRequiresModeration());

        Community updated = communityRepository.save(community);
        auditLogService.record(community, requestingUserId, "COMMUNITY_UPDATED", "COMMUNITY", communityId.toString(), null);
        outboxService.record(communityId, "COMMUNITY_UPDATED", communityId.toString(), Map.of("actorId", requestingUserId));
        return communityMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void archiveCommunity(UUID communityId, Long requestingUserId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));

        boolean isOwner = community.getOwnerId().equals(requestingUserId);
        boolean isPlatformAdmin = roleService != null && roleService.isPlatformAdmin(requestingUserId);

        if (!isOwner && !isPlatformAdmin) {
            throw new InsufficientRoleException("Only the community Owner or Core Platform Admin can archive a community");
        }

        community.setArchivedAt(Instant.now());
        community.setCommunityStatus(CommunityStatus.ARCHIVED);
        communityRepository.save(community);

        if (analyticsService != null) {
            analyticsService.trackEvent("community_archived", Map.of(
                    "community_id", communityId.toString(),
                    "archived_by", requestingUserId.toString()
            ));
        }
        auditLogService.record(community, requestingUserId, "COMMUNITY_ARCHIVED", "COMMUNITY", communityId.toString(), null);
        outboxService.record(communityId, "COMMUNITY_ARCHIVED", communityId.toString(), Map.of("actorId", requestingUserId));
    }

    /**
     * Local outbox consumer hook. It is intentionally idempotent: creation has
     * already committed before the outbox delivery is attempted.
     */
    @Override
    @Transactional(readOnly = true)
    public void handleCommunityCreated(UUID eventId, UUID communityId, String name) {
        if (!communityRepository.existsById(communityId)) {
            throw new CommunityNotFoundException(communityId);
        }
        log.info("Delivered COMMUNITY_CREATED event {} for community '{}' ({})", eventId, name, communityId);
    }

    private void validateAdminRole(UUID communityId, Long userId) {
        if (roleService != null && roleService.isPlatformAdmin(userId)) {
            return;
        }

        CommunityMembershipSnapshot membership =
                membershipCacheService.find(communityId, userId);

        if (membership == null || !membership.isActive()) {
            throw new InsufficientRoleException(
                    "User is not an active member of this community"
            );
        }

        if (!membership.role().hasAtLeastRank(MembershipRole.ADMIN)) {
            throw new InsufficientRoleException(
                    "Requires ADMIN role or higher"
            );
        }
    }

    private void assertVisibleTo(Community community, Long userId) {
        boolean activeMember = userId != null
                && membershipCacheService.isActiveMember(
                community.getId(),
                userId
        );

        boolean isPublicActive =
                community.getVisibility() == Visibility.PUBLIC
                        && community.getArchivedAt() == null
                        && community.getCommunityStatus()
                        == CommunityStatus.ACTIVE;

        if (!isPublicActive && !activeMember) {
            throw new CommunityNotFoundException(community.getId());
        }
    }
}
