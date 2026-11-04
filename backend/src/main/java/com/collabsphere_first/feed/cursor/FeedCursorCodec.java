package com.collabsphere_first.feed.cursor;

import com.collabsphere_first.feed.exception.InvalidFeedCursorException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class FeedCursorCodec {

    private static final int MAX_POSITION = 10000;

    public String encode(String sessionId, int position) {
        if (sessionId == null || sessionId.isBlank() || position < 0) {
            throw new InvalidFeedCursorException("Cannot encode cursor with null sessionId or negative position");
        }
        String raw = sessionId + ":" + position;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public FeedCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            throw new InvalidFeedCursorException("Cursor must not be blank");
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            String raw = new String(bytes, StandardCharsets.UTF_8);
            int colonIdx = raw.indexOf(':');
            if (colonIdx <= 0 || colonIdx >= raw.length() - 1) {
                throw new InvalidFeedCursorException("Invalid feed cursor format");
            }
            String sessionId = raw.substring(0, colonIdx);
            int position = Integer.parseInt(raw.substring(colonIdx + 1));
            if (position < 0 || position > MAX_POSITION) {
                throw new InvalidFeedCursorException("Cursor position out of bounds: " + position);
            }
            return new FeedCursor(sessionId, position);
        } catch (IllegalArgumentException ex) {
            throw new InvalidFeedCursorException("Invalid feed cursor: " + ex.getMessage());
        }
    }
}
