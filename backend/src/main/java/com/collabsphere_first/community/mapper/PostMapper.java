package com.collabsphere_first.community.mapper;

import com.collabsphere_first.community.dto.CreatePostRequest;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostMapper {

    @Mapping(source = "community.id", target = "communityId")
    PostDto toDto(Post post);

    @Mapping(source = "type", target = "type")
    Post toEntity(CreatePostRequest request);
}