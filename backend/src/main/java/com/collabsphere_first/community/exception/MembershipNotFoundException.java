package com.collabsphere_first.community.exception;

import java.util.UUID;

public class MembershipNotFoundException extends RuntimeException {
    public MembershipNotFoundException(UUID communityId, UUID userId) {
        super("Membership not found for user " + userId + " in community " + communityId);
    }

    public MembershipNotFoundException(String message) {
        super(message);
    }
}
