package com.collabsphere_first.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectMilestoneRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private Instant targetDate;
}
