package com.collabsphere_first.notification.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.notification.dto.MarkAllReadRequest;
import com.collabsphere_first.notification.dto.NotificationPageResponse;
import com.collabsphere_first.notification.dto.NotificationResponse;
import com.collabsphere_first.notification.dto.UnreadCountResponse;
import com.collabsphere_first.notification.enums.NotificationCategory;
import com.collabsphere_first.notification.service.NotificationService;
import com.collabsphere_first.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/notification-center", "/api/v1/notifications", "/v1/notifications"})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPageResponse>> getNotifications(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit,
            @RequestParam(name = "category", required = false) NotificationCategory category,
            @RequestParam(name = "filter", required = false) String filter
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NotificationPageResponse response = notificationService.getNotifications(
                currentUserId, cursor, limit, category, filter);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", response));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @PathVariable("notificationId") UUID notificationId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NotificationResponse response = notificationService.getNotification(notificationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Notification retrieved successfully", response));
    }

    @PostMapping("/{id}/seen")
    public ResponseEntity<ApiResponse<NotificationResponse>> markSeen(
            @PathVariable("id") UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NotificationResponse response = notificationService.markSeen(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as seen", response));
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable("id") UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NotificationResponse response = notificationService.markRead(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllRead(
            @RequestBody(required = false) MarkAllReadRequest body,
            @RequestParam(name = "readUpToCursor", required = false) String readUpToCursor,
            @RequestParam(name = "upTo", required = false) Instant upTo
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        String effectiveCursor = body != null && body.getReadUpToCursor() != null
                ? body.getReadUpToCursor()
                : readUpToCursor;
        Instant effectiveUpTo = body != null && body.getUpTo() != null
                ? body.getUpTo()
                : upTo;

        int count = notificationService.markAllRead(currentUserId, effectiveCursor, effectiveUpTo);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", Map.of("updatedCount", count)));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<NotificationResponse>> archive(
            @PathVariable("id") UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NotificationResponse response = notificationService.archive(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Notification archived", response));
    }

    @GetMapping({"/unread-count", "/summary"})
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        UnreadCountResponse response = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved successfully", response));
    }
}
