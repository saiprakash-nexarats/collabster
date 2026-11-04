package com.collabsphere_first.community.repository;
import com.collabsphere_first.community.entity.CommunityEvent;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
public interface CommunityEventRepository extends JpaRepository<CommunityEvent, UUID> {

    Page<CommunityEvent> findByCommunityIdOrderByStartsAtAsc(
            UUID communityId,
            Pageable pageable
    );

    Optional<CommunityEvent> findByIdAndCommunityId(
            UUID id,
            UUID communityId
    );

    long countByCommunity_IdAndCreatedAtBetween(
            UUID communityId,
            Instant from,
            Instant to
    );

    java.util.List<CommunityEvent> findByCommunity_IdAndCreatedAtBetween(
            UUID communityId,
            Instant from,
            Instant to
    );

    @Query("""
            SELECT event
            FROM CommunityEvent event
            JOIN event.community community
            WHERE community.visibility = :visibility
              AND community.communityStatus = :communityStatus
              AND event.endsAt > :now
            """)
    Page<CommunityEvent> findGlobalDiscoverableEvents(
            @Param("visibility") Visibility visibility,
            @Param("communityStatus") CommunityStatus communityStatus,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("""
            SELECT event
            FROM CommunityEvent event
            JOIN event.community community
            WHERE community.visibility = :visibility
              AND community.communityStatus = :communityStatus
              AND event.endsAt > :now
              AND event.startsAt >= :startsFrom
            """)
    Page<CommunityEvent> findGlobalDiscoverableEventsFrom(
            @Param("visibility") Visibility visibility,
            @Param("communityStatus") CommunityStatus communityStatus,
            @Param("now") Instant now,
            @Param("startsFrom") Instant startsFrom,
            Pageable pageable
    );

    @Query("""
            SELECT event
            FROM CommunityEvent event
            JOIN event.community community
            WHERE community.visibility = :visibility
              AND community.communityStatus = :communityStatus
              AND event.endsAt > :now
              AND event.startsAt < :startsBefore
            """)
    Page<CommunityEvent> findGlobalDiscoverableEventsBefore(
            @Param("visibility") Visibility visibility,
            @Param("communityStatus") CommunityStatus communityStatus,
            @Param("now") Instant now,
            @Param("startsBefore") Instant startsBefore,
            Pageable pageable
    );

    @Query("""
            SELECT event
            FROM CommunityEvent event
            JOIN event.community community
            WHERE community.visibility = :visibility
              AND community.communityStatus = :communityStatus
              AND event.endsAt > :now
              AND event.startsAt >= :startsFrom
              AND event.startsAt < :startsBefore
            """)
    Page<CommunityEvent> findGlobalDiscoverableEventsBetween(
            @Param("visibility") Visibility visibility,
            @Param("communityStatus") CommunityStatus communityStatus,
            @Param("now") Instant now,
            @Param("startsFrom") Instant startsFrom,
            @Param("startsBefore") Instant startsBefore,
            Pageable pageable
    );
}

