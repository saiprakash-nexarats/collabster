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
public class CareerJobDto {
    private UUID id;
    private Long employerId;
    private String companyName;
    private String title;
    private String description;
    private String location;
    private String workMode;
    private String employmentType;
    private String salaryRange;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
}

