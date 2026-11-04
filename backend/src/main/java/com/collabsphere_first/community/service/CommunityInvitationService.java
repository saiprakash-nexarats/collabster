package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.InvitationDto;
import com.collabsphere_first.community.entity.CommunityInvitation;
import com.collabsphere_first.community.event.CommunityInvitationCreatedEvent;
import com.collabsphere_first.community.enums.InvitationStatus;
import com.collabsphere_first.community.exception.CommunityNotFoundException;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.repository.CommunityInvitationRepository;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import com.collabsphere_first.community.exception.CommunityRateLimitException;
import com.collabsphere_first.community.util.InvitationTokens;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityInvitationService {
    private final CommunityInvitationRepository invitationRepository;
    private final CommunityRepository communityRepository;
    private final MembershipService membershipService;
    private final MembershipRepository membershipRepository;
    private final CommunityRoleService roleService;
    private final UserRepository userRepository;
    private final CommunitySettingsService settingsService;
    private final ApplicationEventPublisher eventPublisher;
    private final CommunityEventOutboxService outboxService;
    private final CommunitiesRateLimitConfig rateLimitConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public InvitationDto create(UUID communityId, Long inviterId, Long inviteeId) {
        var limit = rateLimitConfig.consumeInvitation(inviterId);
        if (!limit.allowed()) throw new CommunityRateLimitException("Invitation rate limit exceeded", limit.retryAfterSeconds());
        var community = communityRepository.findById(communityId)
                .orElseThrow(() -> new CommunityNotFoundException(communityId));
        if (community.getArchivedAt() != null) throw new InvalidJoinException("Archived communities cannot issue invitations");
        if (inviterId.equals(inviteeId)) throw new InvalidJoinException("You cannot invite yourself");
        var inviter = membershipRepository.findByCommunityIdAndUserId(communityId, inviterId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new InsufficientRoleException("An active community membership is required to invite users"));
        if (!inviter.getRole().hasAtLeastRank(MembershipRole.ADMIN)
                && (!settingsService.getOrDefault(communityId).isMemberInvitesEnabled()
                    || !inviter.getRole().hasAtLeastRank(MembershipRole.MEMBER))) {
            throw new InsufficientRoleException("Community settings do not permit this role to invite members");
        }
        if (!userRepository.existsById(inviteeId)) throw new InvalidJoinException("Invitee does not exist");
        if (invitationRepository.existsByCommunityIdAndInviteeIdAndStatus(communityId, inviteeId, InvitationStatus.PENDING)) {
            throw new InvalidJoinException("A pending invitation already exists for this user");
        }
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        CommunityInvitation invitation = CommunityInvitation.builder()
                .community(community)
                .inviterId(inviterId)
                .inviteeId(inviteeId)
                .tokenHash(InvitationTokens.sha256(token))
                .status(InvitationStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        CommunityInvitation saved = invitationRepository.save(invitation);
        eventPublisher.publishEvent(new CommunityInvitationCreatedEvent(this, saved.getId(), communityId, inviteeId));
        outboxService.record(communityId, "INVITATION_CREATED", saved.getId().toString(),
                java.util.Map.of("invitationId", saved.getId(), "inviterId", inviterId, "inviteeId", inviteeId));
        return toDto(saved, token);
    }

    @Transactional(readOnly = true)
    public Page<InvitationDto> listForCommunity(UUID communityId, InvitationStatus status, Pageable pageable) {
        return invitationRepository.findByCommunityIdAndStatus(communityId, status, pageable).map(i -> toDto(i, null));
    }

    @Transactional(readOnly = true)
    public Page<InvitationDto> listForUser(Long userId, Pageable pageable) {
        return invitationRepository.findByInviteeIdAndStatus(userId, InvitationStatus.PENDING, pageable).map(i -> toDto(i, null));
    }

    @Transactional
    public InvitationDto accept(UUID invitationId, Long userId) {
        CommunityInvitation invitation = find(invitationId);
        validateRecipient(invitation, userId);
        validatePending(invitation);
        membershipService.activateFromInvitation(invitation.getCommunity().getId(), userId);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        CommunityInvitation saved = invitationRepository.save(invitation);
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.InvitationLifecycleEvent(
                this, saved.getId(), saved.getCommunity().getId(), saved.getInviterId(), userId,
                com.collabsphere_first.community.event.InvitationLifecycleEvent.Type.INVITATION_ACCEPTED));
        outboxService.record(saved.getCommunity().getId(), "INVITATION_ACCEPTED", saved.getId().toString(),
                java.util.Map.of("invitationId", saved.getId(), "inviterId", saved.getInviterId(), "inviteeId", userId));
        return toDto(saved, null);
    }

    @Transactional
    public InvitationDto decline(UUID invitationId, Long userId) {
        CommunityInvitation invitation = find(invitationId);
        validateRecipient(invitation, userId);
        validatePending(invitation);
        invitation.setStatus(InvitationStatus.DECLINED);
        CommunityInvitation saved = invitationRepository.save(invitation);
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.InvitationLifecycleEvent(
                this, saved.getId(), saved.getCommunity().getId(), saved.getInviterId(), userId,
                com.collabsphere_first.community.event.InvitationLifecycleEvent.Type.INVITATION_DECLINED));
        outboxService.record(saved.getCommunity().getId(), "INVITATION_DECLINED", saved.getId().toString(),
                java.util.Map.of("invitationId", saved.getId(), "inviterId", saved.getInviterId(), "inviteeId", userId));
        return toDto(saved, null);
    }

    @Transactional
    public void revoke(UUID invitationId, Long requesterId) {
        CommunityInvitation invitation = find(invitationId);
        boolean isCommunityAdmin = membershipRepository.findByCommunityIdAndUserId(invitation.getCommunity().getId(), requesterId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE && m.getRole().hasAtLeastRank(MembershipRole.ADMIN))
                .isPresent();
        boolean isPlatformAdmin = roleService.isPlatformAdmin(requesterId);
        if (!isCommunityAdmin && !isPlatformAdmin && !invitation.getInviterId().equals(requesterId)) {
            throw new InsufficientRoleException("Only the inviter or a community administrator can revoke this invitation");
        }
        validatePending(invitation);
        invitation.setStatus(InvitationStatus.REVOKED);
        invitation.setRevokedAt(Instant.now());
        CommunityInvitation saved = invitationRepository.save(invitation);
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.InvitationLifecycleEvent(
                this, saved.getId(), saved.getCommunity().getId(), saved.getInviterId(), saved.getInviteeId(),
                com.collabsphere_first.community.event.InvitationLifecycleEvent.Type.INVITATION_REVOKED));
        outboxService.record(saved.getCommunity().getId(), "INVITATION_REVOKED", saved.getId().toString(),
                java.util.Map.of("invitationId", saved.getId(), "inviterId", saved.getInviterId(), "inviteeId", saved.getInviteeId()));
    }

    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void expireInvitations() {
        invitationRepository.expirePendingBefore(Instant.now(), InvitationStatus.PENDING, InvitationStatus.EXPIRED);
    }

    private CommunityInvitation find(UUID id) {
        return invitationRepository.findById(id).orElseThrow(() -> new InvalidJoinException("Invitation not found"));
    }

    private void validateRecipient(CommunityInvitation invitation, Long userId) {
        if (!invitation.getInviteeId().equals(userId)) throw new InvalidJoinException("Invitation is not addressed to this user");
    }

    private void validatePending(CommunityInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) throw new InvalidJoinException("Invitation is no longer pending");
        if (!invitation.getExpiresAt().isAfter(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new InvalidJoinException("Invitation has expired");
        }
    }

    private InvitationDto toDto(CommunityInvitation invitation, String rawToken) {
        return InvitationDto.builder()
                .id(invitation.getId())
                .communityId(invitation.getCommunity().getId())
                .communityName(invitation.getCommunity().getName())
                .inviterId(invitation.getInviterId())
                .inviteeId(invitation.getInviteeId())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .acceptedAt(invitation.getAcceptedAt())
                .revokedAt(invitation.getRevokedAt())
                .token(rawToken)
                .build();
    }
}
