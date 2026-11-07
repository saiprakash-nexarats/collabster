package com.collabsphere_first.services.repository;

import com.collabsphere_first.services.entity.ServiceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceReviewRepository extends JpaRepository<ServiceReview, UUID> {

    List<ServiceReview> findByServiceIdOrderByCreatedAtDesc(UUID serviceId);

    boolean existsByServiceIdAndAuthorId(UUID serviceId, Long authorId);

    @Query("SELECT AVG(CAST(r.rating AS double)) FROM ServiceReview r WHERE r.serviceId = :serviceId")
    Double getAverageRatingByServiceId(@Param("serviceId") UUID serviceId);

    @Query("SELECT COUNT(r) FROM ServiceReview r WHERE r.serviceId = :serviceId")
    Long countByServiceId(@Param("serviceId") UUID serviceId);
}
