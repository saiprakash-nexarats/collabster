package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.service.CommunityRuleService;
import com.collabsphere_first.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequiredArgsConstructor
public class CommunityRuleController {
    private final CommunityRuleService ruleService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/v1/communities/{communityId}/rules")
    public ResponseEntity<ApiResponse<List<CommunityRuleDto>>> list(@PathVariable UUID communityId) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.list(communityId, currentUserService.getCurrentUser().getId())));
    }
    @PostMapping("/api/v1/communities/{communityId}/rules")
    @PreAuthorize("hasPermission(#communityId, 'ADMIN')")
    public ResponseEntity<ApiResponse<CommunityRuleDto>> create(@PathVariable UUID communityId, @Valid @RequestBody CreateCommunityRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Rule created", ruleService.create(communityId,
                currentUserService.getCurrentUser().getId(), request)));
    }
    @PatchMapping("/api/v1/communities/{communityId}/rules/{ruleId}")
    @PreAuthorize("hasPermission(#communityId, 'ADMIN')")
    public ResponseEntity<ApiResponse<CommunityRuleDto>> update(@PathVariable UUID communityId, @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateCommunityRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.update(communityId, ruleId, currentUserService.getCurrentUser().getId(), request)));
    }
    @DeleteMapping("/api/v1/communities/{communityId}/rules/{ruleId}")
    @PreAuthorize("hasPermission(#communityId, 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID communityId, @PathVariable UUID ruleId) {
        ruleService.deactivate(communityId, ruleId, currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Rule deactivated", null));
    }
    @GetMapping("/api/v1/communities/{communityId}/rules/{ruleId}/versions")
    public ResponseEntity<ApiResponse<List<CommunityRuleRevisionDto>>> revisions(@PathVariable UUID communityId, @PathVariable UUID ruleId) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.revisions(communityId, ruleId, currentUserService.getCurrentUser().getId())));
    }
}
