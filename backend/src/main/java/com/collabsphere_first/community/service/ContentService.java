package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CommentDto;
import com.collabsphere_first.community.dto.CreateCommentRequest;
import com.collabsphere_first.community.dto.CreatePostRequest;
import com.collabsphere_first.community.dto.PostDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ContentService {
    PostDto createPost(UUID communityId, CreatePostRequest request, Long authorId);
    Page<PostDto> getCommunityFeed(UUID communityId, Long requestingUserId, Pageable pageable);
    PostDto getPost(UUID postId, Long requestingUserId);
    PostDto pinPost(UUID postId, boolean pin, Long requestingUserId);
    PostDto lockPost(UUID postId, boolean lock, Long requestingUserId);

    CommentDto createComment(UUID postId, CreateCommentRequest request, Long authorId);
    Page<CommentDto> getPostComments(UUID postId, Long requestingUserId, Pageable pageable);
}
