package com.collabsphere_first.learning.dto;

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
public class CourseDto {
    private UUID id;
    private Long instructorId;
    private String title;
    private String summary;
    private String description;
    private String difficulty;
    private String category;
    private String state;
    private String enrollmentState;
    private Integer progress;
    private Instant createdAt;
}
