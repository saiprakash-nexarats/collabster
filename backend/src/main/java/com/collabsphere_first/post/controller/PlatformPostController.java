package com.collabsphere_first.post.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.post.constant.PostConstants;
import com.collabsphere_first.post.dto.request.CreatePlatformPostRequest;
import com.collabsphere_first.post.dto.request.UpdatePlatformPostRequest;
import com.collabsphere_first.post.dto.response.PlatformPostCursorPageResponse;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.service.PlatformPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authoritative REST API for Platform Posts.
 * Base path: /api/v1/posts
 */
@RestController
@RequestMapping("/api/v1/posts/platform")
@RequiredArgsConstructor
public class PlatformPostController {

    private final PlatformPostService postService;

    /**
     * Creates a new platform post for the current authenticated user.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PlatformPostResponse>> createPost(
            @Valid @RequestBody CreatePlatformPostRequest request
    ) {
        PlatformPostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", response));
    }

    /**
     * Retrieves a single platform post by its unique ID.
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PlatformPostResponse>> getPostById(
            @PathVariable UUID postId
    ) {
        PlatformPostResponse response = postService.getPostById(postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates an existing platform post (author-only).
     */
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<PlatformPostResponse>> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePlatformPostRequest request
    ) {
        PlatformPostResponse response = postService.updatePost(postId, request);
        return ResponseEntity.ok(ApiResponse.success("Post updated successfully", response));
    }

    /**
     * Soft-deletes a platform post (author-only).
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID postId
    ) {
        postService.deletePost(postId);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
    }

    /**
     * Returns keyset / cursor-paginated posts authored by the current authenticated user.
     * Supports the mobile user sidebar "My Posts" view.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PlatformPostCursorPageResponse>> getMyPosts(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + PostConstants.DEFAULT_PAGE_LIMIT) int limit
    ) {
        PlatformPostCursorPageResponse response = postService.getMyPosts(cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
