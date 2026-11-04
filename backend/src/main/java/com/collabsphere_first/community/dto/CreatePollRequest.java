package com.collabsphere_first.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreatePollRequest {
    @NotBlank @Size(max = 200)
    private String question;
    @Size(min = 2, max = 10)
    private List<@NotBlank @Size(max = 200) String> options;
    private boolean allowMultiple;
    private Instant closesAt;
}
