package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityChannelMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CommunityChannelMessageRepository extends JpaRepository<CommunityChannelMessage, UUID> {
    Page<CommunityChannelMessage> findByChannelIdAndCommunityId(UUID channelId, UUID communityId, Pageable pageable);
    long countByCommunityId(UUID communityId);
    long countByCommunityIdAndCreatedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);
    java.util.List<CommunityChannelMessage> findByCommunityIdAndCreatedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);

    @Query(value = """
            SELECT m.channel_id AS channelId, COUNT(m.id) AS unreadCount
            FROM community_channel_messages m
            LEFT JOIN community_channel_read_states s
              ON s.channel_id = m.channel_id AND s.user_id = :userId
            WHERE m.community_id = :communityId
              AND m.removed = FALSE
              AND m.sender_id <> :userId
              AND (s.last_read_at IS NULL OR m.created_at > s.last_read_at)
            GROUP BY m.channel_id
            """, nativeQuery = true)
    java.util.List<ChannelUnreadCount> countUnreadByChannel(@Param("communityId") UUID communityId, @Param("userId") Long userId);

    interface ChannelUnreadCount {
        UUID getChannelId();
        long getUnreadCount();
    }

    @Query("""
            SELECT m.channel.id AS channelId, COUNT(m.id) AS messageCount
            FROM CommunityChannelMessage m
            WHERE m.communityId = :communityId
              AND m.createdAt >= :from AND m.createdAt < :to
              AND m.removed = false
            GROUP BY m.channel.id
            """)
    java.util.List<ChannelMessageCount> countMessagesByChannel(
            @Param("communityId") UUID communityId,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);

    interface ChannelMessageCount {
        UUID getChannelId();
        long getMessageCount();
    }
}
