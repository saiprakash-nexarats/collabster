package com.collabsphere_first.learning.repository;

import com.collabsphere_first.learning.entity.CourseLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseLessonRepository extends JpaRepository<CourseLesson, UUID> {
    List<CourseLesson> findByCourseIdOrderByOrderIndexAsc(UUID courseId);
    Optional<CourseLesson> findByCourseIdAndId(UUID courseId, UUID id);
    long countByCourseId(UUID courseId);
}
