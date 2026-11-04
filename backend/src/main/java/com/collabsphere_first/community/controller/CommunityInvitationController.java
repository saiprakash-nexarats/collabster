package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.community.dto.CreateInvitationRequest;
import com.collabsphere_first.community.dto.InvitationDto;
import com.collabsphere_first.community.enums.InvitationStatus;
import com.collabsphere_first.community.service.CommunityInvitationService;
import com.collabsphere_first.security.service.CurrentUserService;
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
@RequiredArgsConstructor
public class CommunityInvitationController {
    private final CommunityInvitationService invitationService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/v1/communities/{communityId}/invitations")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<InvitationDto>> create(@PathVariable UUID communityId,
                                                              @Valid @RequestBody CreateInvitationRequest request) {
        InvitationDto dto = invitationService.create(communityId, currentUserService.getCurrentUser().getId(), request.getInviteeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Invitation created", dto));
    }

    @GetMapping("/api/v1/communities/{communityId}/invitations")
    @PreAuthorize("hasPermission(#communityId, 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<InvitationDto>>> listForCommunity(@PathVariable UUID communityId,
            @RequestParam(defaultValue = "PENDING") InvitationStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(invitationService.listForCommunity(communityId, status, pageable)));
    }

    @GetMapping("/api/v1/invitations/me")
    public ResponseEntity<ApiResponse<Page<InvitationDto>>> mine(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(invitationService.listForUser(currentUserService.getCurrentUser().getId(), pageable)));
    }

    @PostMapping("/api/v1/invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<InvitationDto>> accept(@PathVariable UUID invitationId) {
        return ResponseEntity.ok(ApiResponse.success(invitationService.accept(invitationId, currentUserService.getCurrentUser().getId())));
    }

    @PostMapping("/api/v1/invitations/{invitationId}/decline")
    public ResponseEntity<ApiResponse<InvitationDto>> decline(@PathVariable UUID invitationId) {
        return ResponseEntity.ok(ApiResponse.success(invitationService.decline(invitationId, currentUserService.getCurrentUser().getId())));
    }

    @DeleteMapping("/api/v1/invitations/{invitationId}")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable UUID invitationId) {
        invitationService.revoke(invitationId, currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Invitation revoked", null));
    }
}
