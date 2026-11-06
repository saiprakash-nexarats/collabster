package com.collabsphere_first.company.dto;

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
public class CompanyTaskDto {
    private UUID id;
    private UUID companyId;
    private String title;
    private String description;
    private Long assigneeId;
    private String state;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
