package com.collabsphere_first.network.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.UUID;

@Getter
public class UserFollowedEvent extends ApplicationEvent {
    private final UUID followId;
    private final Long followerId;
    private final Long followingId;
    private final Instant occurredAt;

    public UserFollowedEvent(Object source, UUID followId, Long followerId, Long followingId) {
        super(source);
        this.followId = followId;
        this.followerId = followerId;
        this.followingId = followingId;
        this.occurredAt = Instant.now();
    }

    public UserFollowedEvent(Object source, Long followerId, Long followingId) {
        this(source, null, followerId, followingId);
    }
}
