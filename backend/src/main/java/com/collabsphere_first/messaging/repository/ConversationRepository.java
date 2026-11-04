package com.collabsphere_first.messaging.repository;

import com.collabsphere_first.messaging.entity.Conversation;
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
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByDirectUserLowIdAndDirectUserHighId(Long low, Long high);

    boolean existsByDirectUserLowIdAndDirectUserHighId(Long low, Long high);

    @Query("SELECT c FROM Conversation c " +
           "JOIN ConversationParticipant cp ON cp.id.conversationId = c.id " +
           "WHERE cp.id.userId = :userId " +
           "AND cp.archived = :archived " +
           "AND c.lastMessageId IS NOT NULL " +
           "ORDER BY c.lastMessageAt DESC, c.id DESC")
    List<Conversation> findInitialConversations(
            @Param("userId") Long userId,
            @Param("archived") boolean archived,
            Pageable pageable
    );

    @Query("SELECT c FROM Conversation c " +
           "JOIN ConversationParticipant cp ON cp.id.conversationId = c.id " +
           "WHERE cp.id.userId = :userId " +
           "AND cp.archived = :archived " +
           "AND c.lastMessageId IS NOT NULL " +
           "AND (c.lastMessageAt < :cursorTime OR (c.lastMessageAt = :cursorTime AND c.id < :cursorId)) " +
           "ORDER BY c.lastMessageAt DESC, c.id DESC")
    List<Conversation> findConversationsAfterCursor(
            @Param("userId") Long userId,
            @Param("archived") boolean archived,
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
