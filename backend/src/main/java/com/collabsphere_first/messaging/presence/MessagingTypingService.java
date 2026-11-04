package com.collabsphere_first.messaging.presence;

import java.util.UUID;

public interface MessagingTypingService {

    void handleTyping(UUID conversationId, Long senderId, String senderUsername, boolean typing);

    boolean isTyping(UUID conversationId, Long userId);
}
