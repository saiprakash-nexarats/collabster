package com.collabsphere_first.community.mapper;

import com.collabsphere_first.community.dto.CreateResourceRequest;
import com.collabsphere_first.community.dto.ResourceDto;
import com.collabsphere_first.community.entity.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResourceMapper {
    @Mapping(source = "community.id", target = "communityId")
    ResourceDto toDto(Resource resource);

    Resource toEntity(CreateResourceRequest request);
}
