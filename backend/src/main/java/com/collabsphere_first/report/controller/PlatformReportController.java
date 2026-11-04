package com.collabsphere_first.report.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.report.dto.CreatePlatformReportRequest;
import com.collabsphere_first.report.dto.PlatformReportResponse;
import com.collabsphere_first.report.dto.ResolvePlatformReportRequest;
import com.collabsphere_first.report.enums.PlatformReportStatus;
import com.collabsphere_first.report.service.PlatformReportService;
import com.collabsphere_first.security.service.CurrentUserService;
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
public class PlatformReportController {

    private final PlatformReportService reportService;
    private final CurrentUserService currentUserService;

    /**
     * File a platform-wide report against a User, PlatformPost, CommunityPost, or DirectMessage.
     */
    @PostMapping("/api/v1/platform/reports")
    public ResponseEntity<ApiResponse<PlatformReportResponse>> fileReport(
            @Valid @RequestBody CreatePlatformReportRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        PlatformReportResponse response = reportService.fileReport(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report submitted successfully", response));
    }

    /**
     * View the platform moderation queue (Platform Admin only).
     */
    @GetMapping("/api/v1/admin/reports")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Page<PlatformReportResponse>>> getReportQueue(
            @RequestParam(required = false) PlatformReportStatus status,
            Pageable pageable
    ) {
        Page<PlatformReportResponse> response = reportService.getReportQueue(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Resolve a platform report with a moderation action (Platform Admin only).
     */
    @PatchMapping("/api/v1/admin/reports/{reportId}/resolve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PlatformReportResponse>> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolvePlatformReportRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        PlatformReportResponse response = reportService.resolveReport(reportId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Report resolved successfully", response));
    }
}
