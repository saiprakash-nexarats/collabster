package com.collabsphere_first.community.mapper;

import com.collabsphere_first.community.dto.CommentDto;
import com.collabsphere_first.community.dto.CreateCommentRequest;
import com.collabsphere_first.community.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {
    @Mapping(source = "post.id", target = "postId")
    CommentDto toDto(Comment comment);

    Comment toEntity(CreateCommentRequest request);
}
