package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.service.CommunityPollService;
import com.collabsphere_first.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CommunityPollController {
    private final CommunityPollService pollService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/v1/communities/{communityId}/polls")
    public ResponseEntity<ApiResponse<CommunityPollDto>> create(@PathVariable UUID communityId, @Valid @RequestBody CreatePollRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Poll created", pollService.create(communityId,
                currentUserService.getCurrentUser().getId(), request)));
    }

    @GetMapping("/api/v1/communities/{communityId}/polls")
    public ResponseEntity<ApiResponse<Page<CommunityPollDto>>> list(@PathVariable UUID communityId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(pollService.list(communityId, currentUserService.getCurrentUser().getId(), pageable)));
    }

    @GetMapping("/api/v1/polls/{pollId}")
    public ResponseEntity<ApiResponse<CommunityPollDto>> get(@PathVariable UUID pollId) {
        return ResponseEntity.ok(ApiResponse.success(pollService.get(pollId, currentUserService.getCurrentUser().getId())));
    }

    @PostMapping("/api/v1/polls/{pollId}/vote")
    public ResponseEntity<ApiResponse<CommunityPollDto>> vote(@PathVariable UUID pollId, @Valid @RequestBody VotePollRequest request) {
        return ResponseEntity.ok(ApiResponse.success(pollService.vote(pollId, currentUserService.getCurrentUser().getId(), request)));
    }

    @PostMapping("/api/v1/polls/{pollId}/close")
    public ResponseEntity<ApiResponse<CommunityPollDto>> close(@PathVariable UUID pollId) {
        return ResponseEntity.ok(ApiResponse.success(pollService.close(pollId, currentUserService.getCurrentUser().getId())));
    }
}
