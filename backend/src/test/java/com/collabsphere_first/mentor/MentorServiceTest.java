package com.collabsphere_first.mentor;

import com.collabsphere_first.community.exception.ResourceNotFoundException;
import com.collabsphere_first.mentor.dto.*;
import com.collabsphere_first.mentor.entity.*;
import com.collabsphere_first.mentor.repository.*;
import com.collabsphere_first.mentor.service.impl.MentorServiceImpl;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorServiceTest {

    @Mock
    private MentorProfileRepository profileRepository;
    @Mock
    private MentorSlotRepository slotRepository;
    @Mock
    private MentorSessionRepository sessionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MentorServiceImpl mentorService;

    private Long mentorId;
    private Long learnerId;
    private UUID slotId;

    @BeforeEach
    void setUp() {
        mentorId = 201L;
        learnerId = 301L;
        slotId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Upsert profile creates or updates mentor profile")
    void testUpsertProfile() {
        UpsertMentorProfileRequest request = UpsertMentorProfileRequest.builder()
                .headline("Staff Software Engineer @ Tech Corp")
                .bio("10+ years in distributed databases and cloud platforms")
                .hourlyRate(new BigDecimal("120.00"))
                .currency("USD")
                .timezone("UTC")
                .skills("Java, PostgreSQL, Redis")
                .build();

        when(profileRepository.findByUserId(mentorId)).thenReturn(Optional.empty());
        when(profileRepository.save(any(MentorProfile.class))).thenAnswer(i -> {
            MentorProfile p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        MentorProfileDto dto = mentorService.upsertProfile(request, mentorId);
        assertThat(dto).isNotNull();
        assertThat(dto.getHeadline()).isEqualTo("Staff Software Engineer @ Tech Corp");
        assertThat(dto.getSkills()).contains("Java", "PostgreSQL");
    }

    @Test
    @DisplayName("Create slot adds available slot for mentor")
    void testCreateSlot() {
        Instant time = Instant.now().plusSeconds(86400);
        CreateMentorSlotRequest request = CreateMentorSlotRequest.builder()
                .slotTime(time)
                .durationMinutes(45)
                .build();

        when(slotRepository.save(any(MentorSlot.class))).thenAnswer(i -> {
            MentorSlot s = i.getArgument(0);
            s.setId(slotId);
            return s;
        });

        MentorSlotDto dto = mentorService.createSlot(request, mentorId);
        assertThat(dto.getId()).isEqualTo(slotId);
        assertThat(dto.getBooked()).isFalse();
        assertThat(dto.getDurationMinutes()).isEqualTo(45);
    }

    @Test
    @DisplayName("Book slot prevents double booking and records session")
    void testBookSlotSuccessAndDoubleBookingPrevention() {
        Instant time = Instant.now().plusSeconds(86400);
        MentorSlot availableSlot = MentorSlot.builder()
                .id(slotId)
                .mentorId(mentorId)
                .slotTime(time)
                .durationMinutes(30)
                .booked(false)
                .build();

        when(slotRepository.findById(slotId)).thenReturn(Optional.of(availableSlot));
        when(slotRepository.reserveSlotAtomically(eq(slotId), any(Instant.class))).thenReturn(1);
        when(sessionRepository.save(any(MentorSession.class))).thenAnswer(i -> {
            MentorSession s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        BookMentorSlotRequest request = BookMentorSlotRequest.builder()
                .sessionType("System Architecture")
                .note("Review high availability setup")
                .build();

        Map<String, Object> result = mentorService.bookSlot(slotId, request, learnerId);
        assertThat(result).containsKey("session");
        assertThat(result).containsKey("slot");
        assertThat(availableSlot.getBooked()).isTrue();

        // Attempting to book again when slot.booked == true fails
        assertThatThrownBy(() -> mentorService.bookSlot(slotId, request, learnerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been booked");

        // Concurrent race condition: slot appears unbooked but atomic update returns 0 rows updated
        availableSlot.setBooked(false);
        when(slotRepository.reserveSlotAtomically(eq(slotId), any(Instant.class))).thenReturn(0);
        assertThatThrownBy(() -> mentorService.bookSlot(slotId, request, learnerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been booked");
    }

    @Test
    @DisplayName("Confirm session allows only mentor to confirm")
    void testConfirmSessionAuthorization() {
        UUID sessionId = UUID.randomUUID();
        MentorSession session = MentorSession.builder()
                .id(sessionId)
                .mentorId(mentorId)
                .learnerId(learnerId)
                .state("requested")
                .startsAt(Instant.now().plusSeconds(3600))
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // Learner cannot confirm
        assertThatThrownBy(() -> mentorService.confirmSession(sessionId, learnerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the mentor can confirm this session");

        // Mentor can confirm
        when(sessionRepository.save(any(MentorSession.class))).thenAnswer(i -> i.getArgument(0));
        MentorSessionDto confirmed = mentorService.confirmSession(sessionId, mentorId);
        assertThat(confirmed.getState()).isEqualTo("confirmed");
    }

    @Test
    @DisplayName("Cancel session releases slot if linked and updates state")
    void testCancelSession() {
        UUID sessionId = UUID.randomUUID();
        MentorSession session = MentorSession.builder()
                .id(sessionId)
                .mentorId(mentorId)
                .learnerId(learnerId)
                .slotId(slotId)
                .state("confirmed")
                .startsAt(Instant.now().plusSeconds(3600))
                .build();

        MentorSlot slot = MentorSlot.builder().id(slotId).mentorId(mentorId).booked(true).build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(sessionRepository.save(any(MentorSession.class))).thenAnswer(i -> i.getArgument(0));

        MentorSessionDto cancelled = mentorService.cancelSession(sessionId, learnerId);
        assertThat(cancelled.getState()).isEqualTo("cancelled");
        assertThat(slot.getBooked()).isFalse();
        verify(slotRepository).save(slot);
    }
}
