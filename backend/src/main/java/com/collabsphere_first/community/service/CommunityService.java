package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CommunityDto;
import com.collabsphere_first.community.dto.CreateCommunityRequest;
import com.collabsphere_first.community.dto.UpdateCommunityRequest;

import java.util.UUID;

public interface CommunityService {
    CommunityDto createCommunity(CreateCommunityRequest request, Long creatorUserId);
    CommunityDto getCommunity(UUID communityId);
    CommunityDto getCommunityBySlug(String slug);
    CommunityDto getCommunity(UUID communityId, Long requestingUserId);
    CommunityDto getCommunityBySlug(String slug, Long requestingUserId);
    CommunityDto updateCommunity(UUID communityId, UpdateCommunityRequest request, Long requestingUserId);
    void archiveCommunity(UUID communityId, Long requestingUserId);

    void handleCommunityCreated(UUID eventId, UUID communityId, String name);
}
