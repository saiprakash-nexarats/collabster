package com.collabsphere_first.post.mapper;

import com.collabsphere_first.post.dto.request.PlatformPostMediaRequest;
import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;
import com.collabsphere_first.post.dto.response.PlatformPostMediaResponse;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PlatformPostMedia;
import com.collabsphere_first.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PlatformPostMapper {

    public PlatformPostResponse toResponse(PlatformPost post) {
        if (post == null) {
            return null;
        }

        boolean edited = post.getUpdatedAt() != null
                && post.getCreatedAt() != null
                && post.getUpdatedAt().isAfter(post.getCreatedAt());

        return PlatformPostResponse.builder()
                .contentRef(post.toContentRef())
                .id(post.getId())
                .author(toAuthorSummary(post.getAuthor()))
                .body(post.getBody())
                .visibility(post.getVisibility())
                .status(post.getStatus())
                .media(toMediaResponseList(post.getMedia()))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .edited(edited)
                .version(post.getVersion())
                .build();
    }

    public AuthorSummaryResponse toAuthorSummary(User author) {
        if (author == null) {
            return null;
        }

        return AuthorSummaryResponse.builder()
                .id(author.getId())
                .firstName(author.getFirstName())
                .lastName(author.getLastName())
                .username(author.getUsername())
                .profilePicture(author.getProfilePicture())
                .build();
    }

    public PlatformPostMediaResponse toMediaResponse(PlatformPostMedia media) {
        if (media == null) {
            return null;
        }

        return PlatformPostMediaResponse.builder()
                .id(media.getId())
                .mediaType(media.getMediaType())
                .storageKey(media.getStorageKey())
                .mimeType(media.getMimeType())
                .displayOrder(media.getDisplayOrder())
                .width(media.getWidth())
                .height(media.getHeight())
                .durationMs(media.getDurationMs())
                .createdAt(media.getCreatedAt())
                .build();
    }

    public List<PlatformPostMediaResponse> toMediaResponseList(List<PlatformPostMedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) {
            return Collections.emptyList();
        }

        return mediaList.stream()
                .map(this::toMediaResponse)
                .collect(Collectors.toList());
    }

    public PlatformPostMedia toMediaEntity(PlatformPostMediaRequest request, PlatformPost post) {
        if (request == null) {
            return null;
        }

        return PlatformPostMedia.builder()
                .post(post)
                .mediaType(request.getMediaType())
                .storageKey(request.getStorageKey())
                .mimeType(request.getMimeType())
                .displayOrder(request.getDisplayOrder())
                .width(request.getWidth())
                .height(request.getHeight())
                .durationMs(request.getDurationMs())
                .build();
    }
}
