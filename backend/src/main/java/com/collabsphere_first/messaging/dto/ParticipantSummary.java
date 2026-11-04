package com.collabsphere_first.messaging.dto;

public record ParticipantSummary(
    Long id,
    String username,
    String fullName,
    String avatarUrl,
    boolean online
) {}
