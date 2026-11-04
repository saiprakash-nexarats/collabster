package com.collabsphere_first.network.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowSummaryResponse {
    private UserSummaryDto user;
    private Instant followedAt;
}
