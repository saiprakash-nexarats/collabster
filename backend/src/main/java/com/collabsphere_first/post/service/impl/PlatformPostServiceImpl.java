package com.collabsphere_first.post.service.impl;

import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.post.constant.PostConstants;
import com.collabsphere_first.post.dto.request.CreatePlatformPostRequest;
import com.collabsphere_first.post.dto.request.PlatformPostMediaRequest;
import com.collabsphere_first.post.dto.request.UpdatePlatformPostRequest;
import com.collabsphere_first.post.dto.response.PlatformPostCursorPageResponse;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PlatformPostMedia;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.exception.InvalidPlatformPostException;
import com.collabsphere_first.post.exception.PlatformPostAccessDeniedException;
import com.collabsphere_first.post.exception.PlatformPostNotFoundException;
import com.collabsphere_first.post.mapper.PlatformPostMapper;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.post.service.PlatformPostService;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformPostServiceImpl implements PlatformPostService {

    private static final Pattern SAFE_STORAGE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-/]+\\.[a-zA-Z0-9]+$");

    private final PlatformPostRepository postRepository;
    private final PlatformPostMapper postMapper;
    private final CurrentUserService currentUserService;
    private final NetworkAccessService networkAccessService;

    @Override
    @Transactional
    public PlatformPostResponse createPost(CreatePlatformPostRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        if (request == null || !request.hasContent()) {
            throw new InvalidPlatformPostException("Post must contain non-blank body text or at least one media item.");
        }

        PostVisibility visibility = request.getVisibility() != null ? request.getVisibility() : PostVisibility.PUBLIC;

        validateMediaRequests(request.getMedia());

        PlatformPost post = PlatformPost.builder()
                .author(currentUser)
                .body(request.getBody() != null ? request.getBody().trim() : null)
                .visibility(visibility)
                .status(PostStatus.ACTIVE)
                .build();

        if (request.getMedia() != null && !request.getMedia().isEmpty()) {
            attachMediaItems(post, request.getMedia());
        }

        PlatformPost savedPost = postRepository.save(post);
        log.info("PlatformPost created with ID: {} by author ID: {}", savedPost.getId(), currentUser.getId());
        return postMapper.toResponse(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformPostResponse getPostById(UUID postId) {
        if (postId == null) {
            throw new InvalidPlatformPostException("Post ID must not be null.");
        }

        PlatformPost post = postRepository.findById(postId)
                .orElseThrow(() -> new PlatformPostNotFoundException(postId));

        if (post.isDeleted()) {
            throw new PlatformPostNotFoundException(postId);
        }

        // Access control checks for non-public visibility
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            User currentUser = currentUserService.getCurrentUser();
            if (!post.getAuthor().getId().equals(currentUser.getId())) {
                throw new PlatformPostAccessDeniedException("You do not have permission to view this private post.");
            }
        } else if (post.getVisibility() == PostVisibility.FOLLOWERS_ONLY) {
            User currentUser = currentUserService.getCurrentUser();
            Long viewerId = currentUser.getId();
            Long authorId = post.getAuthor().getId();
            if (!authorId.equals(viewerId)) {
                if (networkAccessService.isBlockedEitherDirection(viewerId, authorId)) {
                    throw new PlatformPostAccessDeniedException("You do not have permission to view this post.");
                }
                if (!networkAccessService.isFollowing(viewerId, authorId)) {
                    throw new PlatformPostAccessDeniedException("Only followers can view this post.");
                }
            }
        } else if (post.getVisibility() == PostVisibility.PUBLIC) {
            try {
                User currentUser = currentUserService.getCurrentUser();
                if (currentUser != null && currentUser.getId() != null) {
                    Long viewerId = currentUser.getId();
                    Long authorId = post.getAuthor().getId();
                    if (!authorId.equals(viewerId) && networkAccessService.isBlockedEitherDirection(viewerId, authorId)) {
                        throw new PlatformPostAccessDeniedException("You do not have permission to view this post.");
                    }
                }
            } catch (IllegalStateException ex) {
                // Anonymous or internal background context: public post is accessible
            }
        }

        return postMapper.toResponse(post);
    }

    @Override
    @Transactional
    public PlatformPostResponse updatePost(UUID postId, UpdatePlatformPostRequest request) {
        if (postId == null) {
            throw new InvalidPlatformPostException("Post ID must not be null.");
        }

        PlatformPost post = postRepository.findById(postId)
                .orElseThrow(() -> new PlatformPostNotFoundException(postId));

        if (post.isDeleted()) {
            throw new PlatformPostNotFoundException(postId);
        }

        User currentUser = currentUserService.getCurrentUser();
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new PlatformPostAccessDeniedException("You are not authorized to update this post.");
        }

        if (request.getBody() != null) {
            post.setBody(request.getBody().trim());
        }

        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }

        if (request.getMedia() != null) {
            validateMediaRequests(request.getMedia());
            post.clearMedia();
            attachMediaItems(post, request.getMedia());
        }

        // Validate that post is not left empty after update
        boolean hasBody = post.getBody() != null && !post.getBody().trim().isEmpty();
        boolean hasMedia = !post.getMedia().isEmpty();
        if (!hasBody && !hasMedia) {
            throw new InvalidPlatformPostException("Updated post cannot be empty; text body or media is required.");
        }

        PlatformPost updatedPost = postRepository.save(post);
        log.info("PlatformPost updated with ID: {} by author ID: {}", updatedPost.getId(), currentUser.getId());
        return postMapper.toResponse(updatedPost);
    }

    @Override
    @Transactional
    public void deletePost(UUID postId) {
        if (postId == null) {
            throw new InvalidPlatformPostException("Post ID must not be null.");
        }

        PlatformPost post = postRepository.findById(postId)
                .orElseThrow(() -> new PlatformPostNotFoundException(postId));

        if (post.isDeleted()) {
            throw new PlatformPostNotFoundException(postId);
        }

        User currentUser = currentUserService.getCurrentUser();
        boolean isAuthor = post.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == com.collabsphere_first.role.entity.RoleName.ROLE_ADMIN);

        if (!isAuthor && !isAdmin) {
            throw new PlatformPostAccessDeniedException("You are not authorized to delete this post.");
        }

        post.softDelete();
        postRepository.save(post);
        log.info("PlatformPost soft-deleted with ID: {} by actor ID: {} (isAdmin={})", postId, currentUser.getId(), isAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformPostCursorPageResponse getMyPosts(String cursor, int limit) {
        User currentUser = currentUserService.getCurrentUser();
        int safeLimit = Math.max(1, Math.min(limit > 0 ? limit : PostConstants.DEFAULT_PAGE_LIMIT, PostConstants.MAX_PAGE_LIMIT));

        // Fetch safeLimit + 1 to reliably determine if there is a next page
        int fetchSize = safeLimit + 1;
        List<PlatformPost> posts;

        if (cursor == null || cursor.trim().isEmpty()) {
            posts = postRepository.findInitialAuthorPosts(
                    currentUser.getId(),
                    PostStatus.ACTIVE,
                    PageRequest.of(0, fetchSize)
            );
        } else {
            DecodedCursor decoded = decodeCursor(cursor);
            posts = postRepository.findAuthorPostsAfterCursor(
                    currentUser.getId(),
                    PostStatus.ACTIVE,
                    decoded.createdAt(),
                    decoded.id(),
                    PageRequest.of(0, fetchSize)
            );
        }

        boolean hasMore = posts.size() > safeLimit;
        List<PlatformPost> pageItems = hasMore ? posts.subList(0, safeLimit) : posts;

        String nextCursor = null;
        if (hasMore && !pageItems.isEmpty()) {
            PlatformPost lastItem = pageItems.get(pageItems.size() - 1);
            nextCursor = encodeCursor(lastItem.getCreatedAt(), lastItem.getId());
        }

        List<PlatformPostResponse> responseItems = pageItems.stream()
                .map(postMapper::toResponse)
                .collect(Collectors.toList());

        return PlatformPostCursorPageResponse.builder()
                .items(responseItems)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .limit(safeLimit)
                .build();
    }

    private void validateMediaRequests(List<PlatformPostMediaRequest> mediaRequests) {
        if (mediaRequests == null) return;

        if (mediaRequests.size() > PostConstants.MAX_MEDIA_COUNT) {
            throw new InvalidPlatformPostException("Maximum " + PostConstants.MAX_MEDIA_COUNT + " media attachments permitted per post.");
        }

        Set<Integer> orders = new HashSet<>();
        for (PlatformPostMediaRequest mediaReq : mediaRequests) {
            if (mediaReq.getDisplayOrder() < 0) {
                throw new InvalidPlatformPostException("Media display order cannot be negative.");
            }
            if (!orders.add(mediaReq.getDisplayOrder())) {
                throw new InvalidPlatformPostException("Duplicate media display order detected: " + mediaReq.getDisplayOrder());
            }
            validateStorageKey(mediaReq.getStorageKey());
        }
    }

    private void validateStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new InvalidPlatformPostException("Media storage key must not be blank.");
        }
        String normalized = storageKey.replace('\\', '/');
        if (normalized.contains("..") || normalized.startsWith("/") || normalized.contains(":") || !SAFE_STORAGE_KEY_PATTERN.matcher(normalized).matches()) {
            throw new InvalidPlatformPostException("Invalid media storage key format: path traversal, absolute paths, or special characters are forbidden.");
        }
    }

    private void attachMediaItems(PlatformPost post, List<PlatformPostMediaRequest> mediaRequests) {
        for (PlatformPostMediaRequest req : mediaRequests) {
            PlatformPostMedia mediaEntity = postMapper.toMediaEntity(req, post);
            post.addMedia(mediaEntity);
        }
    }

    private String encodeCursor(Instant createdAt, UUID id) {
        String raw = createdAt.toEpochMilli() + ":" + id.toString();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private DecodedCursor decodeCursor(String cursor) {
        try {
            if (cursor == null || cursor.isBlank()) {
                throw new IllegalArgumentException("Cursor cannot be blank");
            }
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cursor);
            String raw = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("Malformed cursor format");
            }
            long epochMilli = Long.parseLong(parts[0]);
            if (epochMilli <= 0 || epochMilli > 32503680000000L) { // Year 3000 boundary guard
                throw new IllegalArgumentException("Cursor timestamp out of valid range");
            }
            Instant createdAt = Instant.ofEpochMilli(epochMilli);
            UUID id = UUID.fromString(parts[1]);
            return new DecodedCursor(createdAt, id);
        } catch (Exception ex) {
            log.warn("Invalid pagination cursor provided: {}", cursor);
            throw new InvalidPlatformPostException("Invalid pagination cursor token.");
        }
    }

    private record DecodedCursor(Instant createdAt, UUID id) {}
}
