package com.collabsphere_first.community.service.impl;

import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.integration.storage.FirebaseStorageService;
import com.collabsphere_first.community.dto.CreateResourceRequest;
import com.collabsphere_first.community.dto.ResourceDto;
import com.collabsphere_first.community.exception.CommunityNotFoundException;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.exception.ResourceNotFoundException;
import com.collabsphere_first.community.mapper.ResourceMapper;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.entity.Resource;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.enums.ResourceType;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.repository.ResourceRepository;
import com.collabsphere_first.community.service.ModerationService;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.service.ResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class ResourceServiceImpl implements ResourceService {

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;
    private final FirebaseStorageService firebaseStorageService;
    private final ModerationService moderationService;
    private final CommunityRoleService roleService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final com.collabsphere_first.community.service.CommunityEventOutboxService outboxService;
    private final com.collabsphere_first.community.service.CommunitySettingsService settingsService;

    @org.springframework.beans.factory.annotation.Autowired
    public ResourceServiceImpl(
            CommunityRepository communityRepository,
            MembershipRepository membershipRepository,
            ResourceRepository resourceRepository,
            ResourceMapper resourceMapper,
            FirebaseStorageService firebaseStorageService,
            ModerationService moderationService,
            CommunityRoleService roleService,
            org.springframework.context.ApplicationEventPublisher eventPublisher,
            com.collabsphere_first.community.service.CommunityEventOutboxService outboxService,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            com.collabsphere_first.community.service.CommunitySettingsService settingsService
    ) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.resourceRepository = resourceRepository;
        this.resourceMapper = resourceMapper;
        this.firebaseStorageService = firebaseStorageService;
        this.moderationService = moderationService;
        this.roleService = roleService;
        this.eventPublisher = eventPublisher;
        this.outboxService = outboxService;
        this.settingsService = settingsService;
    }

    public ResourceServiceImpl(
            CommunityRepository communityRepository,
            MembershipRepository membershipRepository,
            ResourceRepository resourceRepository,
            ResourceMapper resourceMapper,
            FirebaseStorageService firebaseStorageService,
            ModerationService moderationService,
            CommunityRoleService roleService,
            org.springframework.context.ApplicationEventPublisher eventPublisher,
            com.collabsphere_first.community.service.CommunityEventOutboxService outboxService
    ) {
        this(communityRepository, membershipRepository, resourceRepository, resourceMapper,
                firebaseStorageService, moderationService, roleService, eventPublisher, outboxService, null);
    }

    @Override
    @Transactional
    public ResourceDto createResource(UUID communityId, CreateResourceRequest request, Long uploadedBy) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));
        if (community.getCommunityStatus() != CommunityStatus.ACTIVE) throw new InsufficientRoleException("Community is not accepting resources");

        validateActiveMember(communityId, uploadedBy);

        Resource resource = resourceMapper.toEntity(request);
        resource.setCommunity(community);
        resource.setUploadedBy(uploadedBy);
        resource.setModerationStatus(ModerationStatus.PENDING_REVIEW);

        if (request.getType() == ResourceType.FILE && request.getStorageKeyOrUrl() != null && firebaseStorageService != null) {
            String signedUrl = firebaseStorageService.generateSignedUrl(request.getStorageKeyOrUrl(), 120);
            resource.setStorageKeyOrUrl(signedUrl);
        }

        Resource saved = resourceRepository.save(resource);

        eventPublisher.publishEvent(new com.collabsphere_first.community.event.ResourceLifecycleEvent(
                this, saved.getId(), communityId, uploadedBy, uploadedBy,
                com.collabsphere_first.community.event.ResourceLifecycleEvent.Action.CREATED, saved.getTitle()));
        outboxService.record(communityId, "RESOURCE_CREATED", saved.getId().toString(),
                java.util.Map.of("resourceId", saved.getId(), "uploaderId", uploadedBy));

        if (settingsService == null || settingsService.getOrDefault(communityId).isAiModerationEnabled()) {
            String contentToModerate = saved.getTitle() + " " + (saved.getBody() != null ? saved.getBody() : "");
            moderationService.triggerAsyncResourceModeration(saved.getId(), contentToModerate);
        }

        return resourceMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceDto> getCommunityResources(UUID communityId, Long requestingUserId, Pageable pageable) {
        Community community = communityRepository.findById(communityId).orElseThrow(() -> new CommunityNotFoundException(communityId));
        if ((community.getVisibility() != Visibility.PUBLIC || community.getCommunityStatus() != CommunityStatus.ACTIVE)
                && membershipRepository.findByCommunityIdAndUserId(communityId, requestingUserId)
                    .filter(m -> m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.MUTED).isEmpty()) {
            throw new InsufficientRoleException("Community membership is required to view resources");
        }
        return resourceRepository.findVisibleOrAuthorResources(communityId, ModerationStatus.VISIBLE, requestingUserId, pageable)
                .map(resourceMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceDto getResource(UUID resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        return resourceMapper.toDto(resource);
    }

    private void validateActiveMember(UUID communityId, Long userId) {
        if (roleService != null && roleService.isPlatformAdmin(userId)) {
            return;
        }
        Membership membership = membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new InsufficientRoleException("Must be a member of the community to upload resources"));
        if (membership.getStatus() != MembershipStatus.ACTIVE || !membership.getRole().hasAtLeastRank(MembershipRole.MEMBER)) {
            throw new InsufficientRoleException("Active Member role or higher required to upload resources");
        }
    }
}

