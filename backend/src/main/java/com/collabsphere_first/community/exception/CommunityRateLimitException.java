package com.collabsphere_first.community.exception;

public class CommunityRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;
    public CommunityRateLimitException(String message) { this(message, 1); }
    public CommunityRateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
