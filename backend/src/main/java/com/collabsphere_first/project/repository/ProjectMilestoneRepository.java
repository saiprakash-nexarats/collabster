package com.collabsphere_first.project.repository;

import com.collabsphere_first.project.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID> {
    List<ProjectMilestone> findByProjectIdOrderByCreatedAtAsc(UUID projectId);
    Optional<ProjectMilestone> findByProjectIdAndId(UUID projectId, UUID milestoneId);
}
