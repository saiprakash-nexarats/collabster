package com.collabsphere_first.explore;

import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.cursor.ExploreKeysetCursor;
import com.collabsphere_first.explore.cursor.ExploreSearchCursor;
import com.collabsphere_first.explore.exception.InvalidExploreCursorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExploreCursorCodecTest {

    private ExploreCursorCodec cursorCodec;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        cursorCodec = new ExploreCursorCodec(objectMapper);
    }

    @Test
    @DisplayName("Search cursor roundtrip succeeds for matching query")
    void testSearchCursorRoundtrip() {
        String query = "rust programming";
        String queryHash = ExploreCursorCodec.computeQueryHash(query);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        ExploreSearchCursor original = new ExploreSearchCursor(
                queryHash,
                0.85,
                now,
                id,
                ContentType.PLATFORM_POST,
                20
        );

        String encoded = cursorCodec.encodeSearchCursor(original);
        assertThat(encoded).isNotNull().isNotBlank();

        ExploreSearchCursor decoded = cursorCodec.decodeSearchCursor(encoded, query);
        assertThat(decoded).isNotNull();
        assertThat(decoded.queryHash()).isEqualTo(queryHash);
        assertThat(decoded.rank()).isEqualTo(0.85);
        assertThat(decoded.id()).isEqualTo(id);
        assertThat(decoded.contentType()).isEqualTo(ContentType.PLATFORM_POST);
        assertThat(decoded.position()).isEqualTo(20);
    }

    @Test
    @DisplayName("Search cursor generated for 'rust' is strictly rejected for 'java' with HTTP 400 semantics")
    void testSearchCursorQueryMismatchRejected() {
        String queryRust = "rust";
        String queryJava = "java";
        String rustHash = ExploreCursorCodec.computeQueryHash(queryRust);

        ExploreSearchCursor rustCursor = new ExploreSearchCursor(
                rustHash,
                0.9,
                Instant.now(),
                UUID.randomUUID(),
                ContentType.PLATFORM_POST,
                10
        );

        String encoded = cursorCodec.encodeSearchCursor(rustCursor);

        assertThatThrownBy(() -> cursorCodec.decodeSearchCursor(encoded, queryJava))
                .isInstanceOf(InvalidExploreCursorException.class)
                .hasMessageContaining("Cursor was not generated for query");
    }

    @Test
    @DisplayName("Malformed Base64 cursor string throws InvalidExploreCursorException")
    void testMalformedCursorRejected() {
        assertThatThrownBy(() -> cursorCodec.decodeSearchCursor("!@#$%^&*()_invalid_base64", "rust"))
                .isInstanceOf(InvalidExploreCursorException.class);
    }

    @Test
    @DisplayName("Keyset cursor roundtrip succeeds")
    void testKeysetCursorRoundtrip() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        ExploreKeysetCursor original = new ExploreKeysetCursor(
                now,
                id,
                123L,
                ContentType.COMMUNITY_POST,
                42.5,
                15
        );

        String encoded = cursorCodec.encodeKeysetCursor(original);
        assertThat(encoded).isNotNull().isNotBlank();

        ExploreKeysetCursor decoded = cursorCodec.decodeKeysetCursor(encoded);
        assertThat(decoded).isNotNull();
        assertThat(decoded.id()).isEqualTo(id);
        assertThat(decoded.numericId()).isEqualTo(123L);
        assertThat(decoded.contentType()).isEqualTo(ContentType.COMMUNITY_POST);
        assertThat(decoded.score()).isEqualTo(42.5);
        assertThat(decoded.position()).isEqualTo(15);
    }
}
