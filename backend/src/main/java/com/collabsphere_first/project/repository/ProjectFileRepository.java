package com.collabsphere_first.project.repository;

import com.collabsphere_first.project.entity.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFile, UUID> {
    List<ProjectFile> findByProjectIdOrderByPathAsc(UUID projectId);
    Optional<ProjectFile> findByProjectIdAndPath(UUID projectId, String path);
    Optional<ProjectFile> findByProjectIdAndId(UUID projectId, UUID fileId);
    void deleteByProjectIdAndId(UUID projectId, UUID fileId);
}
