package com.collabsphere_first.company.repository;

import com.collabsphere_first.company.entity.CompanyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyTaskRepository extends JpaRepository<CompanyTask, UUID> {
    List<CompanyTask> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    Optional<CompanyTask> findByCompanyIdAndId(UUID companyId, UUID taskId);
}
