package com.collabsphere_first.community.mapper;

import com.collabsphere_first.community.dto.CreateReportRequest;
import com.collabsphere_first.community.dto.ReportDto;
import com.collabsphere_first.community.entity.Report;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportMapper {
    @Mapping(source = "community.id", target = "communityId")
    ReportDto toDto(Report report);

    Report toEntity(CreateReportRequest request);
}
