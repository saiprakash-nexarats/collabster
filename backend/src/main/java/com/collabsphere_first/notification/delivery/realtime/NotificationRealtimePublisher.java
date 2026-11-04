package com.collabsphere_first.notification.delivery.realtime;

import com.collabsphere_first.notification.dto.NotificationResponse;

public interface NotificationRealtimePublisher {
    void publishRealtimeNotification(String recipientUserPrincipal, NotificationResponse notification);
}
