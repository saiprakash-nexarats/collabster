package com.collabsphere_first.notification.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.notification.dto.NotificationPreferenceDto;
import com.collabsphere_first.notification.dto.UpdateNotificationPreferenceRequest;
import com.collabsphere_first.notification.service.NotificationPreferenceService;
import com.collabsphere_first.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/notification-center/preferences", "/api/v1/notifications/preferences", "/v1/notifications/preferences"})
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationPreferenceDto>>> getPreferences() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<NotificationPreferenceDto> preferences = preferenceService.getPreferences(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Notification preferences retrieved", preferences));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceDto>> updatePreference(
            @RequestBody @Valid UpdateNotificationPreferenceRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NotificationPreferenceDto updated = preferenceService.updatePreference(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Notification preference updated", updated));
    }
}
