package com.collabsphere_first.notification.bridge;

import com.collabsphere_first.community.entity.CommunityNotification;
import com.collabsphere_first.community.event.CommunityNotificationSavedEvent;
import com.collabsphere_first.notification.dto.CreateNotificationCommand;
import com.collabsphere_first.notification.enums.NavigationTargetType;
import com.collabsphere_first.notification.enums.NotificationType;
import com.collabsphere_first.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityNotificationBridge {

    private final NotificationService notificationService;

    @EventListener
    public void onCommunityNotificationSaved(CommunityNotificationSavedEvent event) {
        CommunityNotification cn = event.getNotification();
        if (cn == null || cn.getUserId() == null) {
            return;
        }

        log.debug("Bridging CommunityNotification to global notification center (cnId={}, userId={}, type={})",
                cn.getId(), cn.getUserId(), cn.getType());

        NotificationType type = mapCommunityType(cn.getType());
        NavigationTargetType targetType = "POST".equalsIgnoreCase(cn.getTargetType())
                ? NavigationTargetType.OPEN_COMMUNITY_POST
                : NavigationTargetType.OPEN_COMMUNITY;

        String targetId = cn.getTargetId() != null
                ? cn.getTargetId()
                : (cn.getCommunityId() != null ? cn.getCommunityId().toString() : null);

        Map<String, Object> payload = new HashMap<>();
        if (cn.getCommunityId() != null) {
            payload.put("communityId", cn.getCommunityId().toString());
        }
        if (cn.getTargetId() != null) {
            payload.put("targetId", cn.getTargetId());
            if ("POST".equalsIgnoreCase(cn.getTargetType())) {
                payload.put("communityPostId", cn.getTargetId());
            }
        }

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(cn.getUserId())
                .actorUserId(null)
                .type(type)
                .sourceType("COMMUNITY")
                .sourceId(cn.getId() != null ? cn.getId().toString() : null)
                .contextType(cn.getCommunityId() != null ? "COMMUNITY" : null)
                .contextId(cn.getCommunityId() != null ? cn.getCommunityId().toString() : null)
                .eventKey("COMMUNITY:" + cn.getId() + ":" + cn.getUserId())
                .titleKey("notification.community.title")
                .bodyKey("notification.community.body")
                .targetType(targetType)
                .targetId(targetId)
                .payload(payload)
                .build();

        notificationService.createNotification(cmd);
    }

    private NotificationType mapCommunityType(String rawType) {
        if (rawType == null) {
            return NotificationType.COMMUNITY_POST_ACTIVITY;
        }
        String upper = rawType.toUpperCase();
        if (upper.contains("INVITATION") || upper.contains("INVITE")) {
            return NotificationType.COMMUNITY_INVITATION;
        } else if (upper.contains("EVENT")) {
            return NotificationType.COMMUNITY_EVENT;
        } else if (upper.contains("MENTION")) {
            return NotificationType.COMMUNITY_MENTION;
        } else {
            return NotificationType.COMMUNITY_POST_ACTIVITY;
        }
    }
}
