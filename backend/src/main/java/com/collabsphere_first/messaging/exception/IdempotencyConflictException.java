package com.collabsphere_first.messaging.exception;

public class IdempotencyConflictException extends MessagingException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
