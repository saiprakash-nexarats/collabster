package com.collabsphere_first.messaging.exception;

import java.util.UUID;

public class ConversationNotFoundException extends MessagingException {
    public ConversationNotFoundException(UUID conversationId) {
        super("Conversation not found with id: " + conversationId);
    }

    public ConversationNotFoundException(String message) {
        super(message);
    }
}
