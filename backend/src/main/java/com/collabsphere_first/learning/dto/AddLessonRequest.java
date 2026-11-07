package com.collabsphere_first.learning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddLessonRequest {
    @NotBlank(message = "Title must not be blank")
    private String title;

    private String summary;
    private String content;
    private String exercisePrompt;
    private String starterCode;
    private Integer orderIndex;
    private Integer durationMinutes;
}
