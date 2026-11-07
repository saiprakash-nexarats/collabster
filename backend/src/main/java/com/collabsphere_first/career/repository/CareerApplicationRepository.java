package com.collabsphere_first.career.repository;

import com.collabsphere_first.career.entity.CareerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CareerApplicationRepository extends JpaRepository<CareerApplication, UUID> {
    List<CareerApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);
    List<CareerApplication> findByJobIdOrderByCreatedAtDesc(UUID jobId);
    Optional<CareerApplication> findByJobIdAndApplicantId(UUID jobId, Long applicantId);
    Optional<CareerApplication> findByIdAndJobId(UUID id, UUID jobId);
}
