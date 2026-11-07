package com.collabsphere_first.learning.repository;

import com.collabsphere_first.learning.entity.CourseCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseCertificateRepository extends JpaRepository<CourseCertificate, UUID> {
    Optional<CourseCertificate> findByCertificateNumber(String certificateNumber);
    Optional<CourseCertificate> findByCourseIdAndUserId(UUID courseId, Long userId);
    List<CourseCertificate> findByUserIdOrderByIssuedAtDesc(Long userId);
}

