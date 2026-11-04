package com.collabsphere_first.community.controller;

import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.community.dto.CommunityDto;
import com.collabsphere_first.community.dto.CommunitySearchQuery;
import com.collabsphere_first.community.dto.CreateCommunityRequest;
import com.collabsphere_first.community.dto.UpdateCommunityRequest;
import com.collabsphere_first.community.enums.CommunityType;
import com.collabsphere_first.community.enums.CommunityCategory;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.service.CommunityService;
import com.collabsphere_first.community.service.DiscoveryService;
import com.collabsphere_first.community.service.MembershipService;
import com.collabsphere_first.community.service.CommunityLifecycleService;
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
@RequestMapping({"/api/v1/communities", "/v1/communities"})
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Communities API", description = "Endpoints for managing and discovering communities")
public class CommunityController {

    private final CommunityService communityService;
    private final DiscoveryService discoveryService;
    private final CurrentUserService currentUserService;
    private final MembershipService membershipService;
    private final CommunityLifecycleService lifecycleService;

    @GetMapping("/mine")
    @Operation(summary = "List communities associated with the authenticated user")
    public ResponseEntity<ApiResponse<Page<com.collabsphere_first.community.dto.MyCommunityDto>>> myCommunities(Pageable pageable) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(ApiResponse.success(membershipService.listMyCommunities(userId, pageable)));
    }

    @GetMapping
    @Operation(summary = "Browse, search, and filter communities")
    public ResponseEntity<ApiResponse<Page<CommunityDto>>> searchCommunities(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) CommunityType type,
            @RequestParam(required = false) CommunityCategory category,
            @RequestParam(required = false) Visibility visibility,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        CommunitySearchQuery searchQuery = CommunitySearchQuery.builder()
                .query(query)
                .type(type)
                .category(category)
                .visibility(visibility)
                .tag(tag)
                .page(page)
                .size(size)
                .build();

        Page<CommunityDto> result = discoveryService.searchCommunities(searchQuery);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending communities by decayed activity score")
    public ResponseEntity<ApiResponse<Page<CommunityDto>>> getTrendingCommunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CommunityDto> result = discoveryService.getTrendingCommunities(page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get personalized public-community recommendations")
    public ResponseEntity<ApiResponse<Page<CommunityDto>>> recommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.recommendCommunities(
                currentUserService.getCurrentUser().getId(), page, size)));
    }

    @PostMapping
    @Operation(summary = "Create a new community")
    public ResponseEntity<ApiResponse<CommunityDto>> createCommunity(@Valid @RequestBody CreateCommunityRequest request) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CommunityDto created = communityService.createCommunity(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Community created successfully", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch community metadata by ID")
    public ResponseEntity<ApiResponse<CommunityDto>> getCommunity(@PathVariable UUID id) {
        CommunityDto community = communityService.getCommunity(id, currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success(community));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Fetch community metadata by unique slug")
    public ResponseEntity<ApiResponse<CommunityDto>> getCommunityBySlug(@PathVariable String slug) {
        CommunityDto community = communityService.getCommunityBySlug(slug, currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success(community));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'ADMIN')")
    @Operation(summary = "Edit community settings (Requires ADMIN role or higher)")
    public ResponseEntity<ApiResponse<CommunityDto>> updateCommunity(
            @PathVariable UUID id,
            @RequestBody UpdateCommunityRequest request) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CommunityDto updated = communityService.updateCommunity(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Community updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'OWNER')")
    @Operation(summary = "Archive community (Requires OWNER role or Core Admin)")
    public ResponseEntity<ApiResponse<Void>> archiveCommunity(@PathVariable UUID id) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        communityService.archiveCommunity(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Community archived successfully", null));
    }

    @PostMapping("/{id}/transfer-ownership")
    @PreAuthorize("hasPermission(#id, 'OWNER')")
    @Operation(summary = "Transfer community ownership to an active member")
    public ResponseEntity<ApiResponse<Void>> transferOwnership(@PathVariable UUID id, @RequestParam Long targetUserId) {
        membershipService.transferOwnership(id, targetUserId, currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Community ownership transferred", null));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasPermission(#id, 'OWNER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable UUID id) {
        lifecycleService.restore(id, currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Community restored", null));
    }

    @PostMapping("/{id}/request-deletion")
    @PreAuthorize("hasPermission(#id, 'OWNER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> requestDeletion(@PathVariable UUID id) {
        lifecycleService.requestDeletion(id, currentUserService.getCurrentUser().getId());
        return ResponseEntity.accepted().body(ApiResponse.success("Community deletion scheduled for 30 days from now", null));
    }

    @PostMapping("/{id}/cancel-deletion")
    @PreAuthorize("hasPermission(#id, 'OWNER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelDeletion(@PathVariable UUID id) {
        lifecycleService.cancelDeletion(id, currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Community deletion cancelled", null));
    }
}
