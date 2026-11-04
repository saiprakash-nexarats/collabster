package com.collabsphere_first.community.mapper;

import com.collabsphere_first.community.dto.CommunityDto;
import com.collabsphere_first.community.dto.CreateCommunityRequest;
import com.collabsphere_first.community.entity.Community;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommunityMapper {
    @Mapping(source = "communityStatus", target = "status")
    CommunityDto toDto(Community community);
    Community toEntity(CreateCommunityRequest request);
}
