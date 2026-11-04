package com.collabsphere_first.notification.delivery.realtime;

import com.collabsphere_first.notification.config.NotificationProperties;
import com.collabsphere_first.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRealtimePublisherImpl implements NotificationRealtimePublisher {

    public static final String QUEUE_NOTIFICATIONS = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationProperties properties;

    @Override
    public void publishRealtimeNotification(String recipientUserPrincipal, NotificationResponse notification) {
        if (!properties.getRealtime().isEnabled()) {
            log.debug("Realtime notification disabled in config (notificationId={})", notification.getId());
            return;
        }

        if (recipientUserPrincipal == null || recipientUserPrincipal.isBlank()) {
            return;
        }

        try {
            messagingTemplate.convertAndSendToUser(recipientUserPrincipal, QUEUE_NOTIFICATIONS, notification);
            log.debug("Delivered realtime notification to user {} (notificationId={})",
                    recipientUserPrincipal, notification.getId());
        } catch (Exception ex) {
            log.warn("Failed to deliver realtime notification to user {}: {}",
                    recipientUserPrincipal, ex.getMessage());
        }
    }
}
