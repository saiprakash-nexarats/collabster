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
public class CreateCourseRequest {
    @NotBlank(message = "Title must not be blank")
    private String title;

    private String summary;
    private String description;
    private String difficulty;
    private String category;
}

