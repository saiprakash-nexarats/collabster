package com.collabsphere_first.community.controller;

import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.community.dto.CreateResourceRequest;
import com.collabsphere_first.community.dto.ResourceDto;
import com.collabsphere_first.community.service.ResourceService;
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
@RequestMapping("/api/v1/communities/{id}/resources")
@RequiredArgsConstructor
@Tag(name = "Resources API", description = "Endpoints for community resources (files, links, wikis)")
public class ResourceController {

    private final ResourceService resourceService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @PreAuthorize("hasPermission(#id, 'MEMBER')")
    @Operation(summary = "Upload or link a community resource (Requires MEMBER role)")
    public ResponseEntity<ApiResponse<ResourceDto>> createResource(
            @PathVariable UUID id,
            @Valid @RequestBody CreateResourceRequest request) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ResourceDto created = resourceService.createResource(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Resource created successfully", created));
    }

    @GetMapping
    @Operation(summary = "List resources for a community")
    public ResponseEntity<ApiResponse<Page<ResourceDto>>> listResources(
            @PathVariable UUID id,
            Pageable pageable) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        Page<ResourceDto> resources = resourceService.getCommunityResources(id, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(resources));
    }
}
