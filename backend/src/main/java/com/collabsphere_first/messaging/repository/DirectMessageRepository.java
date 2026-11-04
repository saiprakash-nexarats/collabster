package com.collabsphere_first.messaging.repository;

import com.collabsphere_first.messaging.entity.DirectMessage;
import com.collabsphere_first.messaging.enums.MessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    Optional<DirectMessage> findByConversationIdAndSenderIdAndClientMessageId(
            UUID conversationId,
            Long senderId,
            UUID clientMessageId
    );

    @Query("SELECT m FROM DirectMessage m " +
           "WHERE m.conversationId = :conversationId " +
           "ORDER BY m.createdAt DESC, m.id DESC")
    List<DirectMessage> findInitialMessages(
            @Param("conversationId") UUID conversationId,
            Pageable pageable
    );

    @Query("SELECT m FROM DirectMessage m " +
           "WHERE m.conversationId = :conversationId " +
           "AND (m.createdAt < :cursorTime OR (m.createdAt = :cursorTime AND m.id < :cursorId)) " +
           "ORDER BY m.createdAt DESC, m.id DESC")
    List<DirectMessage> findMessagesAfterCursor(
            @Param("conversationId") UUID conversationId,
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    long countByConversationIdAndSenderIdNotAndStatus(
            UUID conversationId, Long senderId, MessageStatus status
    );

    long countByConversationIdAndSenderIdNotAndStatusAndCreatedAtAfter(
            UUID conversationId, Long senderId, MessageStatus status, Instant createdAt
    );

    @Query("SELECT COUNT(m) FROM DirectMessage m " +
           "WHERE m.conversationId = :conversationId " +
           "AND m.senderId != :userId " +
           "AND m.status = :status " +
           "AND (m.createdAt > :cursorTime OR (m.createdAt = :cursorTime AND m.id > :cursorId))")
    long countUnreadAfterCursor(
            @Param("conversationId") UUID conversationId,
            @Param("userId") Long userId,
            @Param("status") MessageStatus status,
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId
    );

    Optional<DirectMessage> findFirstByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId);
}

