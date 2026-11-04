package com.collabsphere_first.network.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.network.dto.*;
import com.collabsphere_first.network.service.NetworkService;
import com.collabsphere_first.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/network")
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkService networkService;
    private final CurrentUserService currentUserService;

    // ==========================================
    // FOLLOW
    // ==========================================

    @PostMapping("/follows/{targetUserId}")
    public ResponseEntity<ApiResponse<FollowSummaryResponse>> followUser(@PathVariable Long targetUserId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        FollowSummaryResponse response = networkService.followUser(currentUserId, targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Followed successfully", response));
    }

    @DeleteMapping("/follows/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(@PathVariable Long targetUserId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        networkService.unfollowUser(currentUserId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Unfollowed successfully", null));
    }

    @GetMapping("/followers/me")
    public ResponseEntity<ApiResponse<NetworkCursorPageResponse<FollowSummaryResponse>>> getMyFollowers(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NetworkCursorPageResponse<FollowSummaryResponse> page = networkService.getFollowers(currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/following/me")
    public ResponseEntity<ApiResponse<NetworkCursorPageResponse<FollowSummaryResponse>>> getMyFollowing(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NetworkCursorPageResponse<FollowSummaryResponse> page = networkService.getFollowing(currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/followers/{userId}")
    public ResponseEntity<ApiResponse<NetworkCursorPageResponse<FollowSummaryResponse>>> getUserFollowers(
            @PathVariable Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        NetworkCursorPageResponse<FollowSummaryResponse> page = networkService.getFollowers(userId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/following/{userId}")
    public ResponseEntity<ApiResponse<NetworkCursorPageResponse<FollowSummaryResponse>>> getUserFollowing(
            @PathVariable Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        NetworkCursorPageResponse<FollowSummaryResponse> page = networkService.getFollowing(userId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    // ==========================================
    // CONNECTION
    // ==========================================

    @PostMapping("/connections/requests/{targetUserId}")
    public ResponseEntity<ApiResponse<ConnectionRequestResponse>> sendConnectionRequest(@PathVariable Long targetUserId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ConnectionRequestResponse response = networkService.sendConnectionRequest(currentUserId, targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Connection request sent", response));
    }

    @GetMapping("/connections/requests/incoming")
    public ResponseEntity<ApiResponse<NetworkCursorPageResponse<ConnectionRequestResponse>>> getIncomingRequests(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NetworkCursorPageResponse<ConnectionRequestResponse> page = networkService.getIncomingRequests(currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/connections/requests/outgoing")
    public ResponseEntity<ApiResponse<NetworkCursorPageResponse<ConnectionRequestResponse>>> getOutgoingRequests(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NetworkCursorPageResponse<ConnectionRequestResponse> page = networkService.getOutgoingRequests(currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PostMapping("/connections/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<ConnectionRequestResponse>> acceptConnectionRequest(@PathVariable UUID requestId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ConnectionRequestResponse response = networkService.acceptConnectionRequest(currentUserId, requestId);
        return ResponseEntity.ok(ApiResponse.success("Connection request accepted", response));
    }

    @PostMapping("/connections/requests/{requestId}/decline")
    public ResponseEntity<ApiResponse<ConnectionRequestResponse>> declineConnectionRequest(@PathVariable UUID requestId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ConnectionRequestResponse response = networkService.declineConnectionRequest(currentUserId, requestId);
        return ResponseEntity.ok(ApiResponse.success("Connection request declined", response));
    }

    @DeleteMapping("/connections/requests/{requestId}")
    public ResponseEntity<ApiResponse<Void>> cancelConnectionRequest(@PathVariable UUID requestId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        networkService.cancelConnectionRequest(currentUserId, requestId);
        return ResponseEntity.ok(ApiResponse.success("Connection request cancelled", null));
    }

    @GetMapping("/connections")
    public ResponseEntity<ApiResponse<NetworkCursorPageResponse<UserSummaryDto>>> getConnections(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NetworkCursorPageResponse<UserSummaryDto> page = networkService.getConnections(currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @DeleteMapping("/connections/{userId}")
    public ResponseEntity<ApiResponse<Void>> disconnect(@PathVariable Long userId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        networkService.disconnect(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Disconnected successfully", null));
    }

    // ==========================================
    // BLOCK
    // ==========================================

    @PutMapping("/blocks/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long targetUserId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        networkService.blockUser(currentUserId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", null));
    }

    @DeleteMapping("/blocks/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long targetUserId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        networkService.unblockUser(currentUserId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }

    @GetMapping("/blocks")
    public ResponseEntity<ApiResponse<NetworkCursorPageResponse<UserSummaryDto>>> getBlockedUsers(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NetworkCursorPageResponse<UserSummaryDto> page = networkService.getBlockedUsers(currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    // ==========================================
    // MUTE
    // ==========================================

    @PutMapping("/mutes/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> muteUser(@PathVariable Long targetUserId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        networkService.muteUser(currentUserId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("User muted successfully", null));
    }

    @DeleteMapping("/mutes/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> unmuteUser(@PathVariable Long targetUserId) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        networkService.unmuteUser(currentUserId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("User unmuted successfully", null));
    }

    @GetMapping("/mutes")
    public ResponseEntity<ApiResponse<NetworkCursorPageResponse<UserSummaryDto>>> getMutedUsers(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NetworkCursorPageResponse<UserSummaryDto> page = networkService.getMutedUsers(currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    // ==========================================
    // SOCIAL COUNTS
    // ==========================================

    @GetMapping("/counts/me")
    public ResponseEntity<ApiResponse<SocialCountsResponse>> getMyCounts() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        SocialCountsResponse counts = networkService.getSocialCounts(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    @GetMapping("/counts/{userId}")
    public ResponseEntity<ApiResponse<SocialCountsResponse>> getUserCounts(@PathVariable Long userId) {
        SocialCountsResponse counts = networkService.getSocialCounts(userId);
        return ResponseEntity.ok(ApiResponse.success(counts));
    }
}
