package com.collabsphere_first.community.controller;

import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.community.dto.CommentDto;
import com.collabsphere_first.community.dto.CreateCommentRequest;
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
@RequestMapping("/api/v1/posts/{id}/comments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Comments API", description = "Endpoints for post comment threads")
public class CommentController {

    private final ContentService contentService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(summary = "Create comment or nested reply on post (Requires MEMBER role)")
    public ResponseEntity<ApiResponse<CommentDto>> createComment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentRequest request) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CommentDto created = contentService.createComment(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Comment created, pending moderation clearance", created));
    }

    @GetMapping
    @Operation(summary = "Get comments for a post")
    public ResponseEntity<ApiResponse<Page<CommentDto>>> getPostComments(
            @PathVariable UUID id,
            Pageable pageable) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        Page<CommentDto> comments = contentService.getPostComments(id, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}
