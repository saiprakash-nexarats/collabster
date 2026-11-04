package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.enums.EventRsvpStatus;
import com.collabsphere_first.community.service.CommunityEventService;
import com.collabsphere_first.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class CommunityEventController {
    private final CommunityEventService eventService;
    private final CurrentUserService currentUserService;

    @GetMapping({"/api/v1/communities/{communityId}/events", "/v1/communities/{communityId}/events"})
    public ResponseEntity<ApiResponse<Page<CommunityEventDto>>> list(@PathVariable UUID communityId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(eventService.list(communityId, currentUserService.getCurrentUser().getId(), pageable)));
    }

    @PostMapping({"/api/v1/communities/{communityId}/events", "/v1/communities/{communityId}/events"})
    @PreAuthorize("hasPermission(#communityId, 'MODERATOR')")
    public ResponseEntity<ApiResponse<CommunityEventDto>> create(@PathVariable UUID communityId, @Valid @RequestBody CreateCommunityEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Community event created",
                eventService.create(communityId, currentUserService.getCurrentUser().getId(), request)));
    }

    @GetMapping({"/api/v1/events", "/v1/events"})
    public ResponseEntity<ApiResponse<Page<CommunityEventDto>>> listGlobalEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startsFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startsBefore,
            Pageable pageable
    ) {
        Long userId = null;
        try {
            userId = currentUserService.getCurrentUser().getId();
        } catch (Exception ignored) {}

        Page<CommunityEventDto> events = eventService.listGlobalEvents(
                userId != null ? userId : 0L,
                startsFrom,
                startsBefore,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping({"/api/v1/events/tickets", "/v1/events/tickets"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listTickets() {
        return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
    }

    @GetMapping({"/api/v1/events/{eventId}", "/v1/events/{eventId}"})
    public ResponseEntity<ApiResponse<CommunityEventDto>> get(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(eventService.get(eventId, currentUserService.getCurrentUser().getId())));
    }

    @PutMapping({"/api/v1/events/{eventId}/rsvp", "/v1/events/{eventId}/rsvp"})
    public ResponseEntity<ApiResponse<CommunityEventDto>> rsvp(@PathVariable UUID eventId, @Valid @RequestBody EventRsvpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(eventService.rsvp(eventId, currentUserService.getCurrentUser().getId(), request)));
    }

    @PostMapping({"/api/v1/events/{eventId}/registrations", "/v1/events/{eventId}/registrations"})
    public ResponseEntity<ApiResponse<CommunityEventDto>> register(@PathVariable UUID eventId) {
        EventRsvpRequest request = new EventRsvpRequest();
        request.setStatus(EventRsvpStatus.GOING);
        return ResponseEntity.ok(ApiResponse.success(eventService.rsvp(eventId, currentUserService.getCurrentUser().getId(), request)));
    }

    @PostMapping({"/api/v1/events/{eventId}/registrations/cancel", "/v1/events/{eventId}/registrations/cancel"})
    public ResponseEntity<ApiResponse<CommunityEventDto>> cancelRegistration(@PathVariable UUID eventId) {
        EventRsvpRequest request = new EventRsvpRequest();
        request.setStatus(EventRsvpStatus.DECLINED);
        return ResponseEntity.ok(ApiResponse.success(eventService.rsvp(eventId, currentUserService.getCurrentUser().getId(), request)));
    }

    @PostMapping({"/api/v1/events/{eventId}/tickets/{ticketId}/check-in", "/v1/events/{eventId}/tickets/{ticketId}/check-in"})
    public ResponseEntity<Map<String, Object>> checkIn(@PathVariable UUID eventId, @PathVariable String ticketId) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("eventId", eventId);
        resp.put("ticketId", ticketId);
        resp.put("checkedIn", true);
        resp.put("checkedInAt", Instant.now().toString());
        return ResponseEntity.ok(resp);
    }

    @GetMapping({"/api/v1/events/{eventId}/attendees", "/v1/events/{eventId}/attendees"})
    public ResponseEntity<ApiResponse<Page<EventAttendeeDto>>> attendees(@PathVariable UUID eventId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(eventService.attendees(eventId, currentUserService.getCurrentUser().getId(), pageable)));
    }
}
