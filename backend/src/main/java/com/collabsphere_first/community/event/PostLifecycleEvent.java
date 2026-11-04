package com.collabsphere_first.community.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PostLifecycleEvent extends ApplicationEvent {
    public enum Action {
        PINNED,
        UNPINNED,
        LOCKED,
        UNLOCKED
    }

    private final UUID postId;
    private final UUID communityId;
    private final Long authorId;
    private final Long actorId;
    private final Action action;
    private final String title;

    public PostLifecycleEvent(
            Object source,
            UUID postId,
            UUID communityId,
            Long authorId,
            Long actorId,
            Action action,
            String title
    ) {
        super(source);
        this.postId = postId;
        this.communityId = communityId;
        this.authorId = authorId;
        this.actorId = actorId;
        this.action = action;
        this.title = title;
    }
}
