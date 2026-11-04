package com.collabsphere_first.community.repository;
import com.collabsphere_first.community.entity.CommunityEventRsvp;
import com.collabsphere_first.community.enums.EventRsvpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface CommunityEventRsvpRepository extends JpaRepository<CommunityEventRsvp, UUID> {
    Optional<CommunityEventRsvp> findByEventIdAndUserId(UUID eventId, Long userId);
    Page<CommunityEventRsvp> findByEventIdAndStatusOrderByUpdatedAtDesc(UUID eventId, EventRsvpStatus status, Pageable pageable);
    long countByEventIdAndStatus(UUID eventId, EventRsvpStatus status);
    long countByEvent_Community_IdAndUpdatedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);
    java.util.List<CommunityEventRsvp> findByEvent_Community_IdAndUpdatedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);
}
