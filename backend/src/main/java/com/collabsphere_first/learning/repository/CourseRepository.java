package com.collabsphere_first.learning.repository;

import com.collabsphere_first.learning.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByStateOrderByCreatedAtDesc(String state);
    List<Course> findByInstructorIdOrderByCreatedAtDesc(Long instructorId);
}
