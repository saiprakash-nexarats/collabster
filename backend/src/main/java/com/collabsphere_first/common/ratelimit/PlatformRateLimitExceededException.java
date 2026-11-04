package com.collabsphere_first.common.ratelimit;

public class PlatformRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public PlatformRateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
