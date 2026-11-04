package com.collabsphere_first.community.exception;

import java.util.UUID;

public class CommunityNotFoundException extends RuntimeException {
    public CommunityNotFoundException(UUID communityId) {
        super("Community not found with id: " + communityId);
    }

    public CommunityNotFoundException(String message) {
        super(message);
    }
}
