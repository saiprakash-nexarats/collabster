package com.collabsphere_first.messaging.repository;

import com.collabsphere_first.messaging.entity.StarredMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StarredMessageRepository extends JpaRepository<StarredMessage, UUID> {

    boolean existsByUserIdAndMessageId(Long userId, UUID messageId);

    Optional<StarredMessage> findByUserIdAndMessageId(Long userId, UUID messageId);

    void deleteByUserIdAndMessageId(Long userId, UUID messageId);

    @Query("SELECT sm FROM StarredMessage sm JOIN FETCH sm.message m WHERE sm.user.id = :userId ORDER BY sm.starredAt DESC")
    List<StarredMessage> findByUserIdWithMessages(@Param("userId") Long userId);
}

