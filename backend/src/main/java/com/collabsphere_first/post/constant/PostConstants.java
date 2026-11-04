package com.collabsphere_first.post.constant;

/**
 * Central constants for the Platform Post domain.
 * Avoids scattering magic numbers across entities, DTOs, and validation logic.
 */
public final class PostConstants {

    private PostConstants() {}

    /** Maximum allowed characters in a post body text. */
    public static final int MAX_BODY_LENGTH = 10000;

    /** Maximum allowed media attachments per post. */
    public static final int MAX_MEDIA_COUNT = 10;

    /** Maximum allowed storage key / URL length. */
    public static final int MAX_STORAGE_KEY_LENGTH = 1024;

    /** Maximum allowed MIME type length. */
    public static final int MAX_MIME_TYPE_LENGTH = 100;

    /** Default page limit for cursor pagination. */
    public static final int DEFAULT_PAGE_LIMIT = 20;

    /** Maximum allowed page limit for cursor pagination. */
    public static final int MAX_PAGE_LIMIT = 50;
}
