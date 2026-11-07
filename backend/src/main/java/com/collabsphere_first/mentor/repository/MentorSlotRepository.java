package com.collabsphere_first.mentor.repository;

import com.collabsphere_first.mentor.entity.MentorSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MentorSlotRepository extends JpaRepository<MentorSlot, UUID> {
    List<MentorSlot> findByMentorIdAndBookedFalseAndSlotTimeAfterOrderBySlotTimeAsc(Long mentorId, Instant now);
    List<MentorSlot> findByMentorIdOrderBySlotTimeAsc(Long mentorId);
    Optional<MentorSlot> findByIdAndMentorId(UUID id, Long mentorId);
}
