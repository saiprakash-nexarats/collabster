package com.collabsphere_first.community.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class CommentCreatedEvent extends ApplicationEvent {
    private final UUID commentId;
    private final UUID postId;
    private final UUID communityId;
    private final Long authorId;
    private final UUID parentCommentId;
    private final Long parentAuthorId;
    private final Long postAuthorId;
    private final String body;

    public CommentCreatedEvent(
            Object source,
            UUID commentId,
            UUID postId,
            UUID communityId,
            Long authorId,
            UUID parentCommentId,
            Long parentAuthorId,
            Long postAuthorId,
            String body
    ) {
        super(source);
        this.commentId = commentId;
        this.postId = postId;
        this.communityId = communityId;
        this.authorId = authorId;
        this.parentCommentId = parentCommentId;
        this.parentAuthorId = parentAuthorId;
        this.postAuthorId = postAuthorId;
        this.body = body;
    }
}
