package com.collabsphere_first.messaging.exception;

public class InvalidMessagingCursorException extends MessagingException {
    public InvalidMessagingCursorException(String message) {
        super(message);
    }

    public InvalidMessagingCursorException(String message, Throwable cause) {
        super(message, cause);
    }
}
