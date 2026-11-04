package com.collabsphere_first.community.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PollLifecycleEvent extends ApplicationEvent {
    public enum Action {
        CREATED,
        CLOSED
    }

    private final UUID pollId;
    private final UUID communityId;
    private final Long creatorId;
    private final Long actorId;
    private final Action action;
    private final String question;

    public PollLifecycleEvent(
            Object source,
            UUID pollId,
            UUID communityId,
            Long creatorId,
            Long actorId,
            Action action,
            String question
    ) {
        super(source);
        this.pollId = pollId;
        this.communityId = communityId;
        this.creatorId = creatorId;
        this.actorId = actorId;
        this.action = action;
        this.question = question;
    }
}
