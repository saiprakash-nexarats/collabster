package com.collabsphere_first.network.dto;

import com.collabsphere_first.network.enums.ConnectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionRequestResponse {
    private UUID id;
    private UserSummaryDto requester;
    private UserSummaryDto addressee;
    private ConnectionStatus status;
    private Instant createdAt;
    private Instant respondedAt;
}
