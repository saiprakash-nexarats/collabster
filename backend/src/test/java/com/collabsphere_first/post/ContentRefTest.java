package com.collabsphere_first.post;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ContentRef Contract Tests")
class ContentRefTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should create PlatformPost ContentRef correctly")
    void testPlatformPostContentRef() {
        UUID id = UUID.randomUUID();
        ContentRef ref = ContentRef.platformPost(id);

        assertEquals(ContentType.PLATFORM_POST, ref.type());
        assertEquals(id, ref.id());
    }

    @Test
    @DisplayName("Should create CommunityPost ContentRef correctly")
    void testCommunityPostContentRef() {
        UUID id = UUID.randomUUID();
        ContentRef ref = ContentRef.communityPost(id);

        assertEquals(ContentType.COMMUNITY_POST, ref.type());
        assertEquals(id, ref.id());
    }

    @Test
    @DisplayName("Should reject null type or id")
    void testNullValidation() {
        UUID id = UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> new ContentRef(null, id));
        assertThrows(NullPointerException.class, () -> new ContentRef(ContentType.PLATFORM_POST, null));
    }

    @Test
    @DisplayName("Should serialize and deserialize ContentRef to JSON cleanly")
    void testJsonSerialization() throws Exception {
        UUID id = UUID.fromString("f459ba92-7988-4d5e-9988-123456789abc");
        ContentRef ref = ContentRef.platformPost(id);

        String json = objectMapper.writeValueAsString(ref);
        assertTrue(json.contains("\"type\":\"PLATFORM_POST\""));
        assertTrue(json.contains("\"id\":\"f459ba92-7988-4d5e-9988-123456789abc\""));

        ContentRef deserialized = objectMapper.readValue(json, ContentRef.class);
        assertEquals(ref, deserialized);
    }
}
