package com.collabsphere_first.project.repository;

import com.collabsphere_first.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, Long userId);
    List<ProjectMember> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ProjectMember> findByProjectIdOrderByCreatedAtAsc(UUID projectId);
    boolean existsByProjectIdAndUserIdAndMembershipState(UUID projectId, Long userId, String membershipState);
    boolean existsByProjectIdAndUserId(UUID projectId, Long userId);
}
