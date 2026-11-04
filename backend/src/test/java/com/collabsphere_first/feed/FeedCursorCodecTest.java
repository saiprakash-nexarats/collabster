package com.collabsphere_first.feed;

import com.collabsphere_first.feed.cursor.FeedCursor;
import com.collabsphere_first.feed.cursor.FeedCursorCodec;
import com.collabsphere_first.feed.exception.InvalidFeedCursorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedCursorCodecTest {

    private FeedCursorCodec cursorCodec;

    @BeforeEach
    void setUp() {
        cursorCodec = new FeedCursorCodec();
    }

    @Test
    @DisplayName("Encode and decode roundtrip succeeds for valid sessionId and position")
    void testEncodeDecodeRoundTrip() {
        String sessionId = UUID.randomUUID().toString();
        int position = 40;

        String encoded = cursorCodec.encode(sessionId, position);
        assertThat(encoded).isNotBlank();

        FeedCursor decoded = cursorCodec.decode(encoded);
        assertThat(decoded.sessionId()).isEqualTo(sessionId);
        assertThat(decoded.position()).isEqualTo(position);
    }

    @Test
    @DisplayName("Encoding with null sessionId or negative position throws InvalidFeedCursorException")
    void testEncodeInvalidInputs() {
        assertThatThrownBy(() -> cursorCodec.encode(null, 10))
                .isInstanceOf(InvalidFeedCursorException.class);

        assertThatThrownBy(() -> cursorCodec.encode("session-1", -1))
                .isInstanceOf(InvalidFeedCursorException.class);
    }

    @Test
    @DisplayName("Decoding blank or invalid base64 throws InvalidFeedCursorException")
    void testDecodeMalformedBase64() {
        assertThatThrownBy(() -> cursorCodec.decode(null))
                .isInstanceOf(InvalidFeedCursorException.class);

        assertThatThrownBy(() -> cursorCodec.decode("   "))
                .isInstanceOf(InvalidFeedCursorException.class);

        assertThatThrownBy(() -> cursorCodec.decode("not-valid-base64!@#$"))
                .isInstanceOf(InvalidFeedCursorException.class);
    }

    @Test
    @DisplayName("Decoding cursor without colon or invalid position throws InvalidFeedCursorException")
    void testDecodeInvalidFormat() {
        String noColon = Base64.getUrlEncoder().encodeToString("just-a-session-id".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> cursorCodec.decode(noColon))
                .isInstanceOf(InvalidFeedCursorException.class);

        String nonNumericPos = Base64.getUrlEncoder().encodeToString("session-123:abc".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> cursorCodec.decode(nonNumericPos))
                .isInstanceOf(InvalidFeedCursorException.class);

        String negativePos = Base64.getUrlEncoder().encodeToString("session-123:-5".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> cursorCodec.decode(negativePos))
                .isInstanceOf(InvalidFeedCursorException.class);

        String oversizedPos = Base64.getUrlEncoder().encodeToString("session-123:999999".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> cursorCodec.decode(oversizedPos))
                .isInstanceOf(InvalidFeedCursorException.class);
    }
}
