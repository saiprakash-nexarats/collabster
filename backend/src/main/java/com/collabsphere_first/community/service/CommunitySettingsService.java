package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.entity.CommunitySettings;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.repository.CommunitySettingsRepository;
import com.collabsphere_first.community.security.CommunityAccessService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunitySettingsService {

    private final CommunitySettingsRepository settingsRepository;
    private final CommunityAccessService accessService;
    private final CommunityAuditLogService auditLogService;
    private final CommunityEventOutboxService outboxService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public CommunitySettingsDto get(UUID communityId, Long userId) {

        accessService.requireMember(communityId, userId);

        CommunitySettings settings = settingsRepository
                .findById(communityId)
                .orElseGet(() -> defaults(communityId));

        return toDto(settings);
    }

    @Transactional
    public CommunitySettingsDto update(
            UUID communityId,
            Long userId,
            UpdateCommunitySettingsRequest request) {

        accessService.requireRole(
                communityId,
                userId,
                MembershipRole.ADMIN
        );

        CommunitySettings settings =
                settingsRepository.findById(communityId).orElse(null);

        /*
         * If settings do not exist yet, create them using persist().
         *
         * CommunitySettings uses @MapsId, so communityId is an
         * assigned/shared primary key. repository.save() can incorrectly
         * treat a newly-created instance as an existing entity and call
         * merge().
         */
        if (settings == null) {
            settings = defaults(communityId);
            entityManager.persist(settings);
        }

        if (request.getMemberPostsEnabled() != null) {
            settings.setMemberPostsEnabled(
                    request.getMemberPostsEnabled()
            );
        }

        if (request.getCommentsEnabled() != null) {
            settings.setCommentsEnabled(
                    request.getCommentsEnabled()
            );
        }

        if (request.getChannelsEnabled() != null) {
            settings.setChannelsEnabled(
                    request.getChannelsEnabled()
            );
        }

        if (request.getMessagingEnabled() != null) {
            settings.setMessagingEnabled(
                    request.getMessagingEnabled()
            );
        }

        if (request.getPollsEnabled() != null) {
            settings.setPollsEnabled(
                    request.getPollsEnabled()
            );
        }

        if (request.getEventsEnabled() != null) {
            settings.setEventsEnabled(
                    request.getEventsEnabled()
            );
        }

        if (request.getMemberInvitesEnabled() != null) {
            settings.setMemberInvitesEnabled(
                    request.getMemberInvitesEnabled()
            );
        }

        if (request.getAiModerationEnabled() != null) {
            settings.setAiModerationEnabled(
                    request.getAiModerationEnabled()
            );
        }

        if (request.getChannelCreationRole() != null) {
            settings.setChannelCreationRole(
                    requireManageableRole(
                            request.getChannelCreationRole()
                    )
            );
        }

        if (request.getMessageSendRole() != null) {
            settings.setMessageSendRole(
                    requireMemberOrHigher(
                            request.getMessageSendRole()
                    )
            );
        }

        if (request.getPollCreationRole() != null) {
            settings.setPollCreationRole(
                    requireMemberOrHigher(
                            request.getPollCreationRole()
                    )
            );
        }

        /*
         * If the entity already existed, it is managed by JPA and does
         * not actually require save(). For clarity, save only existing
         * entities.
         */
        if (entityManager.contains(settings)) {
            // Managed entity — changes will be flushed automatically.
        }

        auditLogService.record(
                settings.getCommunity(),
                userId,
                "SETTINGS_UPDATED",
                "SETTINGS",
                communityId.toString(),
                null
        );

        outboxService.record(
                communityId,
                "SETTINGS_UPDATED",
                communityId.toString(),
                java.util.Map.of("actorId", userId)
        );

        return toDto(settings);
    }

    @Transactional(readOnly = true)
    public CommunitySettings getOrDefault(UUID communityId) {

        return settingsRepository
                .findById(communityId)
                .orElseGet(() -> defaults(communityId));
    }

    private CommunitySettings defaults(UUID communityId) {

        return CommunitySettings.builder()
                .communityId(communityId)
                .community(
                        accessService.requireCommunity(communityId)
                )
                .build();
    }

    private MembershipRole requireManageableRole(
            MembershipRole role) {

        if (!role.hasAtLeastRank(MembershipRole.MODERATOR)) {

            throw new InsufficientRoleException(
                    "Channel creation must be restricted to moderators or higher"
            );
        }

        return role;
    }

    private MembershipRole requireMemberOrHigher(
            MembershipRole role) {

        if (!role.hasAtLeastRank(MembershipRole.MEMBER)) {

            throw new InsufficientRoleException(
                    "This action requires at least MEMBER rank"
            );
        }

        return role;
    }

    private CommunitySettingsDto toDto(
            CommunitySettings s) {

        return CommunitySettingsDto.builder()
                .communityId(s.getCommunityId())
                .memberPostsEnabled(s.isMemberPostsEnabled())
                .commentsEnabled(s.isCommentsEnabled())
                .channelsEnabled(s.isChannelsEnabled())
                .messagingEnabled(s.isMessagingEnabled())
                .pollsEnabled(s.isPollsEnabled())
                .eventsEnabled(s.isEventsEnabled())
                .memberInvitesEnabled(s.isMemberInvitesEnabled())
                .aiModerationEnabled(s.isAiModerationEnabled())
                .channelCreationRole(s.getChannelCreationRole())
                .messageSendRole(s.getMessageSendRole())
                .pollCreationRole(s.getPollCreationRole())
                .build();
    }
}