package com.collabsphere_first.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class NotificationCreatedEvent extends ApplicationEvent {
    private final UUID notificationId;
    private final Long recipientUserId;

    public NotificationCreatedEvent(Object source, UUID notificationId, Long recipientUserId) {
        super(source);
        this.notificationId = notificationId;
        this.recipientUserId = recipientUserId;
    }
}
