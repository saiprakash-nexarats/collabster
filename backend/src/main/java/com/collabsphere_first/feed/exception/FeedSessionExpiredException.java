package com.collabsphere_first.feed.exception;

public class FeedSessionExpiredException extends FeedException {

    private final String errorCode;

    public FeedSessionExpiredException(String message) {
        super(message);
        this.errorCode = "FEED_SESSION_EXPIRED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
