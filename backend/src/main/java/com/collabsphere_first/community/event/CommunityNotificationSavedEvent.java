package com.collabsphere_first.community.event;

import com.collabsphere_first.community.entity.CommunityNotification;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CommunityNotificationSavedEvent extends ApplicationEvent {
    private final CommunityNotification notification;

    public CommunityNotificationSavedEvent(Object source, CommunityNotification notification) {
        super(source);
        this.notification = notification;
    }
}
