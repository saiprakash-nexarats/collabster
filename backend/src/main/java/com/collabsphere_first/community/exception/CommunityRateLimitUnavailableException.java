package com.collabsphere_first.community.exception;

public class CommunityRateLimitUnavailableException
        extends RuntimeException {

    public CommunityRateLimitUnavailableException(String message) {
        super(message);
    }
}