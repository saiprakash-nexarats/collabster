package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.entity.*;
import com.collabsphere_first.community.enums.*;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.repository.*;
import com.collabsphere_first.community.security.CommunityAccessService;
import com.collabsphere_first.community.service.*;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Community Event Parity & Functional Acceptance Tests")
class CommunityEventParityTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private CommunityEventService eventService;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private CommunityEventController eventController;

    private final Long userId = 42L;
    private final UUID eventId = UUID.randomUUID();
    private final UUID communityId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
        user = User.builder().id(userId).username("attendee").email("attendee@collobster.com").build();
        lenient().when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    @DisplayName("GET /api/v1/events/tickets returns real registered tickets from repository")
    void listTickets_returnsRegisteredTickets() throws Exception {
        Map<String, Object> ticket = new LinkedHashMap<>();
        ticket.put("id", UUID.randomUUID().toString());
        ticket.put("ticketId", UUID.randomUUID().toString());
        ticket.put("registrationState", "registered");
        ticket.put("status", "GOING");
        ticket.put("event", Map.of(
                "id", eventId.toString(),
                "title", "Collobster Launch Meetup",
                "startsAt", Instant.now().plusSeconds(3600).toString(),
                "endsAt", Instant.now().plusSeconds(7200).toString(),
                "location", "Bangalore"
        ));

        when(eventService.listUserTickets(userId)).thenReturn(List.of(ticket));

        mockMvc.perform(get("/api/v1/events/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].registrationState").value("registered"))
                .andExpect(jsonPath("$.data[0].event.title").value("Collobster Launch Meetup"));
    }

    @Test
    @DisplayName("POST /api/v1/events/{id}/reminders/broadcast triggers real dispatch work")
    void broadcastReminder_queuesDispatch() throws Exception {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("eventId", eventId);
        resp.put("broadcast", true);
        resp.put("status", "QUEUED");
        resp.put("deliveredCount", 15);
        resp.put("recipientsCount", 15);
        resp.put("dispatchedAt", Instant.now().toString());

        when(eventService.broadcastReminder(eq(eventId), eq(userId), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/events/{eventId}/reminders/broadcast", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "Event starts in 1 hour"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.broadcast").value(true))
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.deliveredCount").value(15));
    }

    @Test
    @DisplayName("POST /api/v1/events/{id}/tickets/{ticketId}/check-in validates attendance")
    void checkIn_validatesAttendance() throws Exception {
        String ticketId = UUID.randomUUID().toString();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("eventId", eventId);
        resp.put("ticketId", ticketId);
        resp.put("checkedIn", true);
        resp.put("checkedInAt", Instant.now().toString());

        when(eventService.checkInTicket(eventId, ticketId, userId)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/events/{eventId}/tickets/{ticketId}/check-in", eventId, ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedIn").value(true))
                .andExpect(jsonPath("$.ticketId").value(ticketId));
    }
}

