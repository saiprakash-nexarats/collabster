package com.collabsphere_first.notification.dto;

import com.collabsphere_first.notification.enums.NavigationTargetType;
import com.collabsphere_first.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationCommand {
    private Long recipientUserId;
    private Long actorUserId;
    private NotificationType type;
    private String sourceType;
    private String sourceId;
    private String contextType;
    private String contextId;
    private String eventKey;
    private String titleKey;
    private String bodyKey;
    private NavigationTargetType targetType;
    private String targetId;
    private Map<String, Object> payload;
    @Builder.Default
    private boolean suppressExternalChannels = false;
}
