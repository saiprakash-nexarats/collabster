package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CommunityDto;
import com.collabsphere_first.community.dto.CommunitySearchQuery;
import org.springframework.data.domain.Page;

public interface DiscoveryService {
    Page<CommunityDto> searchCommunities(CommunitySearchQuery query);
    Page<CommunityDto> getTrendingCommunities(int page, int size);
    Page<CommunityDto> recommendCommunities(Long userId, int page, int size);
    void recalculateActivityScores();
}
