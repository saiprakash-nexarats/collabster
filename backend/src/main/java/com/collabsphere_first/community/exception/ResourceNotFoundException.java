package com.collabsphere_first.community.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(UUID resourceId) {
        super("Resource not found with id: " + resourceId);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
