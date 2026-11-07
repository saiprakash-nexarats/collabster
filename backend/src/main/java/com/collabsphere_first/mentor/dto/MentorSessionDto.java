package com.collabsphere_first.mentor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorSessionDto {
    private UUID id;
    private Long mentorId;
    private Long learnerId;
    private UUID slotId;
    private String sessionType;
    private String note;
    private Instant startsAt;
    private Integer durationMinutes;
    private String state;
    private Instant createdAt;
}

