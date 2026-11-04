package com.collabsphere_first.community.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateInvitationRequest {
    @NotNull
    @Positive
    private Long inviteeId;
}
