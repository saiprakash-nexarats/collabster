package com.collabsphere_first.community.controller;

import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.community.dto.CreateReportRequest;
import com.collabsphere_first.community.dto.ReportDto;
import com.collabsphere_first.community.dto.ResolveReportRequest;
import com.collabsphere_first.community.service.ModerationService;
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
@Tag(name = "Reports & Moderation API", description = "Endpoints for content reporting and moderation resolution")
public class ReportController {

    private final ModerationService moderationService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/v1/reports")
    @Operation(summary = "File a report against content or a user")
    public ResponseEntity<ApiResponse<ReportDto>> fileReport(@Valid @RequestBody CreateReportRequest request) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ReportDto report = moderationService.fileReport(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Report filed successfully", report));
    }

    @GetMapping("/api/v1/communities/{id}/reports")
    @PreAuthorize("hasPermission(#id, 'MODERATOR')")
    @Operation(summary = "View community moderation queue (Requires MODERATOR role or higher)")
    public ResponseEntity<ApiResponse<Page<ReportDto>>> getModerationQueue(
            @PathVariable UUID id,
            Pageable pageable) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        Page<ReportDto> reports = moderationService.getModerationQueue(id, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @PatchMapping("/api/v1/reports/{id}/resolve")
    @Operation(summary = "Resolve a moderation report (Requires MODERATOR role or higher)")
    public ResponseEntity<ApiResponse<ReportDto>> resolveReport(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveReportRequest request) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ReportDto resolved = moderationService.resolveReport(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Report resolved successfully", resolved));
    }

    @GetMapping("/api/v1/communities/{id}/pending-posts")
    @PreAuthorize("hasPermission(#id, 'MODERATOR')")
    @Operation(summary = "Get pending posts queue for moderation approval (Requires MODERATOR role or higher)")
    public ResponseEntity<ApiResponse<Page<com.collabsphere_first.community.dto.PostDto>>> getPendingPostsQueue(
            @PathVariable UUID id,
            Pageable pageable) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        Page<com.collabsphere_first.community.dto.PostDto> pendingPosts = moderationService.getPendingPostsQueue(id, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(pendingPosts));
    }

    @PatchMapping("/api/v1/posts/{id}/moderate")
    @Operation(summary = "Approve or reject a pending post (Requires MODERATOR role or higher)")
    public ResponseEntity<ApiResponse<com.collabsphere_first.community.dto.PostDto>> moderatePost(
            @PathVariable UUID id,
            @RequestParam boolean approve) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        com.collabsphere_first.community.dto.PostDto post = moderationService.approveOrRejectPost(id, approve, currentUserId);
        String msg = approve ? "Post approved and published" : "Post rejected and removed";
        return ResponseEntity.ok(ApiResponse.success(msg, post));
    }
}
