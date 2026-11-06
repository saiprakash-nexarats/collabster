package com.collabsphere_first.company.repository;

import com.collabsphere_first.company.entity.CompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, UUID> {
    List<CompanyDocument> findByCompanyIdAndIsArchivedFalseOrderByCreatedAtDesc(UUID companyId);
    Optional<CompanyDocument> findByCompanyIdAndIdAndIsArchivedFalse(UUID companyId, UUID documentId);
}
