package com.collabsphere_first.common.content;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Lightweight, immutable reference to authoritative content across the Collobster platform.
 * Enables Feed, Explore, Interactions, and Recommendations to reference and aggregate
 * heterogeneous content without coupling domain entities or creating super-tables.
 *
 * @param type The type of the content (e.g. PLATFORM_POST, COMMUNITY_POST)
 * @param id   The unique UUID of the referenced content
 */
public record ContentRef(
        @JsonProperty("type") ContentType type,
        @JsonProperty("id") UUID id
) implements Serializable {

    public ContentRef {
        Objects.requireNonNull(type, "ContentType must not be null");
        Objects.requireNonNull(id, "Content ID must not be null");
    }

    public static ContentRef platformPost(UUID id) {
        return new ContentRef(ContentType.PLATFORM_POST, id);
    }

    public static ContentRef communityPost(UUID id) {
        return new ContentRef(ContentType.COMMUNITY_POST, id);
    }
}
