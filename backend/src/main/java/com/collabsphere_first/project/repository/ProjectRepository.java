package com.collabsphere_first.project.repository;

import com.collabsphere_first.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findById(UUID id);
    List<Project> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
