package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityPollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommunityPollVoteRepository extends JpaRepository<CommunityPollVote, UUID> {
    boolean existsByPollIdAndUserId(UUID pollId, Long userId);
    boolean existsByPollIdAndUserIdAndOptionId(UUID pollId, Long userId, UUID optionId);
    long countByOptionId(UUID optionId);
}
