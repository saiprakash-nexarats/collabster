package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.service.CommunitySettingsService;
import com.collabsphere_first.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequiredArgsConstructor
public class CommunitySettingsController {
    private final CommunitySettingsService settingsService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/v1/communities/{communityId}/settings")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<CommunitySettingsDto>> get(@PathVariable UUID communityId) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.get(communityId, currentUserService.getCurrentUser().getId())));
    }

    @PatchMapping("/api/v1/communities/{communityId}/settings")
    @PreAuthorize("hasPermission(#communityId, 'ADMIN')")
    public ResponseEntity<ApiResponse<CommunitySettingsDto>> update(@PathVariable UUID communityId,
            @RequestBody UpdateCommunitySettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.update(communityId,
                currentUserService.getCurrentUser().getId(), request)));
    }
}
