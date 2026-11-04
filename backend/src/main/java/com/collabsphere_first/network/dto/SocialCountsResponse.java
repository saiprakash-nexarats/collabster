package com.collabsphere_first.network.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialCountsResponse {
    private long followersCount;
    private long followingCount;
    private long connectionsCount;
}
