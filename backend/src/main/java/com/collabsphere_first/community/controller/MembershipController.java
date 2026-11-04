package com.collabsphere_first.community.controller;

import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.community.dto.MembershipDto;
import com.collabsphere_first.community.dto.UpdateMembershipRequest;
import com.collabsphere_first.community.service.MembershipService;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/communities/{id}", "/v1/communities/{id}"})
@RequiredArgsConstructor
@Tag(name = "Membership API", description = "Endpoints for community membership management")
public class MembershipController {

    private final MembershipService membershipService;
    private final CurrentUserService currentUserService;

    @PostMapping("/join")
    @Operation(summary = "Join or request to join a community")
    public ResponseEntity<ApiResponse<MembershipDto>> joinCommunity(
            @PathVariable UUID id,
            @RequestParam(required = false) String inviteToken) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        MembershipDto membership = membershipService.joinCommunity(id, currentUserId, inviteToken);
        return ResponseEntity.ok(ApiResponse.success("Joined community successfully", membership));
    }

    @RequestMapping(value = {"/leave", "/join"}, method = RequestMethod.DELETE)
    @Operation(summary = "Leave a community (Owners cannot leave without transferring ownership)")
    public ResponseEntity<ApiResponse<Void>> leaveCommunity(@PathVariable UUID id) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        membershipService.leaveCommunity(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Left community successfully", null));
    }

    @PatchMapping("/members/{userId}")
    @PreAuthorize("hasPermission(#id, 'ADMIN')")
    @Operation(summary = "Change member role or status (Requires ADMIN role or higher)")
    public ResponseEntity<ApiResponse<MembershipDto>> updateMembership(
            @PathVariable UUID id,
            @PathVariable Long userId,
            @RequestBody UpdateMembershipRequest request) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        MembershipDto updated = membershipService.updateMembership(id, userId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Membership updated successfully", updated));
    }

    @PostMapping("/members/{userId}/approve")
    @PreAuthorize("hasPermission(#id, 'MODERATOR')")
    public ResponseEntity<ApiResponse<MembershipDto>> approveJoinRequest(@PathVariable UUID id, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.reviewJoinRequest(id, userId, true,
                currentUserService.getCurrentUser().getId())));
    }

    @PostMapping("/members/{userId}/reject")
    @PreAuthorize("hasPermission(#id, 'MODERATOR')")
    public ResponseEntity<ApiResponse<MembershipDto>> rejectJoinRequest(@PathVariable UUID id, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.reviewJoinRequest(id, userId, false,
                currentUserService.getCurrentUser().getId())));
    }

    @GetMapping("/members")
    @PreAuthorize("hasPermission(#id, 'MEMBER')")
    @Operation(summary = "List community members")
    public ResponseEntity<ApiResponse<Page<MembershipDto>>> listMembers(
            @PathVariable UUID id,
            @RequestParam(required = false) MembershipStatus status,
            Pageable pageable) {
        Page<MembershipDto> members = membershipService.listMembers(id, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(members));
    }
}
