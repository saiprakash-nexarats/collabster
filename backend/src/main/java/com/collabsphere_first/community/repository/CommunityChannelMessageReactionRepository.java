package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityChannelMessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityChannelMessageReactionRepository extends JpaRepository<CommunityChannelMessageReaction, UUID> {
    List<CommunityChannelMessageReaction> findByMessageIdIn(Collection<UUID> messageIds);
    Optional<CommunityChannelMessageReaction> findByMessageIdAndUserIdAndReaction(UUID messageId, Long userId, String reaction);
}
