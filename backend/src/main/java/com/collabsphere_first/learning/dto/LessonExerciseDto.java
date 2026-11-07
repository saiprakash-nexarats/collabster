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
public class LessonExerciseDto {
    private UUID lessonId;
    private String exercisePrompt;
    private String starterCode;
}
