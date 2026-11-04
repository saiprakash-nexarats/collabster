package com.collabsphere_first.feed.exception;

public class FeedException extends RuntimeException {
    public FeedException(String message) {
        super(message);
    }

    public FeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
