package com.collabsphere_first.mentor.service;

import com.collabsphere_first.mentor.dto.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MentorService {
    List<MentorProfileDto> listMentors();
    List<MentorSessionDto> listUserSessions(Long userId);
    MentorProfileDto upsertProfile(UpsertMentorProfileRequest request, Long userId);
    MentorSessionDto requestSession(Long mentorId, RequestSessionRequest request, Long learnerId);
    MentorSessionDto confirmSession(UUID sessionId, Long userId);
    MentorSessionDto cancelSession(UUID sessionId, Long userId);
    List<MentorSlotDto> listSlots(Long mentorId);
    MentorSlotDto createSlot(CreateMentorSlotRequest request, Long mentorId);
    Map<String, Object> bookSlot(UUID slotId, BookMentorSlotRequest request, Long learnerId);
}
