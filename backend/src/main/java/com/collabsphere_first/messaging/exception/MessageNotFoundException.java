package com.collabsphere_first.messaging.exception;

import java.util.UUID;

public class MessageNotFoundException extends MessagingException {
    public MessageNotFoundException(UUID messageId) {
        super("Message not found with id: " + messageId);
    }

    public MessageNotFoundException(String message) {
        super(message);
    }
}
