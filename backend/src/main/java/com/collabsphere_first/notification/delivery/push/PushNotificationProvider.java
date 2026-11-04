package com.collabsphere_first.notification.delivery.push;

import com.collabsphere_first.notification.entity.Notification;
import com.collabsphere_first.notification.entity.NotificationDevice;

public interface PushNotificationProvider {
    PushSendResult send(NotificationDevice device, Notification notification);
    boolean isEnabled();
}
