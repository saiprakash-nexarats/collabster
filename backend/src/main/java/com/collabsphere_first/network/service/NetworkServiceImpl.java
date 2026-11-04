package com.collabsphere_first.network.service;

import com.collabsphere_first.common.pagination.CursorUtils;
import com.collabsphere_first.common.pagination.CursorUtils.CursorPayload;
import com.collabsphere_first.network.dto.*;
import com.collabsphere_first.network.entity.*;
import com.collabsphere_first.network.enums.ConnectionStatus;
import com.collabsphere_first.network.event.ConnectionAcceptedEvent;
import com.collabsphere_first.network.event.ConnectionRequestedEvent;
import com.collabsphere_first.network.event.UserFollowedEvent;
import com.collabsphere_first.network.exception.*;
import com.collabsphere_first.network.repository.*;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NetworkServiceImpl implements NetworkService {

    private final UserFollowRepository userFollowRepository;
    private final UserConnectionRepository userConnectionRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserMuteRepository userMuteRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private User getExistingUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RelationshipNotFoundException("User not found: " + userId));
    }

    // ==========================================
    // FOLLOW
    // ==========================================

    @Override
    public FollowSummaryResponse followUser(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new InvalidRelationshipException("Cannot follow yourself");
        }

        User follower = getExistingUser(followerId);
        User target = getExistingUser(targetUserId);

        if (userBlockRepository.isBlockedEitherDirection(followerId, targetUserId)) {
            throw new NetworkAccessDeniedException("Cannot follow user due to an active block relationship");
        }

        Optional<UserFollow> existing = userFollowRepository.findByFollowerIdAndFollowingId(followerId, targetUserId);
        if (existing.isPresent()) {
            return toFollowSummary(existing.get(), target);
        }

        UserFollow follow = UserFollow.builder()
                .follower(follower)
                .following(target)
                .build();
        UserFollow saved = userFollowRepository.save(follow);

        eventPublisher.publishEvent(new UserFollowedEvent(this, saved.getId(), followerId, targetUserId));
        log.info("User {} followed user {}", followerId, targetUserId);

        return toFollowSummary(saved, target);
    }

    @Override
    public void unfollowUser(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) {
            return;
        }
        userFollowRepository.deleteByFollowerIdAndFollowingId(followerId, targetUserId);
        log.info("User {} unfollowed user {}", followerId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public NetworkCursorPageResponse<FollowSummaryResponse> getFollowers(Long userId, String cursor, int limit) {
        getExistingUser(userId);
        int pageSize = sanitizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<UserFollow> follows;
        if (cursor == null || cursor.isBlank()) {
            follows = userFollowRepository.findInitialFollowers(userId, pageRequest);
        } else {
            CursorPayload payload = CursorUtils.decode(cursor);
            follows = userFollowRepository.findFollowersAfterCursor(userId, payload.timestamp(), payload.uuid(), pageRequest);
        }

        boolean hasNext = follows.size() > pageSize;
        List<UserFollow> pagedList = hasNext ? follows.subList(0, pageSize) : follows;

        String nextCursor = null;
        if (hasNext && !pagedList.isEmpty()) {
            UserFollow last = pagedList.get(pagedList.size() - 1);
            nextCursor = CursorUtils.encode(last.getCreatedAt(), last.getId());
        }

        List<FollowSummaryResponse> items = pagedList.stream()
                .map(f -> toFollowSummary(f, f.getFollower()))
                .collect(Collectors.toList());

        return NetworkCursorPageResponse.<FollowSummaryResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .limit(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NetworkCursorPageResponse<FollowSummaryResponse> getFollowing(Long userId, String cursor, int limit) {
        getExistingUser(userId);
        int pageSize = sanitizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<UserFollow> follows;
        if (cursor == null || cursor.isBlank()) {
            follows = userFollowRepository.findInitialFollowing(userId, pageRequest);
        } else {
            CursorPayload payload = CursorUtils.decode(cursor);
            follows = userFollowRepository.findFollowingAfterCursor(userId, payload.timestamp(), payload.uuid(), pageRequest);
        }

        boolean hasNext = follows.size() > pageSize;
        List<UserFollow> pagedList = hasNext ? follows.subList(0, pageSize) : follows;

        String nextCursor = null;
        if (hasNext && !pagedList.isEmpty()) {
            UserFollow last = pagedList.get(pagedList.size() - 1);
            nextCursor = CursorUtils.encode(last.getCreatedAt(), last.getId());
        }

        List<FollowSummaryResponse> items = pagedList.stream()
                .map(f -> toFollowSummary(f, f.getFollowing()))
                .collect(Collectors.toList());

        return NetworkCursorPageResponse.<FollowSummaryResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .limit(pageSize)
                .build();
    }

    // ==========================================
    // CONNECTION
    // ==========================================

    @Override
    public ConnectionRequestResponse sendConnectionRequest(Long requesterId, Long targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw new InvalidRelationshipException("Cannot connect with yourself");
        }

        User requester = getExistingUser(requesterId);
        User target = getExistingUser(targetUserId);

        if (userBlockRepository.isBlockedEitherDirection(requesterId, targetUserId)) {
            throw new NetworkAccessDeniedException("Cannot send connection request due to an active block relationship");
        }

        Optional<UserConnection> existingOpt = userConnectionRepository.findBetween(requesterId, targetUserId);
        if (existingOpt.isPresent()) {
            UserConnection existing = existingOpt.get();

            if (existing.getStatus() == ConnectionStatus.ACCEPTED) {
                throw new RelationshipConflictException("Already connected with this user");
            }

            if (existing.getStatus() == ConnectionStatus.PENDING) {
                if (existing.getRequester().getId().equals(requesterId)) {
                    // Idempotent duplicate request
                    return toConnectionResponse(existing);
                } else {
                    // Reverse pending request exists -> auto-accept
                    existing.setStatus(ConnectionStatus.ACCEPTED);
                    existing.setRespondedAt(Instant.now());
                    UserConnection saved = userConnectionRepository.save(existing);
                    eventPublisher.publishEvent(new ConnectionAcceptedEvent(this, saved.getId(), saved.getRequester().getId(), saved.getAddressee().getId()));
                    log.info("Auto-accepted reciprocal connection between {} and {}", requesterId, targetUserId);
                    return toConnectionResponse(saved);
                }
            }

            // If previously DECLINED or CANCELLED, reopen as PENDING
            existing.setRequester(requester);
            existing.setAddressee(target);
            existing.setStatus(ConnectionStatus.PENDING);
            existing.setCreatedAt(Instant.now());
            existing.setRespondedAt(null);
            UserConnection saved = userConnectionRepository.save(existing);
            eventPublisher.publishEvent(new ConnectionRequestedEvent(this, saved.getId(), requesterId, targetUserId));
            log.info("Reopened connection request from {} to {}", requesterId, targetUserId);
            return toConnectionResponse(saved);
        }

        UserConnection conn = UserConnection.builder()
                .requester(requester)
                .addressee(target)
                .status(ConnectionStatus.PENDING)
                .build();
        UserConnection saved = userConnectionRepository.save(conn);
        eventPublisher.publishEvent(new ConnectionRequestedEvent(this, saved.getId(), requesterId, targetUserId));
        log.info("Sent connection request from {} to {}", requesterId, targetUserId);

        return toConnectionResponse(saved);
    }

    @Override
    public ConnectionRequestResponse acceptConnectionRequest(Long currentUserId, UUID requestId) {
        UserConnection conn = userConnectionRepository.findById(requestId)
                .orElseThrow(() -> new RelationshipNotFoundException("Connection request not found: " + requestId));

        if (!conn.getAddressee().getId().equals(currentUserId)) {
            throw new NetworkAccessDeniedException("You are not authorized to accept this connection request");
        }

        if (userBlockRepository.isBlockedEitherDirection(conn.getRequester().getId(), conn.getAddressee().getId())) {
            throw new NetworkAccessDeniedException("Cannot accept connection due to an active block relationship");
        }

        if (conn.getStatus() == ConnectionStatus.ACCEPTED) {
            return toConnectionResponse(conn);
        }

        if (conn.getStatus() != ConnectionStatus.PENDING) {
            throw new RelationshipConflictException("Cannot accept connection request in state: " + conn.getStatus());
        }

        conn.setStatus(ConnectionStatus.ACCEPTED);
        conn.setRespondedAt(Instant.now());
        UserConnection saved = userConnectionRepository.save(conn);
        eventPublisher.publishEvent(new ConnectionAcceptedEvent(this, saved.getId(), saved.getRequester().getId(), saved.getAddressee().getId()));
        log.info("User {} accepted connection request {}", currentUserId, requestId);

        return toConnectionResponse(saved);
    }

    @Override
    public ConnectionRequestResponse declineConnectionRequest(Long currentUserId, UUID requestId) {
        UserConnection conn = userConnectionRepository.findById(requestId)
                .orElseThrow(() -> new RelationshipNotFoundException("Connection request not found: " + requestId));

        if (!conn.getAddressee().getId().equals(currentUserId)) {
            throw new NetworkAccessDeniedException("You are not authorized to decline this connection request");
        }

        if (conn.getStatus() == ConnectionStatus.DECLINED) {
            return toConnectionResponse(conn);
        }

        if (conn.getStatus() != ConnectionStatus.PENDING) {
            throw new RelationshipConflictException("Cannot decline connection request in state: " + conn.getStatus());
        }

        conn.setStatus(ConnectionStatus.DECLINED);
        conn.setRespondedAt(Instant.now());
        UserConnection saved = userConnectionRepository.save(conn);
        log.info("User {} declined connection request {}", currentUserId, requestId);

        return toConnectionResponse(saved);
    }

    @Override
    public void cancelConnectionRequest(Long currentUserId, UUID requestId) {
        UserConnection conn = userConnectionRepository.findById(requestId)
                .orElseThrow(() -> new RelationshipNotFoundException("Connection request not found: " + requestId));

        if (!conn.getRequester().getId().equals(currentUserId)) {
            throw new NetworkAccessDeniedException("You are not authorized to cancel this connection request");
        }

        if (conn.getStatus() == ConnectionStatus.CANCELLED) {
            return;
        }

        if (conn.getStatus() != ConnectionStatus.PENDING) {
            throw new RelationshipConflictException("Cannot cancel connection request in state: " + conn.getStatus());
        }

        conn.setStatus(ConnectionStatus.CANCELLED);
        conn.setRespondedAt(Instant.now());
        userConnectionRepository.save(conn);
        log.info("User {} cancelled connection request {}", currentUserId, requestId);
    }

    @Override
    public void disconnect(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return;
        }
        userConnectionRepository.findBetweenWithStatus(currentUserId, targetUserId, ConnectionStatus.ACCEPTED)
                .ifPresent(userConnectionRepository::delete);
        log.info("User {} disconnected from user {}", currentUserId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public NetworkCursorPageResponse<ConnectionRequestResponse> getIncomingRequests(Long currentUserId, String cursor, int limit) {
        int pageSize = sanitizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<UserConnection> connections;
        if (cursor == null || cursor.isBlank()) {
            connections = userConnectionRepository.findInitialIncomingRequests(currentUserId, pageRequest);
        } else {
            CursorPayload payload = CursorUtils.decode(cursor);
            connections = userConnectionRepository.findIncomingRequestsAfterCursor(currentUserId, payload.timestamp(), payload.uuid(), pageRequest);
        }

        boolean hasNext = connections.size() > pageSize;
        List<UserConnection> pagedList = hasNext ? connections.subList(0, pageSize) : connections;

        String nextCursor = null;
        if (hasNext && !pagedList.isEmpty()) {
            UserConnection last = pagedList.get(pagedList.size() - 1);
            nextCursor = CursorUtils.encode(last.getCreatedAt(), last.getId());
        }

        List<ConnectionRequestResponse> items = pagedList.stream()
                .map(this::toConnectionResponse)
                .collect(Collectors.toList());

        return NetworkCursorPageResponse.<ConnectionRequestResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .limit(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NetworkCursorPageResponse<ConnectionRequestResponse> getOutgoingRequests(Long currentUserId, String cursor, int limit) {
        int pageSize = sanitizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<UserConnection> connections;
        if (cursor == null || cursor.isBlank()) {
            connections = userConnectionRepository.findInitialOutgoingRequests(currentUserId, pageRequest);
        } else {
            CursorPayload payload = CursorUtils.decode(cursor);
            connections = userConnectionRepository.findOutgoingRequestsAfterCursor(currentUserId, payload.timestamp(), payload.uuid(), pageRequest);
        }

        boolean hasNext = connections.size() > pageSize;
        List<UserConnection> pagedList = hasNext ? connections.subList(0, pageSize) : connections;

        String nextCursor = null;
        if (hasNext && !pagedList.isEmpty()) {
            UserConnection last = pagedList.get(pagedList.size() - 1);
            nextCursor = CursorUtils.encode(last.getCreatedAt(), last.getId());
        }

        List<ConnectionRequestResponse> items = pagedList.stream()
                .map(this::toConnectionResponse)
                .collect(Collectors.toList());

        return NetworkCursorPageResponse.<ConnectionRequestResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .limit(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NetworkCursorPageResponse<UserSummaryDto> getConnections(Long currentUserId, String cursor, int limit) {
        int pageSize = sanitizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<UserConnection> connections;
        if (cursor == null || cursor.isBlank()) {
            connections = userConnectionRepository.findInitialConnections(currentUserId, pageRequest);
        } else {
            CursorPayload payload = CursorUtils.decode(cursor);
            connections = userConnectionRepository.findConnectionsAfterCursor(currentUserId, payload.timestamp(), payload.uuid(), pageRequest);
        }

        boolean hasNext = connections.size() > pageSize;
        List<UserConnection> pagedList = hasNext ? connections.subList(0, pageSize) : connections;

        String nextCursor = null;
        if (hasNext && !pagedList.isEmpty()) {
            UserConnection last = pagedList.get(pagedList.size() - 1);
            Instant timestamp = last.getUpdatedAt() != null ? last.getUpdatedAt() : last.getCreatedAt();
            nextCursor = CursorUtils.encode(timestamp, last.getId());
        }

        List<UserSummaryDto> items = pagedList.stream()
                .map(c -> {
                    User other = c.getRequester().getId().equals(currentUserId) ? c.getAddressee() : c.getRequester();
                    return UserSummaryDto.from(other);
                })
                .collect(Collectors.toList());

        return NetworkCursorPageResponse.<UserSummaryDto>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .limit(pageSize)
                .build();
    }

    // ==========================================
    // BLOCK
    // ==========================================

    @Override
    public void blockUser(Long blockerId, Long targetUserId) {
        if (blockerId.equals(targetUserId)) {
            throw new InvalidRelationshipException("Cannot block yourself");
        }

        User blocker = getExistingUser(blockerId);
        User target = getExistingUser(targetUserId);

        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId)) {
            UserBlock block = UserBlock.builder()
                    .blocker(blocker)
                    .blocked(target)
                    .build();
            userBlockRepository.save(block);
        }

        // Transactionally break all follows and connections in both directions
        userFollowRepository.deleteAllFollowsBetween(blockerId, targetUserId);
        userConnectionRepository.deleteAllConnectionsBetween(blockerId, targetUserId);

        log.info("User {} blocked user {}. All follows and connections severed.", blockerId, targetUserId);
    }

    @Override
    public void unblockUser(Long blockerId, Long targetUserId) {
        if (blockerId.equals(targetUserId)) {
            return;
        }
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, targetUserId);
        log.info("User {} unblocked user {}", blockerId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public NetworkCursorPageResponse<UserSummaryDto> getBlockedUsers(Long currentUserId, String cursor, int limit) {
        int pageSize = sanitizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<UserBlock> blocks;
        if (cursor == null || cursor.isBlank()) {
            blocks = userBlockRepository.findInitialBlocked(currentUserId, pageRequest);
        } else {
            CursorPayload payload = CursorUtils.decode(cursor);
            blocks = userBlockRepository.findBlockedAfterCursor(currentUserId, payload.timestamp(), payload.uuid(), pageRequest);
        }

        boolean hasNext = blocks.size() > pageSize;
        List<UserBlock> pagedList = hasNext ? blocks.subList(0, pageSize) : blocks;

        String nextCursor = null;
        if (hasNext && !pagedList.isEmpty()) {
            UserBlock last = pagedList.get(pagedList.size() - 1);
            nextCursor = CursorUtils.encode(last.getCreatedAt(), last.getId());
        }

        List<UserSummaryDto> items = pagedList.stream()
                .map(b -> UserSummaryDto.from(b.getBlocked()))
                .collect(Collectors.toList());

        return NetworkCursorPageResponse.<UserSummaryDto>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .limit(pageSize)
                .build();
    }

    // ==========================================
    // MUTE
    // ==========================================

    @Override
    public void muteUser(Long muterId, Long targetUserId) {
        if (muterId.equals(targetUserId)) {
            throw new InvalidRelationshipException("Cannot mute yourself");
        }

        User muter = getExistingUser(muterId);
        User target = getExistingUser(targetUserId);

        if (!userMuteRepository.existsByMuterIdAndMutedId(muterId, targetUserId)) {
            UserMute mute = UserMute.builder()
                    .muter(muter)
                    .muted(target)
                    .build();
            userMuteRepository.save(mute);
        }
        log.info("User {} muted user {}", muterId, targetUserId);
    }

    @Override
    public void unmuteUser(Long muterId, Long targetUserId) {
        if (muterId.equals(targetUserId)) {
            return;
        }
        userMuteRepository.deleteByMuterIdAndMutedId(muterId, targetUserId);
        log.info("User {} unmuted user {}", muterId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public NetworkCursorPageResponse<UserSummaryDto> getMutedUsers(Long currentUserId, String cursor, int limit) {
        int pageSize = sanitizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<UserMute> mutes;
        if (cursor == null || cursor.isBlank()) {
            mutes = userMuteRepository.findInitialMuted(currentUserId, pageRequest);
        } else {
            CursorPayload payload = CursorUtils.decode(cursor);
            mutes = userMuteRepository.findMutedAfterCursor(currentUserId, payload.timestamp(), payload.uuid(), pageRequest);
        }

        boolean hasNext = mutes.size() > pageSize;
        List<UserMute> pagedList = hasNext ? mutes.subList(0, pageSize) : mutes;

        String nextCursor = null;
        if (hasNext && !pagedList.isEmpty()) {
            UserMute last = pagedList.get(pagedList.size() - 1);
            nextCursor = CursorUtils.encode(last.getCreatedAt(), last.getId());
        }

        List<UserSummaryDto> items = pagedList.stream()
                .map(m -> UserSummaryDto.from(m.getMuted()))
                .collect(Collectors.toList());

        return NetworkCursorPageResponse.<UserSummaryDto>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .limit(pageSize)
                .build();
    }

    // ==========================================
    // SOCIAL COUNTS
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public SocialCountsResponse getSocialCounts(Long userId) {
        getExistingUser(userId);
        long followers = userFollowRepository.countByFollowingId(userId);
        long following = userFollowRepository.countByFollowerId(userId);
        long connections = userConnectionRepository.countAcceptedConnections(userId);
        return new SocialCountsResponse(followers, following, connections);
    }

    // ==========================================
    // HELPER MAPPERS
    // ==========================================

    private FollowSummaryResponse toFollowSummary(UserFollow follow, User targetUser) {
        return FollowSummaryResponse.builder()
                .user(UserSummaryDto.from(targetUser))
                .followedAt(follow.getCreatedAt())
                .build();
    }

    private ConnectionRequestResponse toConnectionResponse(UserConnection conn) {
        return ConnectionRequestResponse.builder()
                .id(conn.getId())
                .requester(UserSummaryDto.from(conn.getRequester()))
                .addressee(UserSummaryDto.from(conn.getAddressee()))
                .status(conn.getStatus())
                .createdAt(conn.getCreatedAt())
                .respondedAt(conn.getRespondedAt())
                .build();
    }
}
