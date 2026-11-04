package com.collabsphere_first.notification.service;

import com.collabsphere_first.notification.dto.*;
import com.collabsphere_first.notification.entity.Notification;
import com.collabsphere_first.notification.enums.NotificationCategory;

import java.time.Instant;
import java.util.UUID;

public interface NotificationService {
    Notification createNotification(CreateNotificationCommand command);
    NotificationPageResponse getNotifications(Long userId, String cursor, int limit, NotificationCategory category, String filter);
    NotificationResponse getNotification(UUID id, Long currentUserId);
    NotificationResponse markSeen(UUID id, Long currentUserId);
    NotificationResponse markRead(UUID id, Long currentUserId);
    int markAllRead(Long currentUserId, Instant upToTimestamp);
    int markAllRead(Long currentUserId, String readUpToCursor, Instant upToTimestamp);
    NotificationResponse archive(UUID id, Long currentUserId);
    UnreadCountResponse getUnreadCount(Long currentUserId);
    NotificationResponse mapToResponse(Notification notification);
}
