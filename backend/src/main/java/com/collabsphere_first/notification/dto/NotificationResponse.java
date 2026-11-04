package com.collabsphere_first.notification.dto;

import com.collabsphere_first.notification.enums.NotificationCategory;
import com.collabsphere_first.notification.enums.NotificationState;
import com.collabsphere_first.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private Long recipientUserId;
    private Long actorUserId;
    private NotificationType type;
    private NotificationCategory category;
    private String sourceType;
    private String sourceId;
    private String contextType;
    private String contextId;
    private String titleKey;
    private String bodyKey;
    private NavigationActionDto action;
    private Map<String, Object> payload;
    private Instant createdAt;
    private Instant seenAt;
    private Instant readAt;
    private Instant archivedAt;
    private NotificationState state;
}
