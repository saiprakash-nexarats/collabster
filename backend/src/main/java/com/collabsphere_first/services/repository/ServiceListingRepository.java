package com.collabsphere_first.services.repository;

import com.collabsphere_first.services.entity.ServiceListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceListingRepository extends JpaRepository<ServiceListing, UUID> {
    List<ServiceListing> findByStateOrderByCreatedAtDesc(String state);
    List<ServiceListing> findByCategoryAndStateOrderByCreatedAtDesc(String category, String state);
    List<ServiceListing> findByProviderIdOrderByCreatedAtDesc(Long providerId);
    Optional<ServiceListing> findByIdAndState(UUID id, String state);
}

