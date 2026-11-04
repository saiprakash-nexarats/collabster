package com.collabsphere_first.community.controller;

import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.community.dto.CreatePostRequest;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.service.ContentService;
import com.collabsphere_first.community.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Posts API", description = "Endpoints for community posts and feed management")
public class PostController {

    private final ContentService contentService;
    private final CurrentUserService currentUserService;

    @GetMapping({"/api/v1/communities/{id}/posts", "/v1/communities/{id}/posts"})
    @Operation(summary = "Get community post feed (Requires FOLLOWER role or higher)")
    public ResponseEntity<ApiResponse<Page<PostDto>>> getCommunityFeed(
            @PathVariable UUID id,
            Pageable pageable) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        Page<PostDto> feed = contentService.getCommunityFeed(id, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(feed));
    }

    @PostMapping({"/api/v1/communities/{id}/posts", "/v1/communities/{id}/posts"})
    @PreAuthorize("hasPermission(#id, 'MEMBER')")
    @Operation(summary = "Create a post in community (Requires MEMBER role or higher)")
    public ResponseEntity<ApiResponse<PostDto>> createPost(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePostRequest request) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        PostDto created = contentService.createPost(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Post created, pending moderation clearance", created));
    }

    @GetMapping("/api/v1/posts/{id}")
    @Operation(summary = "Fetch single post details by ID")
    public ResponseEntity<ApiResponse<PostDto>> getPost(@PathVariable UUID id) {
        PostDto post = contentService.getPost(id, currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @PatchMapping("/api/v1/posts/{id}/pin")
    @Operation(summary = "Pin or unpin post (Requires ADMIN role or higher)")
    public ResponseEntity<ApiResponse<PostDto>> pinPost(
            @PathVariable UUID id,
            @RequestParam boolean pin) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        PostDto updated = contentService.pinPost(id, pin, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Post pin status updated", updated));
    }

    @PatchMapping("/api/v1/posts/{id}/lock")
    @Operation(summary = "Lock or unlock post for comments (Requires MODERATOR role or higher)")
    public ResponseEntity<ApiResponse<PostDto>> lockPost(
            @PathVariable UUID id,
            @RequestParam boolean lock) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        PostDto updated = contentService.lockPost(id, lock, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Post lock status updated", updated));
    }
}
