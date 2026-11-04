package com.collabsphere_first.explore.cursor;

import com.collabsphere_first.explore.exception.InvalidExploreCursorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExploreCursorCodec {

    private final ObjectMapper objectMapper;

    public static String computeQueryHash(String normalizedQuery) {
        if (normalizedQuery == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedQuery.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(normalizedQuery.hashCode());
        }
    }

    public String encodeSearchCursor(ExploreSearchCursor cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(cursor);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize search cursor: {}", e.getMessage());
            throw new InvalidExploreCursorException("Failed to encode cursor", e);
        }
    }

    public ExploreSearchCursor decodeSearchCursor(String cursorString, String normalizedQuery) {
        if (cursorString == null || cursorString.isBlank()) {
            return null;
        }
        try {
            byte[] jsonBytes = Base64.getUrlDecoder().decode(cursorString.trim());
            ExploreSearchCursor cursor = objectMapper.readValue(jsonBytes, ExploreSearchCursor.class);

            if (cursor == null) {
                throw new InvalidExploreCursorException("Malformed search cursor");
            }

            if (cursor.position() < 0) {
                throw new InvalidExploreCursorException("Cursor position must be non-negative");
            }
            if (Double.isNaN(cursor.rank()) || Double.isInfinite(cursor.rank()) || cursor.rank() < 0.0) {
                throw new InvalidExploreCursorException("Invalid cursor rank");
            }

            // Verify query-tie
            String expectedHash = computeQueryHash(normalizedQuery);
            if (cursor.queryHash() != null && !cursor.queryHash().equals(expectedHash)) {
                log.warn("Cursor query mismatch: cursor hash {} != expected hash {} for query '{}'",
                        cursor.queryHash(), expectedHash, normalizedQuery);
                throw new InvalidExploreCursorException("Cursor was not generated for query: " + normalizedQuery);
            }

            return cursor;
        } catch (IllegalArgumentException e) {
            throw new InvalidExploreCursorException("Malformed search cursor (invalid Base64): " + e.getMessage());
        } catch (InvalidExploreCursorException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidExploreCursorException("Malformed search cursor: " + e.getMessage());
        }
    }

    public String encodeKeysetCursor(ExploreKeysetCursor cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(cursor);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize keyset cursor: {}", e.getMessage());
            throw new InvalidExploreCursorException("Failed to encode keyset cursor", e);
        }
    }

    public ExploreKeysetCursor decodeKeysetCursor(String cursorString) {
        if (cursorString == null || cursorString.isBlank()) {
            return null;
        }
        try {
            byte[] jsonBytes = Base64.getUrlDecoder().decode(cursorString.trim());
            ExploreKeysetCursor cursor = objectMapper.readValue(jsonBytes, ExploreKeysetCursor.class);
            if (cursor == null) {
                throw new InvalidExploreCursorException("Malformed keyset cursor");
            }
            if (cursor.position() < 0) {
                throw new InvalidExploreCursorException("Cursor position must be non-negative");
            }
            if (Double.isNaN(cursor.score()) || Double.isInfinite(cursor.score())) {
                throw new InvalidExploreCursorException("Invalid cursor score");
            }
            return cursor;
        } catch (IllegalArgumentException e) {
            throw new InvalidExploreCursorException("Malformed keyset cursor (invalid Base64): " + e.getMessage());
        } catch (InvalidExploreCursorException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidExploreCursorException("Malformed keyset cursor: " + e.getMessage());
        }
    }
}
