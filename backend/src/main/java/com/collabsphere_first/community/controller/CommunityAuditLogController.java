package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.community.dto.CommunityAuditLogDto;
import com.collabsphere_first.community.service.CommunityAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CommunityAuditLogController {
    private final CommunityAuditLogService auditLogService;

    @GetMapping("/api/v1/communities/{communityId}/audit-logs")
    @PreAuthorize("hasPermission(#communityId, 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<CommunityAuditLogDto>>> list(@PathVariable UUID communityId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.list(communityId, pageable)));
    }
}
