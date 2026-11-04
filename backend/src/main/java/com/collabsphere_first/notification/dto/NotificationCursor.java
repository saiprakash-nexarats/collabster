package com.collabsphere_first.notification.dto;

import com.collabsphere_first.notification.exception.NotificationCursorException;
import lombok.Value;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Value
public class NotificationCursor {
    Instant createdAt;
    UUID id;

    public String toCursorString() {
        String raw = createdAt.toEpochMilli() + ":" + id.toString();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static NotificationCursor fromCursorString(String cursorStr) {
        if (cursorStr == null || cursorStr.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursorStr);
            String raw = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length != 2) {
                throw new NotificationCursorException("Malformed cursor format");
            }
            long epochMilli = Long.parseLong(parts[0]);
            UUID id = UUID.fromString(parts[1]);
            return new NotificationCursor(Instant.ofEpochMilli(epochMilli), id);
        } catch (Exception e) {
            throw new NotificationCursorException("Invalid notification cursor: " + e.getMessage());
        }
    }
}
