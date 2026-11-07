package com.collabsphere_first.learning.repository;

import com.collabsphere_first.learning.entity.LessonCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, UUID> {
    boolean existsByLessonIdAndUserId(UUID lessonId, Long userId);
    long countByUserIdAndLessonIdIn(Long userId, List<UUID> lessonIds);
    List<LessonCompletion> findByUserIdAndLessonIdIn(Long userId, List<UUID> lessonIds);
}

