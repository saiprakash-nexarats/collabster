package com.collabsphere_first.post.service;

import com.collabsphere_first.post.dto.request.CreatePlatformPostRequest;
import com.collabsphere_first.post.dto.request.UpdatePlatformPostRequest;
import com.collabsphere_first.post.dto.response.PlatformPostCursorPageResponse;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;

import java.util.UUID;

public interface PlatformPostService {

    /**
     * Creates a new PlatformPost for the current authenticated user.
     */
    PlatformPostResponse createPost(CreatePlatformPostRequest request);

    /**
     * Retrieves a single active PlatformPost by ID, enforcing visibility rules.
     */
    PlatformPostResponse getPostById(UUID postId);

    /**
     * Updates an existing PlatformPost. Only the post author can update their post.
     */
    PlatformPostResponse updatePost(UUID postId, UpdatePlatformPostRequest request);

    /**
     * Soft-deletes a PlatformPost. Only the post author can delete their post.
     */
    void deletePost(UUID postId);

    /**
     * Returns cursor-paginated active posts authored by the current authenticated user.
     *
     * @param cursor Opaque cursor token from a previous page, or null for initial page
     * @param limit  Maximum items to return (clamped between 1 and MAX_PAGE_LIMIT)
     */
    PlatformPostCursorPageResponse getMyPosts(String cursor, int limit);
}
