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
public class MentorSlotDto {
    private UUID id;
    private Long mentorId;
    private Instant slotTime;
    private Integer durationMinutes;
    private Boolean booked;
}

