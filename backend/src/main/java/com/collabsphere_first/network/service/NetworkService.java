package com.collabsphere_first.network.service;

import com.collabsphere_first.network.dto.*;

import java.util.UUID;

public interface NetworkService {

    // Follow
    FollowSummaryResponse followUser(Long followerId, Long targetUserId);
    void unfollowUser(Long followerId, Long targetUserId);
    NetworkCursorPageResponse<FollowSummaryResponse> getFollowers(Long userId, String cursor, int limit);
    NetworkCursorPageResponse<FollowSummaryResponse> getFollowing(Long userId, String cursor, int limit);

    // Connection
    ConnectionRequestResponse sendConnectionRequest(Long requesterId, Long targetUserId);
    ConnectionRequestResponse acceptConnectionRequest(Long currentUserId, UUID requestId);
    ConnectionRequestResponse declineConnectionRequest(Long currentUserId, UUID requestId);
    void cancelConnectionRequest(Long currentUserId, UUID requestId);
    void disconnect(Long currentUserId, Long targetUserId);
    NetworkCursorPageResponse<ConnectionRequestResponse> getIncomingRequests(Long currentUserId, String cursor, int limit);
    NetworkCursorPageResponse<ConnectionRequestResponse> getOutgoingRequests(Long currentUserId, String cursor, int limit);
    NetworkCursorPageResponse<UserSummaryDto> getConnections(Long currentUserId, String cursor, int limit);

    // Block
    void blockUser(Long blockerId, Long targetUserId);
    void unblockUser(Long blockerId, Long targetUserId);
    NetworkCursorPageResponse<UserSummaryDto> getBlockedUsers(Long currentUserId, String cursor, int limit);

    // Mute
    void muteUser(Long muterId, Long targetUserId);
    void unmuteUser(Long muterId, Long targetUserId);
    NetworkCursorPageResponse<UserSummaryDto> getMutedUsers(Long currentUserId, String cursor, int limit);

    // Social counts
    SocialCountsResponse getSocialCounts(Long userId);
}
