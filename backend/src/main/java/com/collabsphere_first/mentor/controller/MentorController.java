package com.collabsphere_first.mentor.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.mentor.dto.*;
import com.collabsphere_first.mentor.service.MentorService;
import com.collabsphere_first.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/v1/mentors", "/api/v1/mentors"})
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MentorProfileDto>>> listMentors() {
        List<MentorProfileDto> mentors = mentorService.listMentors();
        return ResponseEntity.ok(ApiResponse.success(mentors));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<MentorSessionDto>>> listSessions() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<MentorSessionDto> sessions = mentorService.listUserSessions(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<MentorProfileDto>> upsertProfile(
            @Valid @RequestBody UpsertMentorProfileRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        MentorProfileDto profile = mentorService.upsertProfile(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/{mentorId}/sessions")
    public ResponseEntity<ApiResponse<MentorSessionDto>> requestSession(
            @PathVariable Long mentorId,
            @Valid @RequestBody RequestSessionRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        MentorSessionDto session = mentorService.requestSession(mentorId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(session));
    }

    @PostMapping("/sessions/{sessionId}/confirm")
    public ResponseEntity<ApiResponse<MentorSessionDto>> confirmSession(
            @PathVariable UUID sessionId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        MentorSessionDto session = mentorService.confirmSession(sessionId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    public ResponseEntity<ApiResponse<MentorSessionDto>> cancelSession(
            @PathVariable UUID sessionId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        MentorSessionDto session = mentorService.cancelSession(sessionId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @GetMapping("/{mentorId}/slots")
    public ResponseEntity<ApiResponse<List<MentorSlotDto>>> listSlots(
            @PathVariable Long mentorId
    ) {
        List<MentorSlotDto> slots = mentorService.listSlots(mentorId);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    @PostMapping("/slots")
    public ResponseEntity<ApiResponse<MentorSlotDto>> createSlot(
            @Valid @RequestBody CreateMentorSlotRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        MentorSlotDto slot = mentorService.createSlot(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(slot));
    }

    @PostMapping("/slots/{slotId}/book")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bookSlot(
            @PathVariable UUID slotId,
            @Valid @RequestBody BookMentorSlotRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        Map<String, Object> result = mentorService.bookSlot(slotId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
