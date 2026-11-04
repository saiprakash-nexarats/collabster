package com.collabsphere_first.messaging.exception;

public class MessagingRateLimitExceededException extends MessagingException {
    public MessagingRateLimitExceededException(String message) {
        super(message);
    }
}
