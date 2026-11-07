package com.collabsphere_first.learning.repository;

import com.collabsphere_first.learning.entity.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
    Optional<CourseEnrollment> findByCourseIdAndUserId(UUID courseId, Long userId);
    List<CourseEnrollment> findByUserId(Long userId);
    boolean existsByCourseIdAndUserId(UUID courseId, Long userId);
}

