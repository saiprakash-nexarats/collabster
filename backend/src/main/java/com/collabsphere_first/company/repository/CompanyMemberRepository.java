package com.collabsphere_first.company.repository;

import com.collabsphere_first.company.entity.CompanyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyMemberRepository extends JpaRepository<CompanyMember, UUID> {
    Optional<CompanyMember> findByCompanyIdAndUserId(UUID companyId, Long userId);
    List<CompanyMember> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CompanyMember> findByCompanyIdOrderByCreatedAtAsc(UUID companyId);
    boolean existsByCompanyIdAndUserIdAndMembershipState(UUID companyId, Long userId, String membershipState);
    boolean existsByCompanyIdAndUserId(UUID companyId, Long userId);
}
