package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CreateResourceRequest;
import com.collabsphere_first.community.dto.ResourceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ResourceService {
    ResourceDto createResource(UUID communityId, CreateResourceRequest request, Long uploadedBy);
    Page<ResourceDto> getCommunityResources(UUID communityId, Long requestingUserId, Pageable pageable);
    ResourceDto getResource(UUID resourceId);
}
