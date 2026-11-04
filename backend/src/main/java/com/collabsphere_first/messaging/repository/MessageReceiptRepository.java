package com.collabsphere_first.messaging.repository;

import com.collabsphere_first.messaging.entity.MessageReceipt;
import com.collabsphere_first.messaging.entity.MessageReceiptId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, MessageReceiptId> {

    Optional<MessageReceipt> findByIdMessageIdAndIdUserId(UUID messageId, Long userId);

    List<MessageReceipt> findByIdMessageId(UUID messageId);

    List<MessageReceipt> findByIdMessageIdIn(Collection<UUID> messageIds);

    @Modifying
    @Query("UPDATE MessageReceipt mr SET mr.readAt = :readAt " +
           "WHERE mr.id.userId = :userId " +
           "AND mr.readAt IS NULL " +
           "AND mr.id.messageId IN (" +
           "    SELECT m.id FROM DirectMessage m " +
           "    WHERE m.conversationId = :conversationId " +
           "    AND m.senderId != :userId " +
           "    AND (m.createdAt < :targetCreatedAt OR (m.createdAt = :targetCreatedAt AND m.id <= :targetMessageId))" +
           ")")
    int markReadForUserUpTo(
            @Param("conversationId") UUID conversationId,
            @Param("userId") Long userId,
            @Param("targetCreatedAt") Instant targetCreatedAt,
            @Param("targetMessageId") UUID targetMessageId,
            @Param("readAt") Instant readAt
    );
}
