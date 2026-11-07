package com.collabsphere_first.mentor.repository;

import com.collabsphere_first.mentor.entity.MentorSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MentorSessionRepository extends JpaRepository<MentorSession, UUID> {
    List<MentorSession> findByMentorIdOrLearnerIdOrderByStartsAtDesc(Long mentorId, Long learnerId);
    List<MentorSession> findByMentorIdOrderByStartsAtDesc(Long mentorId);
    List<MentorSession> findByLearnerIdOrderByStartsAtDesc(Long learnerId);
}

