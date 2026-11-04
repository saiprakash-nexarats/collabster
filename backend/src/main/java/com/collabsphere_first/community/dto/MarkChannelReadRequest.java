package com.collabsphere_first.community.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MarkChannelReadRequest {
    @NotNull
    private UUID lastReadMessageId;
}
