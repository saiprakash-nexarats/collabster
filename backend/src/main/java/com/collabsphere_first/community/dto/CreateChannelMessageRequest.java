package com.collabsphere_first.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChannelMessageRequest {

    @NotBlank(message = "Message body is required")
    private String body;

    private UUID parentMessageId;

    private List<String> attachmentUrls;
}