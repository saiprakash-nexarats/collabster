package com.collabsphere_first.community.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PostCreatedEvent extends ApplicationEvent {
    private final UUID postId;
    private final UUID communityId;
    private final Long authorId;
    private final String contentText;

    public PostCreatedEvent(Object source, UUID postId, UUID communityId, Long authorId, String contentText) {
        super(source);
        this.postId = postId;
        this.communityId = communityId;
        this.authorId = authorId;
        this.contentText = contentText;
    }
}
