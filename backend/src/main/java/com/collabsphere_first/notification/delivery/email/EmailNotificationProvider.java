package com.collabsphere_first.notification.delivery.email;

import com.collabsphere_first.notification.entity.Notification;

public interface EmailNotificationProvider {
    void sendNotificationEmail(String recipientEmail, String recipientName, Notification notification);
    boolean isEnabled();
}
