package com.collabsphere_first.messaging.cursor;

import com.collabsphere_first.messaging.exception.InvalidMessagingCursorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagingCursorCodec {

    private final ObjectMapper objectMapper;

    public String encodeConversationCursor(ConversationCursor cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(cursor);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (JsonProcessingException e) {
            log.error("Failed to encode conversation cursor: {}", e.getMessage());
            throw new InvalidMessagingCursorException("Failed to encode conversation cursor", e);
        }
    }

    public ConversationCursor decodeConversationCursor(String cursorStr) {
        if (cursorStr == null || cursorStr.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursorStr.trim());
            ConversationCursor cursor = objectMapper.readValue(bytes, ConversationCursor.class);
            if (cursor == null || cursor.conversationId() == null) {
                throw new InvalidMessagingCursorException("Malformed conversation cursor");
            }
            return cursor;
        } catch (Exception e) {
            throw new InvalidMessagingCursorException("Invalid conversation cursor: " + e.getMessage(), e);
        }
    }

    public String encodeMessageCursor(MessageCursor cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(cursor);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (JsonProcessingException e) {
            log.error("Failed to encode message cursor: {}", e.getMessage());
            throw new InvalidMessagingCursorException("Failed to encode message cursor", e);
        }
    }

    public MessageCursor decodeMessageCursor(String cursorStr) {
        if (cursorStr == null || cursorStr.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursorStr.trim());
            MessageCursor cursor = objectMapper.readValue(bytes, MessageCursor.class);
            if (cursor == null || cursor.createdAt() == null || cursor.messageId() == null) {
                throw new InvalidMessagingCursorException("Malformed message cursor");
            }
            return cursor;
        } catch (Exception e) {
            throw new InvalidMessagingCursorException("Invalid message cursor: " + e.getMessage(), e);
        }
    }
}
