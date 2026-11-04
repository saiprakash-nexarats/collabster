package com.collabsphere_first.interaction.dto;

public record InteractionSummary(
    boolean liked,
    boolean saved,
    long likeCount
) {}
