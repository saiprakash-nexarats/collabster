package com.collabsphere_first.messaging.repository;

import com.collabsphere_first.messaging.entity.ConversationParticipant;
import com.collabsphere_first.messaging.entity.ConversationParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

    Optional<ConversationParticipant> findByIdConversationIdAndIdUserId(UUID conversationId, Long userId);

    boolean existsByIdConversationIdAndIdUserId(UUID conversationId, Long userId);

    @Query("SELECT cp FROM ConversationParticipant cp JOIN FETCH cp.user WHERE cp.id.conversationId = :conversationId")
    List<ConversationParticipant> findByConversationIdWithUser(@Param("conversationId") UUID conversationId);

    List<ConversationParticipant> findByIdConversationId(UUID conversationId);

    List<ConversationParticipant> findByIdUserId(Long userId);
}
