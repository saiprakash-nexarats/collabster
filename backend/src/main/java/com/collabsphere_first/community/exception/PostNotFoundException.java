package com.collabsphere_first.community.exception;

import java.util.UUID;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(UUID postId) {
        super("Post not found with id: " + postId);
    }
}
