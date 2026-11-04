package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.entity.CommunityEvent;
import com.collabsphere_first.community.entity.CommunityEventRsvp;
import com.collabsphere_first.community.enums.*;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.repository.*;
import com.collabsphere_first.community.security.CommunityAccessService;
import com.collabsphere_first.community.event.CommunityEventCreatedEvent;
import com.collabsphere_first.community.event.CommunityEventRsvpUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class CommunityEventService {
    private final CommunityEventRepository eventRepository;
    private final CommunityEventRsvpRepository rsvpRepository;
    private final CommunityAccessService accessService;
    private final CommunitySettingsService settingsService;
    private final CommunityAuditLogService auditLogService;
    private final CommunityEventOutboxService outboxService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommunityEventDto create(UUID communityId, Long userId, CreateCommunityEventRequest request) {
        accessService.requireRole(communityId, userId, MembershipRole.MODERATOR);
        if (!settingsService.getOrDefault(communityId).isEventsEnabled()) throw new InvalidJoinException("Community events are disabled");
        if (!request.getStartsAt().isAfter(Instant.now()) || !request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new InvalidJoinException("Event must start in the future and end after it starts");
        }
        CommunityEvent event = eventRepository.save(CommunityEvent.builder().community(accessService.requireCommunity(communityId))
                .createdBy(userId).title(request.getTitle().trim()).description(request.getDescription())
                .startsAt(request.getStartsAt()).endsAt(request.getEndsAt()).location(request.getLocation()).createdAt(Instant.now()).build());
        auditLogService.record(event.getCommunity(), userId, "EVENT_CREATED", "EVENT", event.getId().toString(), null);
        outboxService.record(communityId, "EVENT_CREATED", event.getId().toString(), java.util.Map.of("createdBy", userId));
        eventPublisher.publishEvent(new CommunityEventCreatedEvent(this, communityId, event.getId(), userId, event.getTitle()));
        return toDto(event, userId);
    }

    @Transactional(readOnly = true)
    public Page<CommunityEventDto> list(UUID communityId, Long userId, Pageable pageable) {
        requireReadableCommunity(communityId, userId);
        Pageable bounded = PageRequest.of(Math.max(0, pageable.getPageNumber()), Math.max(1, Math.min(pageable.getPageSize(), 100)),
                Sort.by(Sort.Direction.ASC, "startsAt"));
        return eventRepository.findByCommunityIdOrderByStartsAtAsc(communityId, bounded).map(event -> toDto(event, userId));
    }

    @Transactional(readOnly = true)
    public CommunityEventDto get(UUID eventId, Long userId) {
        CommunityEvent event = find(eventId);
        requireReadableCommunity(event.getCommunity().getId(), userId);
        return toDto(event, userId);
    }

    @Transactional
    public CommunityEventDto rsvp(UUID eventId, Long userId, EventRsvpRequest request) {
        CommunityEvent event = find(eventId);
        accessService.requireWritableMember(event.getCommunity().getId(), userId);
        if (event.getEndsAt().isBefore(Instant.now())) throw new InvalidJoinException("RSVP is closed for past events");
        if (!settingsService.getOrDefault(event.getCommunity().getId()).isEventsEnabled()) throw new InvalidJoinException("Community events are disabled");
        CommunityEventRsvp rsvp = rsvpRepository.findByEventIdAndUserId(eventId, userId).orElseGet(() ->
                CommunityEventRsvp.builder().event(event).userId(userId).build());
        rsvp.setStatus(request.getStatus());
        rsvp.setUpdatedAt(Instant.now());
        rsvpRepository.save(rsvp);
        outboxService.record(event.getCommunity().getId(), "EVENT_RSVP_UPDATED", eventId.toString(), java.util.Map.of("userId", userId, "status", request.getStatus().name()));
        eventPublisher.publishEvent(new CommunityEventRsvpUpdatedEvent(this, event.getCommunity().getId(), eventId,
                event.getCreatedBy(), userId, request.getStatus().name()));
        return toDto(event, userId);
    }

    @Transactional(readOnly = true)
    public Page<EventAttendeeDto> attendees(UUID eventId, Long userId, Pageable pageable) {
        CommunityEvent event = find(eventId);
        requireReadableCommunity(event.getCommunity().getId(), userId);
        Pageable bounded = PageRequest.of(Math.max(0, pageable.getPageNumber()), Math.max(1, Math.min(pageable.getPageSize(), 100)),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        return rsvpRepository.findByEventIdAndStatusOrderByUpdatedAtDesc(eventId, EventRsvpStatus.GOING, bounded)
                .map(r -> EventAttendeeDto.builder().userId(r.getUserId()).status(r.getStatus()).build());
    }

    private CommunityEvent find(UUID id) { return eventRepository.findById(id).orElseThrow(() -> new InvalidJoinException("Event not found")); }
    private void requireReadableCommunity(UUID communityId, Long userId) {
        var community = accessService.requireCommunity(communityId);
        if (community.getVisibility() != Visibility.PUBLIC || community.getCommunityStatus() != CommunityStatus.ACTIVE) {
            accessService.requireMember(communityId, userId);
        }
    }
    private CommunityEventDto toDto(CommunityEvent event, Long userId) {
        EventRsvpStatus myRsvp = rsvpRepository.findByEventIdAndUserId(event.getId(), userId).map(CommunityEventRsvp::getStatus).orElse(null);
        return CommunityEventDto.builder().id(event.getId()).communityId(event.getCommunity().getId()).createdBy(event.getCreatedBy())
                .title(event.getTitle()).description(event.getDescription()).startsAt(event.getStartsAt()).endsAt(event.getEndsAt())
                .location(event.getLocation()).createdAt(event.getCreatedAt())
                .goingCount(rsvpRepository.countByEventIdAndStatus(event.getId(), EventRsvpStatus.GOING))
                .maybeCount(rsvpRepository.countByEventIdAndStatus(event.getId(), EventRsvpStatus.MAYBE)).myRsvp(myRsvp).build();
    }

    @Transactional(readOnly = true)
public Page<CommunityEventDto> listGlobalEvents(
        Long userId,
        Instant startsFrom,
        Instant startsBefore,
        Pageable pageable
) {

    if (startsFrom != null
            && startsBefore != null
            && !startsBefore.isAfter(startsFrom)) {

        throw new InvalidJoinException(
                "The event end filter must be after the event start filter"
        );
    }

    Pageable bounded = PageRequest.of(
            Math.max(0, pageable.getPageNumber()),
            Math.max(1, Math.min(pageable.getPageSize(), 100)),
            Sort.by(
                    Sort.Order.asc("startsAt"),
                    Sort.Order.asc("createdAt")
            )
    );

    Instant now = Instant.now();

    Page<CommunityEvent> events;

    if (startsFrom == null && startsBefore == null) {

        events = eventRepository.findGlobalDiscoverableEvents(
                Visibility.PUBLIC,
                CommunityStatus.ACTIVE,
                now,
                bounded
        );

    } else if (startsFrom != null && startsBefore == null) {

        events = eventRepository.findGlobalDiscoverableEventsFrom(
                Visibility.PUBLIC,
                CommunityStatus.ACTIVE,
                now,
                startsFrom,
                bounded
        );

    } else if (startsFrom == null) {

        events = eventRepository.findGlobalDiscoverableEventsBefore(
                Visibility.PUBLIC,
                CommunityStatus.ACTIVE,
                now,
                startsBefore,
                bounded
        );

    } else {

        events = eventRepository.findGlobalDiscoverableEventsBetween(
                Visibility.PUBLIC,
                CommunityStatus.ACTIVE,
                now,
                startsFrom,
                startsBefore,
                bounded
        );
    }

    return events.map(event -> toDto(event, userId));
}

    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> listUserTickets(Long userId) {
        java.util.List<CommunityEventRsvp> rsvps = rsvpRepository.findByUserIdAndStatus(userId, EventRsvpStatus.GOING);
        return rsvps.stream().map(r -> {
            java.util.Map<String, Object> ticket = new java.util.LinkedHashMap<>();
            ticket.put("id", r.getId().toString());
            ticket.put("ticketId", r.getId().toString());
            ticket.put("registrationState", "registered");
            ticket.put("status", r.getStatus().name());
            ticket.put("registeredAt", r.getUpdatedAt().toString());
            ticket.put("event", java.util.Map.of(
                    "id", r.getEvent().getId().toString(),
                    "title", r.getEvent().getTitle(),
                    "startsAt", r.getEvent().getStartsAt().toString(),
                    "endsAt", r.getEvent().getEndsAt().toString(),
                    "location", r.getEvent().getLocation() != null ? r.getEvent().getLocation() : "Online"
            ));
            return ticket;
        }).toList();
    }

    @Transactional
    public java.util.Map<String, Object> broadcastReminder(UUID eventId, Long userId, java.util.Map<String, Object> body) {
        CommunityEvent event = find(eventId);
        accessService.requireRole(event.getCommunity().getId(), userId, MembershipRole.MODERATOR);
        long attendeeCount = rsvpRepository.countByEventIdAndStatus(eventId, EventRsvpStatus.GOING);

        outboxService.record(event.getCommunity().getId(), "EVENT_REMINDER_BROADCAST", eventId.toString(),
                java.util.Map.of("eventId", eventId.toString(), "broadcasterId", userId, "recipientsCount", attendeeCount, "scheduledAt", Instant.now().toString()));

        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("eventId", eventId);
        resp.put("broadcast", true);
        resp.put("status", "QUEUED");
        resp.put("deliveredCount", (int) attendeeCount);
        resp.put("recipientsCount", (int) attendeeCount);
        resp.put("dispatchedAt", Instant.now().toString());
        return resp;
    }

    @Transactional
    public java.util.Map<String, Object> checkInTicket(UUID eventId, String ticketId, Long userId) {
        CommunityEvent event = find(eventId);
        accessService.requireRole(event.getCommunity().getId(), userId, MembershipRole.MODERATOR);
        UUID rsvpId;
        try {
            rsvpId = UUID.fromString(ticketId);
        } catch (IllegalArgumentException e) {
            throw new InvalidJoinException("Invalid ticket identifier");
        }
        CommunityEventRsvp rsvp = rsvpRepository.findById(rsvpId)
                .orElseThrow(() -> new InvalidJoinException("Ticket not found"));
        if (!rsvp.getEvent().getId().equals(eventId)) {
            throw new InvalidJoinException("Ticket does not match the event");
        }
        if (rsvp.getStatus() != EventRsvpStatus.GOING) {
            throw new InvalidJoinException("Ticket is not registered as going");
        }
        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("eventId", eventId);
        resp.put("ticketId", ticketId);
        resp.put("checkedIn", true);
        resp.put("checkedInAt", Instant.now().toString());
        return resp;
    }
}
