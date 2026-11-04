package com.collabsphere_first.interaction.controller;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.interaction.dto.*;
import com.collabsphere_first.interaction.service.InteractionService;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;
    private final CurrentUserService currentUserService;

    // ==========================================
    // LIKE
    // ==========================================

    @PutMapping("/{contentType}/{contentId}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> likeContent(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId
    ) {
        User user = currentUserService.getCurrentUser();
        ContentRef contentRef = new ContentRef(contentType, contentId);
        LikeResponse response = interactionService.likeContent(user, contentRef);
        return ResponseEntity.ok(ApiResponse.success("Liked successfully", response));
    }

    @DeleteMapping("/{contentType}/{contentId}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> unlikeContent(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId
    ) {
        User user = currentUserService.getCurrentUser();
        ContentRef contentRef = new ContentRef(contentType, contentId);
        LikeResponse response = interactionService.unlikeContent(user, contentRef);
        return ResponseEntity.ok(ApiResponse.success("Unliked successfully", response));
    }

    // ==========================================
    // SAVE
    // ==========================================

    @PutMapping("/{contentType}/{contentId}/save")
    public ResponseEntity<ApiResponse<SaveResponse>> saveContent(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId
    ) {
        User user = currentUserService.getCurrentUser();
        ContentRef contentRef = new ContentRef(contentType, contentId);
        SaveResponse response = interactionService.saveContent(user, contentRef);
        return ResponseEntity.ok(ApiResponse.success("Saved successfully", response));
    }

    @DeleteMapping("/{contentType}/{contentId}/save")
    public ResponseEntity<ApiResponse<SaveResponse>> unsaveContent(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId
    ) {
        User user = currentUserService.getCurrentUser();
        ContentRef contentRef = new ContentRef(contentType, contentId);
        SaveResponse response = interactionService.unsaveContent(user, contentRef);
        return ResponseEntity.ok(ApiResponse.success("Unsaved successfully", response));
    }

    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<SavedItemsCursorPageResponse>> getSavedContent(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        User user = currentUserService.getCurrentUser();
        SavedItemsCursorPageResponse response = interactionService.getSavedContent(user, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==========================================
    // SHARE
    // ==========================================

    @PostMapping("/{contentType}/{contentId}/share")
    public ResponseEntity<ApiResponse<Void>> shareContent(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId
    ) {
        User user = currentUserService.getCurrentUser();
        ContentRef contentRef = new ContentRef(contentType, contentId);
        interactionService.recordShare(user, contentRef);
        return ResponseEntity.ok(ApiResponse.success("Share recorded successfully", null));
    }

    // ==========================================
    // EVENTS
    // ==========================================

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<Void>> recordEvent(@Valid @RequestBody InteractionEventRequest request) {
        User user = currentUserService.getCurrentUser();
        interactionService.recordEvent(user, request);
        return ResponseEntity.ok(ApiResponse.success("Event recorded successfully", null));
    }

    @PostMapping("/events/batch")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> recordBatchEvents(
            @Valid @RequestBody BatchInteractionEventRequest request
    ) {
        User user = currentUserService.getCurrentUser();
        int recorded = interactionService.recordBatchEvents(user, request.getEvents());
        return ResponseEntity.ok(ApiResponse.success("Batch events recorded", Map.of("recordedCount", recorded)));
    }

    // ==========================================
    // STATUS / COUNTS
    // ==========================================

    @GetMapping("/{contentType}/{contentId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInteractionStatus(
            @PathVariable ContentType contentType,
            @PathVariable UUID contentId
    ) {
        User user = currentUserService.getCurrentUser();
        ContentRef contentRef = new ContentRef(contentType, contentId);
        boolean liked = interactionService.isLiked(user.getId(), contentRef);
        boolean saved = interactionService.isSaved(user.getId(), contentRef);
        long likeCount = interactionService.getLikeCount(contentRef);
        long saveCount = interactionService.getSaveCount(contentRef);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "contentRef", contentRef,
                "liked", liked,
                "saved", saved,
                "likeCount", likeCount,
                "saveCount", saveCount
        )));
    }
}
