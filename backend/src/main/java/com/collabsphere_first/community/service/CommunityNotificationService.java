package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CommunityNotificationDto;
import com.collabsphere_first.community.entity.CommunityNotification;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.NotificationPreference;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.repository.CommunityNotificationRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class CommunityNotificationService {
    private final CommunityNotificationRepository repository;
    private final MembershipRepository membershipRepository;

//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void create(Long userId, UUID communityId, String type, String title, String body, String targetType, String targetId) {
//        repository.save(CommunityNotification.builder().userId(userId).communityId(communityId).type(type)
//                .title(title).body(body).targetType(targetType).targetId(targetId).createdAt(Instant.now()).build());
//    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(
            Long userId,
            UUID communityId,
            String type,
            String title,
            String body,
            String targetType,
            String targetId
    ) {
        createInternal(
                userId,
                communityId,
                type,
                title,
                body,
                targetType,
                targetId,
                false
        );
    }
    /** Fan out a community activity notification while respecting member preferences. */
    @Transactional
    public void notifyActiveMembers(UUID communityId, Long excludedUserId, String type, String title,
                                    String body, String targetType, String targetId) {
        int page = 0;
        while (true) {
            Page<Membership> memberships = membershipRepository.findByCommunityIdAndStatus(
                    communityId, com.collabsphere_first.community.enums.MembershipStatus.ACTIVE,
                    org.springframework.data.domain.PageRequest.of(page, 500));
            memberships.getContent().stream()
                    .filter(member -> !member.getUserId().equals(excludedUserId))
                    .forEach(member -> createInternal(member.getUserId(), communityId, type, title, body,
                            targetType, targetId, false));
            if (!memberships.hasNext()) return;
            page++;
        }
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDirect(
            Long userId,
            UUID communityId,
            String type,
            String title,
            String body,
            String targetType,
            String targetId
    ) {
        createInternal(
                userId,
                communityId,
                type,
                title,
                body,
                targetType,
                targetId,
                true
        );
    }
    private void createInternal(
            Long userId,
            UUID communityId,
            String type,
            String title,
            String body,
            String targetType,
            String targetId,
            boolean directNotification
    ) {
        if (userId == null) {
            return;
        }

        Membership membership = communityId != null
                ? membershipRepository.findByCommunityIdAndUserId(communityId, userId).orElse(null)
                : null;

        if (membership != null) {
            // Do not send general chatter to banned or inactive users
            if (!directNotification && membership.getStatus() != com.collabsphere_first.community.enums.MembershipStatus.ACTIVE) {
                return;
            }
            if (!shouldNotify(membership.getNotificationPref(), directNotification)) {
                return;
            }
        } else if (!directNotification) {
            // Non-members only receive direct notifications (such as invitations)
            return;
        }

        repository.save(
                CommunityNotification.builder()
                        .userId(userId)
                        .communityId(communityId)
                        .type(type)
                        .title(title)
                        .body(body)
                        .targetType(targetType)
                        .targetId(targetId)
                        .createdAt(Instant.now())
                        .build()
        );
    }
    private boolean shouldNotify(
            NotificationPreference preference,
            boolean directNotification
    ) {
        return switch (preference) {
            case ALL -> true;
            case DIRECT_ONLY -> directNotification;
            case NONE -> false;
        };
    }
    @Transactional(readOnly = true)
    public Page<CommunityNotificationDto> list(Long userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDto);
    }
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) { return repository.countByUserIdAndReadAtIsNull(userId); }
    @Transactional
    public CommunityNotificationDto markRead(UUID id, Long userId) {
        CommunityNotification notification = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new InvalidJoinException("Notification not found"));
        if (notification.getReadAt() == null) notification.setReadAt(Instant.now());
        return toDto(repository.save(notification));
    }
    @Transactional
    public int markAllRead(Long userId) { return repository.markAllRead(userId, Instant.now()); }
    private CommunityNotificationDto toDto(CommunityNotification n) {
        return CommunityNotificationDto.builder().id(n.getId()).userId(n.getUserId()).communityId(n.getCommunityId())
                .type(n.getType()).title(n.getTitle()).body(n.getBody()).targetType(n.getTargetType())
                .targetId(n.getTargetId()).readAt(n.getReadAt()).createdAt(n.getCreatedAt()).build();
    }
}
