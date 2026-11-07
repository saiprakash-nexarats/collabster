package com.collabsphere_first.mentor.service.impl;

import com.collabsphere_first.mentor.dto.*;
import com.collabsphere_first.mentor.entity.*;
import com.collabsphere_first.mentor.repository.*;
import com.collabsphere_first.mentor.service.MentorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentorServiceImpl implements MentorService {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorSlotRepository mentorSlotRepository;
    private final MentorSessionRepository mentorSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MentorProfileDto> listMentors() {
        return mentorProfileRepository.findByStateOrderByCreatedAtDesc("active").stream()
                .map(this::toProfileDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorSessionDto> listUserSessions(Long userId) {
        return mentorSessionRepository.findByMentorIdOrLearnerIdOrderByStartsAtDesc(userId, userId).stream()
                .map(this::toSessionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MentorProfileDto upsertProfile(UpsertMentorProfileRequest request, Long userId) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .orElseGet(() -> MentorProfile.builder().userId(userId).build());

        profile.setHeadline(request.getHeadline().trim());
        profile.setBio(request.getBio());
        profile.setHourlyRate(request.getHourlyRate() != null ? request.getHourlyRate() : BigDecimal.ZERO);
        profile.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        profile.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");
        profile.setSkills(request.getSkills());
        profile.setState("active");

        MentorProfile saved = mentorProfileRepository.save(profile);
        return toProfileDto(saved);
    }

    @Override
    @Transactional
    public MentorSessionDto requestSession(Long mentorId, RequestSessionRequest request, Long learnerId) {
        MentorSession session = MentorSession.builder()
                .mentorId(mentorId)
                .learnerId(learnerId)
                .sessionType(request.getSessionType() != null ? request.getSessionType() : "1:1 mentoring")
                .note(request.getNote())
                .startsAt(request.getStartsAt())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 30)
                .state("requested")
                .build();

        MentorSession saved = mentorSessionRepository.save(session);
        return toSessionDto(saved);
    }

    @Override
    @Transactional
    public MentorSessionDto confirmSession(UUID sessionId, Long userId) {
        MentorSession session = mentorSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));

        if (!session.getMentorId().equals(userId)) {
            throw new AccessDeniedException("Only the mentor can confirm this session");
        }

        session.setState("confirmed");
        return toSessionDto(mentorSessionRepository.save(session));
    }

    @Override
    @Transactional
    public MentorSessionDto cancelSession(UUID sessionId, Long userId) {
        MentorSession session = mentorSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));

        if (!session.getMentorId().equals(userId) && !session.getLearnerId().equals(userId)) {
            throw new AccessDeniedException("You are not a participant of this mentoring session");
        }

        session.setState("cancelled");
        if (session.getSlotId() != null) {
            mentorSlotRepository.findById(session.getSlotId()).ifPresent(slot -> {
                slot.setBooked(false);
                mentorSlotRepository.save(slot);
            });
        }

        return toSessionDto(mentorSessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorSlotDto> listSlots(Long mentorId) {
        return mentorSlotRepository.findByMentorIdOrderBySlotTimeAsc(mentorId).stream()
                .map(this::toSlotDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MentorSlotDto createSlot(CreateMentorSlotRequest request, Long mentorId) {
        MentorSlot slot = MentorSlot.builder()
                .mentorId(mentorId)
                .slotTime(request.getSlotTime())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 30)
                .booked(false)
                .build();

        MentorSlot saved = mentorSlotRepository.save(slot);
        return toSlotDto(saved);
    }

    @Override
    @Transactional
    public Map<String, Object> bookSlot(UUID slotId, BookMentorSlotRequest request, Long learnerId) {
        MentorSlot slot = mentorSlotRepository.findById(slotId)
                .orElseThrow(() -> new NoSuchElementException("Slot not found: " + slotId));

        if (Boolean.TRUE.equals(slot.getBooked())) {
            throw new IllegalStateException("This slot has already been booked");
        }

        slot.setBooked(true);
        mentorSlotRepository.save(slot);

        MentorSession session = MentorSession.builder()
                .mentorId(slot.getMentorId())
                .learnerId(learnerId)
                .slotId(slot.getId())
                .sessionType(request.getSessionType() != null ? request.getSessionType() : "1:1 mentoring")
                .note(request.getNote())
                .startsAt(slot.getSlotTime())
                .durationMinutes(slot.getDurationMinutes())
                .state("confirmed")
                .build();

        MentorSession savedSession = mentorSessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("session", toSessionDto(savedSession));
        result.put("slot", toSlotDto(slot));
        return result;
    }

    private MentorProfileDto toProfileDto(MentorProfile profile) {
        return MentorProfileDto.builder()
                .id(profile.getId())
                .profileId(profile.getUserId())
                .headline(profile.getHeadline())
                .bio(profile.getBio())
                .hourlyRate(profile.getHourlyRate())
                .currency(profile.getCurrency())
                .timezone(profile.getTimezone())
                .skills(profile.getSkills())
                .state(profile.getState())
                .build();
    }

    private MentorSlotDto toSlotDto(MentorSlot slot) {
        return MentorSlotDto.builder()
                .id(slot.getId())
                .mentorId(slot.getMentorId())
                .slotTime(slot.getSlotTime())
                .durationMinutes(slot.getDurationMinutes())
                .booked(slot.getBooked())
                .build();
    }

    private MentorSessionDto toSessionDto(MentorSession session) {
        return MentorSessionDto.builder()
                .id(session.getId())
                .mentorId(session.getMentorId())
                .learnerId(session.getLearnerId())
                .slotId(session.getSlotId())
                .sessionType(session.getSessionType())
                .note(session.getNote())
                .startsAt(session.getStartsAt())
                .durationMinutes(session.getDurationMinutes())
                .state(session.getState())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
