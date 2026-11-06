package com.collabsphere_first.company.repository;

import com.collabsphere_first.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findById(UUID id);
    List<Company> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
