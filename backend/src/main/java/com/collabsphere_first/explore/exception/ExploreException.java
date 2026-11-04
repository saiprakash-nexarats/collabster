package com.collabsphere_first.explore.exception;

public class ExploreException extends RuntimeException {
    public ExploreException(String message) {
        super(message);
    }

    public ExploreException(String message, Throwable cause) {
        super(message, cause);
    }
}
