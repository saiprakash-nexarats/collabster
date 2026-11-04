package com.collabsphere_first.messaging.exception;

public class ConcurrentIdempotentSendException extends RuntimeException {
    public ConcurrentIdempotentSendException(Throwable cause) {
        super(cause);
    }
}
