package com.collabsphere_first.career.dto;

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
public class CareerApplicationDto {
    private UUID id;
    private UUID jobId;
    private Long applicantId;
    private String applicantName;
    private String applicantEmail;
    private UUID resumeObjectId;
    private String note;
    private String state;
    private String reviewNotes;
    private Instant createdAt;
    private Instant updatedAt;
}

