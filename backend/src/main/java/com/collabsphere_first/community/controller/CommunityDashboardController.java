package com.collabsphere_first.community.controller;
import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.service.CommunityDashboardService;
import com.collabsphere_first.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;
@RestController @RequiredArgsConstructor
public class CommunityDashboardController {
    private final CommunityDashboardService dashboardService;
    private final CurrentUserService currentUserService;
    @GetMapping("/api/v1/communities/{communityId}/dashboard")
    public ResponseEntity<ApiResponse<CommunityDashboardDto>> dashboard(@PathVariable UUID communityId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.dashboard(communityId, currentUserService.getCurrentUser().getId())));
    }
    @GetMapping("/api/v1/communities/{communityId}/analytics")
    public ResponseEntity<ApiResponse<CommunityAnalyticsDto>> analytics(@PathVariable UUID communityId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.analytics(communityId,
                currentUserService.getCurrentUser().getId(), from, to)));
    }
}
