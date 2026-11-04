package com.collabsphere_first.community.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class VotePollRequest {
    @NotEmpty
    private List<UUID> optionIds;
}
