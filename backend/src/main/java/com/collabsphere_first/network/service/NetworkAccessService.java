package com.collabsphere_first.network.service;

import com.collabsphere_first.network.dto.SocialCountsResponse;

public interface NetworkAccessService {

    boolean isFollowing(Long viewerId, Long authorId);

    boolean isConnected(Long userA, Long userB);

    boolean isBlocked(Long blockerId, Long blockedId);

    boolean isBlockedEitherDirection(Long userA, Long userB);

    boolean isMuted(Long muterId, Long mutedId);

    SocialCountsResponse getSocialCounts(Long userId);

    long getFollowersCount(Long userId);

    long getFollowingCount(Long userId);

    long getConnectionsCount(Long userId);

    java.util.List<Long> findFollowingUserIds(Long userId, int limit);

    java.util.List<Long> findConnectedUserIds(Long userId, int limit);

    java.util.Set<Long> findMutedUserIds(Long userId);

    java.util.Set<Long> findBlockedOrBlockingUserIds(Long userId);

    java.util.Set<Long> findBlockedOrBlockingUserIdsAmong(Long userId, java.util.Collection<Long> candidateIds);

    java.util.Set<Long> findMutedUserIdsAmong(Long userId, java.util.Collection<Long> candidateIds);

    java.util.Set<Long> findFollowingUserIdsAmong(Long followerId, java.util.Collection<Long> candidateIds);
}
