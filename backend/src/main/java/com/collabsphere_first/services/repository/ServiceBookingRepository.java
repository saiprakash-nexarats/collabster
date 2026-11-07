package com.collabsphere_first.services.repository;

import com.collabsphere_first.services.entity.ServiceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, UUID> {

    List<ServiceBooking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<ServiceBooking> findByProviderIdOrderByCreatedAtDesc(Long providerId);

    @Query("SELECT b FROM ServiceBooking b WHERE b.customerId = :userId OR b.providerId = :userId ORDER BY b.createdAt DESC")
    List<ServiceBooking> findAllUserBookings(@Param("userId") Long userId);

    Optional<ServiceBooking> findByIdAndCustomerId(UUID id, Long customerId);

    Optional<ServiceBooking> findByIdAndProviderId(UUID id, Long providerId);

    boolean existsByServiceIdAndStartsAtAndStatusNotIn(UUID serviceId, Instant startsAt, Collection<String> excludedStatuses);

    boolean existsByServiceIdAndCustomerIdAndStatus(UUID serviceId, Long customerId, String status);

    @Modifying
    @Query("UPDATE ServiceBooking b SET b.status = :newStatus, b.updatedAt = :now WHERE b.id = :bookingId AND b.status = :expectedStatus")
    int updateStatusAtomically(@Param("bookingId") UUID bookingId, @Param("expectedStatus") String expectedStatus, @Param("newStatus") String newStatus, @Param("now") Instant now);
}
