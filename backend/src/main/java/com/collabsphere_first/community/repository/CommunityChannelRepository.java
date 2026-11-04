package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityChannelRepository extends JpaRepository<CommunityChannel, UUID> {

    List<CommunityChannel> findByCommunityIdOrderByNameAsc(UUID communityId);

    @Query("""
        SELECT c
        FROM CommunityChannel c
        WHERE c.id = :channelId
          AND c.community.id = :communityId
    """)
    Optional<CommunityChannel> findChannelByIdAndCommunityId(
            @Param("channelId") UUID channelId,
            @Param("communityId") UUID communityId
    );

    @Query("""
        SELECT c
        FROM CommunityChannel c
        WHERE c.id = :channelId
          AND c.community.id = :communityId
    """)
    Optional<CommunityChannel> findChannelDirect(
            @Param("channelId") UUID channelId,
            @Param("communityId") UUID communityId
    );

    boolean existsByCommunityIdAndNameIgnoreCase(
            UUID communityId,
            String name
    );

    long countByCommunity_Id(UUID communityId);
}