package com.collabsphere_first.notification.service;

import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.notification.config.NotificationProperties;
import com.collabsphere_first.notification.dto.*;
import com.collabsphere_first.notification.entity.Notification;
import com.collabsphere_first.notification.enums.NotificationCategory;
import com.collabsphere_first.notification.enums.NotificationChannel;
import com.collabsphere_first.notification.event.NotificationCreatedEvent;
import com.collabsphere_first.notification.exception.NotificationAccessDeniedException;
import com.collabsphere_first.notification.exception.NotificationNotFoundException;
import com.collabsphere_first.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService preferenceService;
    private final NotificationOutboxService outboxService;
    private final NetworkAccessService networkAccessService;
    private final NotificationProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationProperties getProperties() {
        return properties;
    }

    @Override
    @Transactional
    public Notification createNotification(CreateNotificationCommand command) {
        Long recipientUserId = command.getRecipientUserId();
        Long actorUserId = command.getActorUserId();

        // 1. Self-notification suppression (Section 11)
        if (actorUserId != null && actorUserId.equals(recipientUserId)) {
            log.debug("Suppressed self-notification: actor and recipient are user {}", recipientUserId);
            return null;
        }

        // 2. Block enforcement (Section 12)
        if (actorUserId != null && networkAccessService.isBlockedEitherDirection(actorUserId, recipientUserId)) {
            log.debug("Suppressed notification due to network block between actor {} and recipient {}",
                    actorUserId, recipientUserId);
            return null;
        }

        // 3. Event-key deduplication (Section 8)
        Optional<Notification> existing = notificationRepository.findByEventKey(command.getEventKey());
        if (existing.isPresent()) {
            log.debug("Notification already exists for event_key: {}", command.getEventKey());
            return existing.get();
        }

        NotificationCategory category = command.getType().getCategory();

        // 4. In-App preference check (Section 28-29)
        boolean inAppEnabled = preferenceService.isInAppEnabled(recipientUserId, category);
        if (!inAppEnabled && category != NotificationCategory.SECURITY) {
            log.debug("In-app notification suppressed by user preference for user {} and category {}",
                    recipientUserId, category);
            return null;
        }

        // 5. Persist notification row
        Notification notification = Notification.builder()
                .recipientUserId(recipientUserId)
                .actorUserId(actorUserId)
                .type(command.getType())
                .category(category)
                .sourceType(command.getSourceType())
                .sourceId(command.getSourceId())
                .contextType(command.getContextType())
                .contextId(command.getContextId())
                .eventKey(command.getEventKey())
                .titleKey(command.getTitleKey())
                .bodyKey(command.getBodyKey())
                .targetType(command.getTargetType())
                .targetId(command.getTargetId())
                .payload(command.getPayload())
                .createdAt(Instant.now())
                .build();

        Notification saved;
        try {
            saved = notificationRepository.save(notification);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert race condition handled cleanly
            return notificationRepository.findByEventKey(command.getEventKey())
                    .orElseThrow(() -> ex);
        }

        // 6. Enqueue durable outbox channels using centralized preference matrix
        Set<NotificationChannel> eligibleChannels = preferenceService.determineEligibleChannels(
                recipientUserId, category, command.isSuppressExternalChannels());
        for (NotificationChannel channel : eligibleChannels) {
            outboxService.enqueue(saved.getId(), channel);
        }

        // 7. Publish Spring application event for immediate reactive processing
        eventPublisher.publishEvent(new NotificationCreatedEvent(this, saved.getId(), recipientUserId));

        log.info("Created global notification (id={}, recipient={}, type={}, category={})",
                saved.getId(), recipientUserId, saved.getType(), saved.getCategory());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(Long userId, String cursor, int limit,
                                                     NotificationCategory category, String filter) {
        int effectiveLimit = Math.max(1, Math.min(limit <= 0 ? DEFAULT_LIMIT : limit, MAX_LIMIT));
        NotificationCursor decodedCursor = NotificationCursor.fromCursorString(cursor);

        PageRequest pageRequest = PageRequest.of(0, effectiveLimit + 1);
        List<Notification> rows;

        boolean unreadOnly = "UNREAD".equalsIgnoreCase(filter);

        if (unreadOnly) {
            if (decodedCursor == null) {
                rows = notificationRepository.findInitialUnreadByRecipient(userId, pageRequest);
            } else {
                rows = notificationRepository.findAfterCursorUnreadByRecipient(
                        userId, decodedCursor.getCreatedAt(), decodedCursor.getId(), pageRequest);
            }
        } else if (category != null) {
            if (decodedCursor == null) {
                rows = notificationRepository.findInitialByRecipientAndCategory(userId, category, pageRequest);
            } else {
                rows = notificationRepository.findAfterCursorByRecipientAndCategory(
                        userId, category, decodedCursor.getCreatedAt(), decodedCursor.getId(), pageRequest);
            }
        } else {
            if (decodedCursor == null) {
                rows = notificationRepository.findInitialByRecipient(userId, pageRequest);
            } else {
                rows = notificationRepository.findAfterCursorByRecipient(
                        userId, decodedCursor.getCreatedAt(), decodedCursor.getId(), pageRequest);
            }
        }

        boolean hasMore = rows.size() > effectiveLimit;
        List<Notification> pagedList = hasMore ? rows.subList(0, effectiveLimit) : rows;

        String nextCursor = null;
        if (hasMore && !pagedList.isEmpty()) {
            Notification last = pagedList.get(pagedList.size() - 1);
            nextCursor = new NotificationCursor(last.getCreatedAt(), last.getId()).toCursorString();
        }

        long unreadCount = notificationRepository.countUnreadByRecipient(userId);

        List<NotificationResponse> items = pagedList.stream()
                .map(this::mapToResponse)
                .toList();

        return NotificationPageResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(UUID id, Long currentUserId) {
        Notification notification = findAndAuthorize(id, currentUserId);
        return mapToResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse markSeen(UUID id, Long currentUserId) {
        Notification notification = findAndAuthorize(id, currentUserId);
        if (notification.markSeen(Instant.now())) {
            notification = notificationRepository.save(notification);
        }
        return mapToResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(UUID id, Long currentUserId) {
        Notification notification = findAndAuthorize(id, currentUserId);
        if (notification.markRead(Instant.now())) {
            notification = notificationRepository.save(notification);
        }
        return mapToResponse(notification);
    }

    @Override
    @Transactional
    public int markAllRead(Long currentUserId, String readUpToCursor, Instant upToTimestamp) {
        Instant now = Instant.now();
        int updated;

        if (readUpToCursor != null && !readUpToCursor.isBlank()) {
            NotificationCursor cursor = NotificationCursor.fromCursorString(readUpToCursor);
            updated = notificationRepository.markAllReadUpToTuple(
                    currentUserId, cursor.getCreatedAt(), cursor.getId(), now);
            log.info("Marked all notifications read up to cursor ({}, {}) for user {} (updated={})",
                    cursor.getCreatedAt(), cursor.getId(), currentUserId, updated);
        } else if (upToTimestamp != null) {
            updated = notificationRepository.markAllReadUpTo(currentUserId, upToTimestamp, now);
            log.info("Marked all notifications read up to timestamp {} for user {} (updated={})",
                    upToTimestamp, currentUserId, updated);
        } else {
            updated = notificationRepository.markAllRead(currentUserId, now);
            log.info("Marked all notifications read for user {} (updated={})", currentUserId, updated);
        }
        return updated;
    }

    @Override
    @Transactional
    public int markAllRead(Long currentUserId, Instant upToTimestamp) {
        return markAllRead(currentUserId, null, upToTimestamp);
    }

    @Override
    @Transactional
    public NotificationResponse archive(UUID id, Long currentUserId) {
        Notification notification = findAndAuthorize(id, currentUserId);
        if (notification.archive(Instant.now())) {
            notification = notificationRepository.save(notification);
        }
        return mapToResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long currentUserId) {
        long count = notificationRepository.countUnreadByRecipient(currentUserId);
        return new UnreadCountResponse(count);
    }

    @Override
    public NotificationResponse mapToResponse(Notification n) {
        NavigationActionDto action = null;
        if (n.getTargetType() != null) {
            action = NavigationActionDto.builder()
                    .targetType(n.getTargetType())
                    .targetId(n.getTargetId())
                    .build();
        }

        return NotificationResponse.builder()
                .id(n.getId())
                .recipientUserId(n.getRecipientUserId())
                .actorUserId(n.getActorUserId())
                .type(n.getType())
                .category(n.getCategory())
                .sourceType(n.getSourceType())
                .sourceId(n.getSourceId())
                .contextType(n.getContextType())
                .contextId(n.getContextId())
                .titleKey(n.getTitleKey())
                .bodyKey(n.getBodyKey())
                .action(action)
                .payload(n.getPayload())
                .createdAt(n.getCreatedAt())
                .seenAt(n.getSeenAt())
                .readAt(n.getReadAt())
                .archivedAt(n.getArchivedAt())
                .state(n.getState())
                .build();
    }

    private Notification findAndAuthorize(UUID id, Long currentUserId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        if (!notification.getRecipientUserId().equals(currentUserId)) {
            log.warn("Access denied to notification {} for user {}", id, currentUserId);
            throw new NotificationAccessDeniedException("You do not have permission to access this notification");
        }

        return notification;
    }
}
