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
public class RequestSessionRequest {
    private String sessionType;
    private String note;

    @NotNull(message = "Session start time is required")
    private Instant startsAt;

    private Integer durationMinutes;
}

