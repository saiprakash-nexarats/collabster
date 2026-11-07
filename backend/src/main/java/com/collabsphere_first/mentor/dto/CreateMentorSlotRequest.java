package com.collabsphere_first.mentor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMentorSlotRequest {
    @NotNull(message = "Slot time is required")
    private Instant slotTime;

    private Integer durationMinutes;
}
