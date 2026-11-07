package com.collabsphere_first.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseLessonDto {
    private UUID id;
    private UUID courseId;
    private String title;
    private String summary;
    private String content;
    private Integer orderIndex;
    private Integer durationMinutes;
    private Boolean completed;
}

