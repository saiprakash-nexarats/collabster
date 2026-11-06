package com.collabsphere_first.project.repository;

import com.collabsphere_first.project.entity.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, UUID> {
    List<ProjectTask> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<ProjectTask> findByProjectIdAndId(UUID projectId, UUID taskId);
}
