package com.collabsphere_first.network.service;

import com.collabsphere_first.network.dto.SocialCountsResponse;
import com.collabsphere_first.network.repository.UserBlockRepository;
import com.collabsphere_first.network.repository.UserConnectionRepository;
import com.collabsphere_first.network.repository.UserFollowRepository;
import com.collabsphere_first.network.repository.UserMuteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NetworkAccessServiceImpl implements NetworkAccessService {

    private final UserFollowRepository userFollowRepository;
    private final UserConnectionRepository userConnectionRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserMuteRepository userMuteRepository;

    @Override
    public boolean isFollowing(Long viewerId, Long authorId) {
        if (viewerId == null || authorId == null) {
            return false;
        }
        return userFollowRepository.existsByFollowerIdAndFollowingId(viewerId, authorId);
    }

    @Override
    public boolean isConnected(Long userA, Long userB) {
        if (userA == null || userB == null) {
            return false;
        }
        return userConnectionRepository.isConnected(userA, userB);
    }

    @Override
    public boolean isBlocked(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            return false;
        }
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    public boolean isBlockedEitherDirection(Long userA, Long userB) {
        if (userA == null || userB == null) {
            return false;
        }
        return userBlockRepository.isBlockedEitherDirection(userA, userB);
    }

    @Override
    public boolean isMuted(Long muterId, Long mutedId) {
        if (muterId == null || mutedId == null) {
            return false;
        }
        return userMuteRepository.existsByMuterIdAndMutedId(muterId, mutedId);
    }

    @Override
    public SocialCountsResponse getSocialCounts(Long userId) {
        if (userId == null) {
            return new SocialCountsResponse(0, 0, 0);
        }
        long followers = userFollowRepository.countByFollowingId(userId);
        long following = userFollowRepository.countByFollowerId(userId);
        long connections = userConnectionRepository.countAcceptedConnections(userId);
        return new SocialCountsResponse(followers, following, connections);
    }

    @Override
    public long getFollowersCount(Long userId) {
        if (userId == null) return 0;
        return userFollowRepository.countByFollowingId(userId);
    }

    @Override
    public long getFollowingCount(Long userId) {
        if (userId == null) return 0;
        return userFollowRepository.countByFollowerId(userId);
    }

    @Override
    public long getConnectionsCount(Long userId) {
        if (userId == null) return 0;
        return userConnectionRepository.countAcceptedConnections(userId);
    }

    @Override
    public java.util.List<Long> findFollowingUserIds(Long userId, int limit) {
        if (userId == null || limit <= 0) return java.util.Collections.emptyList();
        int boundedLimit = Math.min(limit, 500);
        return userFollowRepository.findFollowingUserIds(userId, org.springframework.data.domain.PageRequest.of(0, boundedLimit));
    }

    @Override
    public java.util.List<Long> findConnectedUserIds(Long userId, int limit) {
        if (userId == null || limit <= 0) return java.util.Collections.emptyList();
        int boundedLimit = Math.min(limit, 500);
        return userConnectionRepository.findConnectedUserIds(userId, org.springframework.data.domain.PageRequest.of(0, boundedLimit));
    }

    @Override
    public java.util.Set<Long> findMutedUserIds(Long userId) {
        if (userId == null) return java.util.Collections.emptySet();
        return userMuteRepository.findMutedUserIds(userId);
    }

    @Override
    public java.util.Set<Long> findBlockedOrBlockingUserIds(Long userId) {
        if (userId == null) return java.util.Collections.emptySet();
        return userBlockRepository.findBlockedOrBlockingUserIds(userId);
    }

    @Override
    public java.util.Set<Long> findBlockedOrBlockingUserIdsAmong(Long userId, java.util.Collection<Long> candidateIds) {
        if (userId == null || candidateIds == null || candidateIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return userBlockRepository.findBlockedOrBlockingUserIdsAmong(userId, candidateIds);
    }

    @Override
    public java.util.Set<Long> findMutedUserIdsAmong(Long userId, java.util.Collection<Long> candidateIds) {
        if (userId == null || candidateIds == null || candidateIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return userMuteRepository.findMutedUserIdsAmong(userId, candidateIds);
    }

    @Override
    public java.util.Set<Long> findFollowingUserIdsAmong(Long followerId, java.util.Collection<Long> candidateIds) {
        if (followerId == null || candidateIds == null || candidateIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return userFollowRepository.findFollowingUserIdsAmong(followerId, candidateIds);
    }
}
