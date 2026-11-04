package com.collabsphere_first.community.service;

import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.entity.*;
import com.collabsphere_first.community.enums.*;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.repository.*;
import com.collabsphere_first.community.security.CommunityAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityPollsAndEventsTest {

    @Mock private CommunityPollRepository pollRepository;
    @Mock private CommunityPollOptionRepository optionRepository;
    @Mock private CommunityPollVoteRepository voteRepository;
    @Mock private CommunityEventRepository eventRepository;
    @Mock private CommunityEventRsvpRepository rsvpRepository;
    @Mock private CommunityAccessService accessService;
    @Mock private CommunitySettingsService settingsService;
    @Mock private CommunityAuditLogService auditLogService;
    @Mock private CommunityEventOutboxService outboxService;
    @Mock private CommunityNotificationService notificationService;
    @Mock private CommunitiesRateLimitConfig rateLimitConfig;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CommunityPollService pollService;
    private CommunityEventService eventService;

    private UUID communityId;
    private Community community;
    private Long userId = 201L;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        community = Community.builder().id(communityId).communityStatus(CommunityStatus.ACTIVE).visibility(Visibility.PUBLIC).build();
        pollService = new CommunityPollService(pollRepository, optionRepository, voteRepository,
                accessService, settingsService, outboxService, rateLimitConfig, notificationService, eventPublisher);
        eventService = new CommunityEventService(eventRepository, rsvpRepository, accessService,
                settingsService, auditLogService, outboxService, eventPublisher);
    }

    @Test
    @DisplayName("Poll creation requires unique non-empty options")
    void pollCreation_requiresUniqueOptions() {
        when(rateLimitConfig.consumePoll(userId)).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
        when(accessService.requireWritableMember(communityId, userId)).thenReturn(Membership.builder().role(MembershipRole.ADMIN).build());
        when(settingsService.getOrDefault(communityId)).thenReturn(CommunitySettings.builder().pollsEnabled(true).pollCreationRole(MembershipRole.MEMBER).build());

        CreatePollRequest request = new CreatePollRequest();
        request.setQuestion("Favorite language?");
        request.setOptions(List.of("Java", "Java")); // Duplicate option

        assertThatThrownBy(() -> pollService.create(communityId, userId, request))
                .isInstanceOf(InvalidJoinException.class)
                .hasMessageContaining("unique");
    }

    @Test
    @DisplayName("Duplicate vote on single-choice poll is prevented")
    void duplicateVote_isPrevented() {
        UUID pollId = UUID.randomUUID();
        CommunityPoll poll = CommunityPoll.builder().id(pollId).community(community).allowMultiple(false).build();
        when(rateLimitConfig.consumePoll(userId)).thenReturn(new CommunitiesRateLimitConfig.Decision(true, 0));
        when(pollRepository.findByIdForUpdate(pollId)).thenReturn(Optional.of(poll));
        when(settingsService.getOrDefault(communityId)).thenReturn(CommunitySettings.builder().pollsEnabled(true).build());
        when(voteRepository.existsByPollIdAndUserId(pollId, userId)).thenReturn(true);

        VotePollRequest request = new VotePollRequest();
        request.setOptionIds(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> pollService.vote(pollId, userId, request))
                .isInstanceOf(InvalidJoinException.class)
                .hasMessageContaining("already voted");
    }

    @Test
    @DisplayName("Event creation requires valid future dates and moderator rank")
    void eventCreation_validatesDatesAndRole() {
        when(settingsService.getOrDefault(communityId)).thenReturn(CommunitySettings.builder().eventsEnabled(true).build());

        CreateCommunityEventRequest invalidDateReq = new CreateCommunityEventRequest();
        invalidDateReq.setTitle("Meeting");
        invalidDateReq.setStartsAt(Instant.now().minus(1, ChronoUnit.HOURS)); // Past date
        invalidDateReq.setEndsAt(Instant.now().plus(1, ChronoUnit.HOURS));

        assertThatThrownBy(() -> eventService.create(communityId, userId, invalidDateReq))
                .isInstanceOf(InvalidJoinException.class)
                .hasMessageContaining("future");
    }

    @Test
    @DisplayName("Event RSVP saves status and publishes RSVP update event")
    void eventRsvp_savesAndPublishesEvent() {
        UUID eventId = UUID.randomUUID();
        CommunityEvent event = CommunityEvent.builder()
                .id(eventId)
                .community(community)
                .createdBy(100L)
                .title("Workshop")
                .startsAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .endsAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(accessService.requireWritableMember(communityId, userId)).thenReturn(null);
        when(settingsService.getOrDefault(communityId)).thenReturn(CommunitySettings.builder().eventsEnabled(true).build());
        when(rsvpRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());
        when(rsvpRepository.save(any(CommunityEventRsvp.class))).thenAnswer(i -> i.getArgument(0));

        EventRsvpRequest rsvpReq = new EventRsvpRequest();
        rsvpReq.setStatus(EventRsvpStatus.GOING);

        eventService.rsvp(eventId, userId, rsvpReq);

        verify(rsvpRepository).save(any(CommunityEventRsvp.class));
        verify(outboxService).record(eq(communityId), eq("EVENT_RSVP_UPDATED"), eq(eventId.toString()), anyMap());
        verify(eventPublisher).publishEvent(any());
    }
}
