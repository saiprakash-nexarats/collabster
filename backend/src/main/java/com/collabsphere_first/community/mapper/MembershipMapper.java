package com.collabsphere_first.community.mapper;

import com.collabsphere_first.community.dto.MembershipDto;
import com.collabsphere_first.community.entity.Membership;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MembershipMapper {
    @Mapping(source = "community.id", target = "communityId")
    MembershipDto toDto(Membership membership);
}
