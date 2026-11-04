package com.collabsphere_first.post.exception;

import java.util.UUID;

public class PlatformPostNotFoundException extends RuntimeException {

    public PlatformPostNotFoundException(UUID id) {
        super("Platform post not found with ID: " + id);
    }

    public PlatformPostNotFoundException(String message) {
        super(message);
    }
}
