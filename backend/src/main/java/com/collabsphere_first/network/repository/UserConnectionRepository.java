package com.collabsphere_first.network.repository;

import com.collabsphere_first.network.entity.UserConnection;
import com.collabsphere_first.network.enums.ConnectionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnection, UUID> {

    @Query("SELECT c FROM UserConnection c WHERE " +
           "(c.requester.id = :userA AND c.addressee.id = :userB) OR " +
           "(c.requester.id = :userB AND c.addressee.id = :userA)")
    Optional<UserConnection> findBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("SELECT c FROM UserConnection c WHERE " +
           "((c.requester.id = :userA AND c.addressee.id = :userB) OR " +
           " (c.requester.id = :userB AND c.addressee.id = :userA)) AND c.status = :status")
    Optional<UserConnection> findBetweenWithStatus(
            @Param("userA") Long userA,
            @Param("userB") Long userB,
            @Param("status") ConnectionStatus status
    );

    @Query("SELECT COUNT(c) > 0 FROM UserConnection c WHERE " +
           "((c.requester.id = :userA AND c.addressee.id = :userB) OR " +
           " (c.requester.id = :userB AND c.addressee.id = :userA)) AND c.status = 'ACCEPTED'")
    boolean isConnected(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("SELECT COUNT(c) FROM UserConnection c WHERE " +
           "(c.requester.id = :userId OR c.addressee.id = :userId) AND c.status = 'ACCEPTED'")
    long countAcceptedConnections(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserConnection c WHERE " +
           "(c.requester.id = :userA AND c.addressee.id = :userB) OR " +
           "(c.requester.id = :userB AND c.addressee.id = :userA)")
    void deleteAllConnectionsBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    // Incoming requests for addressee
    @Query("SELECT c FROM UserConnection c JOIN FETCH c.requester WHERE c.addressee.id = :addresseeId AND c.status = 'PENDING' ORDER BY c.createdAt DESC, c.id DESC")
    List<UserConnection> findInitialIncomingRequests(@Param("addresseeId") Long addresseeId, Pageable pageable);

    @Query("SELECT c FROM UserConnection c JOIN FETCH c.requester WHERE c.addressee.id = :addresseeId AND c.status = 'PENDING' " +
           "AND (c.createdAt < :cursorCreatedAt OR (c.createdAt = :cursorCreatedAt AND c.id < :cursorId)) " +
           "ORDER BY c.createdAt DESC, c.id DESC")
    List<UserConnection> findIncomingRequestsAfterCursor(
            @Param("addresseeId") Long addresseeId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    // Outgoing requests for requester
    @Query("SELECT c FROM UserConnection c JOIN FETCH c.addressee WHERE c.requester.id = :requesterId AND c.status = 'PENDING' ORDER BY c.createdAt DESC, c.id DESC")
    List<UserConnection> findInitialOutgoingRequests(@Param("requesterId") Long requesterId, Pageable pageable);

    @Query("SELECT c FROM UserConnection c JOIN FETCH c.addressee WHERE c.requester.id = :requesterId AND c.status = 'PENDING' " +
           "AND (c.createdAt < :cursorCreatedAt OR (c.createdAt = :cursorCreatedAt AND c.id < :cursorId)) " +
           "ORDER BY c.createdAt DESC, c.id DESC")
    List<UserConnection> findOutgoingRequestsAfterCursor(
            @Param("requesterId") Long requesterId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    // Accepted connections for a user
    @Query("SELECT c FROM UserConnection c JOIN FETCH c.requester JOIN FETCH c.addressee WHERE " +
           "(c.requester.id = :userId OR c.addressee.id = :userId) AND c.status = 'ACCEPTED' ORDER BY c.updatedAt DESC, c.id DESC")
    List<UserConnection> findInitialConnections(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM UserConnection c JOIN FETCH c.requester JOIN FETCH c.addressee WHERE " +
           "(c.requester.id = :userId OR c.addressee.id = :userId) AND c.status = 'ACCEPTED' " +
           "AND (c.updatedAt < :cursorUpdatedAt OR (c.updatedAt = :cursorUpdatedAt AND c.id < :cursorId)) " +
           "ORDER BY c.updatedAt DESC, c.id DESC")
    List<UserConnection> findConnectionsAfterCursor(
            @Param("userId") Long userId,
            @Param("cursorUpdatedAt") Instant cursorUpdatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("SELECT CASE WHEN c.requester.id = :userId THEN c.addressee.id ELSE c.requester.id END " +
           "FROM UserConnection c WHERE (c.requester.id = :userId OR c.addressee.id = :userId) AND c.status = 'ACCEPTED' ORDER BY c.updatedAt DESC")
    List<Long> findConnectedUserIds(@Param("userId") Long userId, Pageable pageable);
}
