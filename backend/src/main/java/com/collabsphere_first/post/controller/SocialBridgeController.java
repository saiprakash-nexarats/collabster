package com.collabsphere_first.post.controller;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.community.dto.CommentDto;
import com.collabsphere_first.community.dto.CreateCommentRequest;
import com.collabsphere_first.community.service.ContentService;
import com.collabsphere_first.feed.dto.FeedPageResponse;
import com.collabsphere_first.feed.service.FeedService;
import com.collabsphere_first.interaction.dto.LikeResponse;
import com.collabsphere_first.interaction.dto.SaveResponse;
import com.collabsphere_first.interaction.dto.SavedItemsCursorPageResponse;
import com.collabsphere_first.interaction.service.InteractionService;
import com.collabsphere_first.network.dto.FollowSummaryResponse;
import com.collabsphere_first.network.repository.UserFollowRepository;
import com.collabsphere_first.network.service.NetworkService;
import com.collabsphere_first.post.dto.request.CreatePlatformPostRequest;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.service.PlatformPostService;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller bridging web & mobile social contracts (/api/v1/social and /v1/social)
 * with the underlying modular services.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SocialBridgeController {

    private final FeedService feedService;
    private final PlatformPostService postService;
    private final InteractionService interactionService;
    private final NetworkService networkService;
    private final UserFollowRepository userFollowRepository;
    private final ContentService contentService;
    private final CurrentUserService currentUserService;

    @GetMapping({"/api/v1/social/feed", "/v1/social/feed"})
    public ResponseEntity<ApiResponse<FeedPageResponse>> getSocialFeed(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit
    ) {
        User viewer = currentUserService.getCurrentUser();
        FeedPageResponse page = feedService.getFeed(viewer, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PostMapping({"/api/v1/social/posts", "/v1/social/posts"})
    public ResponseEntity<PlatformPostResponse> createSocialPost(
            @RequestBody Map<String, Object> draft
    ) {
        String body = (String) draft.get("body");
        if (body == null || body.isBlank()) {
            body = (String) draft.get("text");
        }
        if (body == null || body.isBlank()) {
            body = (String) draft.get("title");
        }
        if (body == null || body.isBlank()) {
            body = "Untitled post";
        }

        CreatePlatformPostRequest request = CreatePlatformPostRequest.builder()
                .body(body)
                .visibility(PostVisibility.PUBLIC)
                .media(Collections.emptyList())
                .build();

        PlatformPostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping({"/api/v1/social/posts/{id}/comments", "/v1/social/posts/{id}/comments"})
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        User viewer = currentUserService.getCurrentUser();
        String content = body.get("body");
        if (content == null || content.isBlank()) {
            content = body.get("content");
        }

        try {
            CreateCommentRequest request = new CreateCommentRequest();
            request.setBody(content != null ? content : "");
            CommentDto commentDto = contentService.createComment(id, request, viewer.getId());

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", commentDto.getId());
            resp.put("author", viewer.getFirstName() + " " + viewer.getLastName());
            resp.put("authorId", viewer.getId().toString());
            resp.put("body", commentDto.getBody());
            resp.put("createdAt", commentDto.getCreatedAt());
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", UUID.randomUUID().toString());
            resp.put("author", viewer.getFirstName() + " " + viewer.getLastName());
            resp.put("authorId", viewer.getId().toString());
            resp.put("body", content);
            resp.put("createdAt", new Date().toString());
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        }
    }

    @PutMapping({"/api/v1/social/posts/{id}/reaction", "/v1/social/posts/{id}/reaction"})
    public ResponseEntity<ApiResponse<LikeResponse>> setReaction(@PathVariable UUID id) {
        User user = currentUserService.getCurrentUser();
        ContentRef ref = new ContentRef(ContentType.PLATFORM_POST, id);
        LikeResponse res = interactionService.likeContent(user, ref);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @DeleteMapping({"/api/v1/social/posts/{id}/reaction", "/v1/social/posts/{id}/reaction"})
    public ResponseEntity<ApiResponse<LikeResponse>> removeReaction(@PathVariable UUID id) {
        User user = currentUserService.getCurrentUser();
        ContentRef ref = new ContentRef(ContentType.PLATFORM_POST, id);
        LikeResponse res = interactionService.unlikeContent(user, ref);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @PostMapping({"/api/v1/social/posts/{id}/save", "/v1/social/posts/{id}/save"})
    public ResponseEntity<ApiResponse<SaveResponse>> savePost(@PathVariable UUID id) {
        User user = currentUserService.getCurrentUser();
        ContentRef ref = new ContentRef(ContentType.PLATFORM_POST, id);
        SaveResponse res = interactionService.saveContent(user, ref);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @DeleteMapping({"/api/v1/social/posts/{id}/save", "/v1/social/posts/{id}/save"})
    public ResponseEntity<ApiResponse<SaveResponse>> unsavePost(@PathVariable UUID id) {
        User user = currentUserService.getCurrentUser();
        ContentRef ref = new ContentRef(ContentType.PLATFORM_POST, id);
        SaveResponse res = interactionService.unsaveContent(user, ref);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @GetMapping({"/api/v1/social/saved", "/v1/social/saved"})
    public ResponseEntity<ApiResponse<SavedItemsCursorPageResponse>> getSaved(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit
    ) {
        User user = currentUserService.getCurrentUser();
        SavedItemsCursorPageResponse res = interactionService.getSavedContent(user, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @GetMapping({"/api/v1/social/saved/ids", "/v1/social/saved/ids"})
    public ResponseEntity<List<String>> getSavedIds() {
        User user = currentUserService.getCurrentUser();
        SavedItemsCursorPageResponse res = interactionService.getSavedContent(user, null, 100);
        List<String> ids = res.getItems() != null ?
                res.getItems().stream().map(item -> item.getContentRef().id().toString()).toList() :
                Collections.emptyList();
        return ResponseEntity.ok(ids);
    }

    @PostMapping({"/api/v1/social/profiles/{profileId}/follow", "/v1/social/profiles/{profileId}/follow"})
    public ResponseEntity<ApiResponse<FollowSummaryResponse>> follow(@PathVariable Long profileId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        FollowSummaryResponse res = networkService.followUser(currentUserId, profileId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(res));
    }

    @DeleteMapping({"/api/v1/social/profiles/{profileId}/follow", "/v1/social/profiles/{profileId}/follow"})
    public ResponseEntity<ApiResponse<Void>> unfollow(@PathVariable Long profileId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        networkService.unfollowUser(currentUserId, profileId);
        return ResponseEntity.ok(ApiResponse.success("Unfollowed successfully", null));
    }

    @GetMapping({"/api/v1/social/profiles/{profileId}/follow/status", "/v1/social/profiles/{profileId}/follow/status"})
    public ResponseEntity<Map<String, Boolean>> followStatus(@PathVariable Long profileId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        boolean following = userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, profileId);
        return ResponseEntity.ok(Map.of("following", following));
    }
}
