package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.service.CommunityNotificationService;
import com.collabsphere_first.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CommunityNotificationController {

    private final CommunityNotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/v1/community/notifications")
    public ResponseEntity<ApiResponse<Page<CommunityNotificationDto>>> list(Pageable pageable) {
        Pageable bounded = PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.max(1, Math.min(pageable.getPageSize(), 100)),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.list(
                                currentUserService.getCurrentUser().getId(),
                                bounded
                        )
                )
        );
    }

    @GetMapping("/api/v1/community/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.unreadCount(
                                currentUserService.getCurrentUser().getId()
                        )
                )
        );
    }

    @PatchMapping("/api/v1/community/notifications/{id}/read")
    public ResponseEntity<ApiResponse<CommunityNotificationDto>> markRead(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.markRead(
                                id,
                                currentUserService.getCurrentUser().getId()
                        )
                )
        );
    }

    @PatchMapping("/api/v1/community/notifications/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllRead() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.markAllRead(
                                currentUserService.getCurrentUser().getId()
                        )
                )
        );
    }
}