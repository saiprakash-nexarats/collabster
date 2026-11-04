package com.collabsphere_first.community.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class ResourceLifecycleEvent extends ApplicationEvent {
    public enum Action {
        CREATED,
        APPROVED,
        REJECTED
    }

    private final UUID resourceId;
    private final UUID communityId;
    private final Long uploaderId;
    private final Long actorId;
    private final Action action;
    private final String title;

    public ResourceLifecycleEvent(
            Object source,
            UUID resourceId,
            UUID communityId,
            Long uploaderId,
            Long actorId,
            Action action,
            String title
    ) {
        super(source);
        this.resourceId = resourceId;
        this.communityId = communityId;
        this.uploaderId = uploaderId;
        this.actorId = actorId;
        this.action = action;
        this.title = title;
    }
}
