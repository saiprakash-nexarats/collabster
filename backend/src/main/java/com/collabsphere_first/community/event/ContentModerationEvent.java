package com.collabsphere_first.community.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class ContentModerationEvent extends ApplicationEvent {
    public enum Action {
        POST_APPROVED,
        POST_REJECTED,
        POST_FLAGGED,
        COMMENT_APPROVED,
        COMMENT_REJECTED,
        COMMENT_FLAGGED,
        RESOURCE_APPROVED,
        RESOURCE_REJECTED,
        RESOURCE_FLAGGED
    }

    private final UUID communityId;
    private final String targetType;
    private final String targetId;
    private final Long authorId;
    private final Long moderatorId;
    private final Action action;
    private final String reason;

    public ContentModerationEvent(
            Object source,
            UUID communityId,
            String targetType,
            String targetId,
            Long authorId,
            Long moderatorId,
            Action action,
            String reason
    ) {
        super(source);
        this.communityId = communityId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.authorId = authorId;
        this.moderatorId = moderatorId;
        this.action = action;
        this.reason = reason;
    }
}
