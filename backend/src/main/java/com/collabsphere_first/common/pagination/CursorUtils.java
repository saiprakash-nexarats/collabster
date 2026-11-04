package com.collabsphere_first.common.pagination;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Shared utility for deterministic keyset pagination cursor encoding and decoding.
 */
public final class CursorUtils {

    private static final long MAX_EPOCH_MILLI = 32503680000000L; // Year 3000 boundary

    private CursorUtils() {}

    public static String encode(Instant createdAt, UUID id) {
        if (createdAt == null || id == null) {
            return null;
        }
        String raw = createdAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String encode(Instant createdAt, Long id) {
        if (createdAt == null || id == null) {
            return null;
        }
        String raw = createdAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static DecodedUuidCursor decodeUuidCursor(String cursor) {
        String[] parts = parseRawCursor(cursor);
        long epochMilli = parseEpochMilli(parts[0]);
        try {
            UUID id = UUID.fromString(parts[1]);
            return new DecodedUuidCursor(Instant.ofEpochMilli(epochMilli), id);
        } catch (IllegalArgumentException ex) {
            throw new InvalidCursorException("Invalid UUID component in pagination cursor.");
        }
    }

    public static CursorPayload decode(String cursor) {
        DecodedUuidCursor decoded = decodeUuidCursor(cursor);
        return new CursorPayload(decoded.createdAt(), decoded.id());
    }

    public static DecodedLongCursor decodeLongCursor(String cursor) {
        String[] parts = parseRawCursor(cursor);
        long epochMilli = parseEpochMilli(parts[0]);
        try {
            long id = Long.parseLong(parts[1]);
            if (id <= 0) {
                throw new InvalidCursorException("ID component must be positive.");
            }
            return new DecodedLongCursor(Instant.ofEpochMilli(epochMilli), id);
        } catch (NumberFormatException ex) {
            throw new InvalidCursorException("Invalid numeric ID component in pagination cursor.");
        }
    }

    private static String[] parseRawCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            throw new InvalidCursorException("Pagination cursor token must not be blank.");
        }
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cursor.trim());
            String raw = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new InvalidCursorException("Malformed cursor format; expected timestamp:id.");
            }
            return parts;
        } catch (IllegalArgumentException ex) {
            throw new InvalidCursorException("Invalid Base64 encoding in pagination cursor.");
        }
    }

    private static long parseEpochMilli(String part) {
        try {
            long epochMilli = Long.parseLong(part);
            if (epochMilli <= 0 || epochMilli > MAX_EPOCH_MILLI) {
                throw new InvalidCursorException("Cursor timestamp value is out of valid temporal bounds.");
            }
            return epochMilli;
        } catch (NumberFormatException ex) {
            throw new InvalidCursorException("Invalid numeric timestamp in pagination cursor.");
        }
    }

    public record CursorPayload(Instant timestamp, UUID uuid) {}
    public record DecodedUuidCursor(Instant createdAt, UUID id) {}
    public record DecodedLongCursor(Instant createdAt, Long id) {}
}
