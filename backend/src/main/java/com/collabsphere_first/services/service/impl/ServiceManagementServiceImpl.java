package com.collabsphere_first.services.service.impl;

import com.collabsphere_first.services.dto.*;
import com.collabsphere_first.services.entity.ServiceBooking;
import com.collabsphere_first.services.entity.ServiceListing;
import com.collabsphere_first.services.entity.ServiceReview;
import com.collabsphere_first.services.repository.ServiceBookingRepository;
import com.collabsphere_first.services.repository.ServiceListingRepository;
import com.collabsphere_first.services.repository.ServiceReviewRepository;
import com.collabsphere_first.services.service.ServiceManagementService;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceManagementServiceImpl implements ServiceManagementService {

    private final ServiceListingRepository listingRepository;
    private final ServiceBookingRepository bookingRepository;
    private final ServiceReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceListingDto> listServices(String category, String query) {
        List<ServiceListing> listings;
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            listings = listingRepository.findByCategoryAndStateOrderByCreatedAtDesc(category.toLowerCase(Locale.ROOT), "active");
        } else {
            listings = listingRepository.findByStateOrderByCreatedAtDesc("active");
        }

        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase(Locale.ROOT).trim();
            listings = listings.stream()
                    .filter(s -> (s.getTitle() != null && s.getTitle().toLowerCase(Locale.ROOT).contains(q))
                            || (s.getDescription() != null && s.getDescription().toLowerCase(Locale.ROOT).contains(q))
                            || (s.getCategory() != null && s.getCategory().toLowerCase(Locale.ROOT).contains(q)))
                    .collect(Collectors.toList());
        }

        return listings.stream().map(this::mapToListingDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceListingDto getService(UUID id) {
        ServiceListing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service listing not found: " + id));
        return mapToListingDto(listing);
    }

    @Override
    @Transactional
    public ServiceListingDto createListing(CreateServiceListingRequest request, Long providerId) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service title cannot be blank");
        }

        BigDecimal startingPrice = request.getStartingPrice() != null ? request.getStartingPrice()
                : (request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
        long priceCents = request.getPriceCents() != null ? request.getPriceCents()
                : startingPrice.multiply(BigDecimal.valueOf(100)).longValue();

        if (priceCents < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service price cannot be negative");
        }

        String category = request.getCategory() != null && !request.getCategory().isBlank()
                ? request.getCategory().toLowerCase(Locale.ROOT).trim()
                : "software";

        ServiceListing listing = ServiceListing.builder()
                .providerId(providerId)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(category)
                .serviceArea(request.getServiceArea() != null ? request.getServiceArea() : "Remote (Global)")
                .serviceRadiusKm(request.getServiceRadiusKm() != null ? request.getServiceRadiusKm() : 15)
                .startingPrice(startingPrice)
                .priceCents(priceCents)
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase(Locale.ROOT) : "INR")
                .duration(request.getDuration() != null ? request.getDuration() : "3 days")
                .pricingUnit(request.getPricingUnit() != null ? request.getPricingUnit() : "Service visit")
                .deliveryMode(request.getDeliveryMode() != null ? request.getDeliveryMode() : "Remote (Global)")
                .pricingType(request.getPricingType() != null ? request.getPricingType() : "Fixed Package")
                .highlights(request.getHighlights())
                .state("active")
                .averageRating(BigDecimal.ZERO)
                .reviewCount(0)
                .build();

        ServiceListing saved = listingRepository.save(listing);
        log.info("Provider {} created service listing {} - '{}'", providerId, saved.getId(), saved.getTitle());
        return mapToListingDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceBookingDto> listUserBookings(Long userId) {
        List<ServiceBooking> bookings = bookingRepository.findAllUserBookings(userId);
        return bookings.stream().map(this::mapToBookingDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ServiceBookingDto bookService(UUID serviceId, CreateServiceBookingRequest request, Long customerId) {
        ServiceListing listing = listingRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + serviceId));

        if (!"active".equalsIgnoreCase(listing.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service is not currently accepting bookings");
        }

        if (listing.getProviderId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Providers cannot book their own services");
        }

        if (request.getStartsAt() != null) {
            if (request.getStartsAt().isBefore(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book appointments in the past");
            }
            // Double-booking check for conflicting slot time
            boolean conflict = bookingRepository.existsByServiceIdAndStartsAtAndStatusNotIn(
                    serviceId,
                    request.getStartsAt(),
                    List.of("cancelled", "rejected", "expired")
            );
            if (conflict) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Service is already booked for the selected appointment time");
            }
        }

        BigDecimal price = request.getPrice() != null ? request.getPrice() : listing.getStartingPrice();
        long amountCents = request.getAmountCents() != null ? request.getAmountCents()
                : price.multiply(BigDecimal.valueOf(100)).longValue();

        String generatedOrderId = "CL-ORD-" + (System.currentTimeMillis() % 100000);

        ServiceBooking booking = ServiceBooking.builder()
                .serviceId(serviceId)
                .customerId(customerId)
                .providerId(listing.getProviderId())
                .orderId(generatedOrderId)
                .title(listing.getTitle())
                .category(listing.getCategory())
                .tierName(request.getTierName() != null ? request.getTierName() : "Standard Package")
                .price(price)
                .amountCents(amountCents)
                .currency(request.getCurrency() != null ? request.getCurrency() : listing.getCurrency())
                .deliveryMode(request.getDeliveryMode() != null ? request.getDeliveryMode() : listing.getDeliveryMode())
                .status("requested")
                .startsAt(request.getStartsAt())
                .serviceAddress(request.getServiceAddress())
                .notes(request.getNotes())
                .milestones(request.getMilestones())
                .build();

        ServiceBooking saved = bookingRepository.save(booking);
        log.info("Customer {} booked service {} with booking {}", customerId, serviceId, saved.getId());
        return mapToBookingDto(saved);
    }

    @Override
    @Transactional
    public ServiceBookingDto confirmBooking(UUID bookingId, Long providerId) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found: " + bookingId));

        if (!booking.getProviderId().equals(providerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the designated service provider can confirm this booking");
        }

        if ("confirmed".equalsIgnoreCase(booking.getStatus())) {
            return mapToBookingDto(booking);
        }

        if (!"requested".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is in state '" + booking.getStatus() + "' and cannot be confirmed");
        }

        int updated = bookingRepository.updateStatusAtomically(bookingId, "requested", "confirmed", Instant.now());
        if (updated == 0) {
            ServiceBooking refreshed = bookingRepository.findById(bookingId).orElseThrow();
            if ("confirmed".equalsIgnoreCase(refreshed.getStatus())) {
                return mapToBookingDto(refreshed);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrent modification on booking status");
        }

        booking.setStatus("confirmed");
        booking.setUpdatedAt(Instant.now());
        log.info("Provider {} confirmed booking {}", providerId, bookingId);
        return mapToBookingDto(booking);
    }

    @Override
    @Transactional
    public ServiceBookingDto cancelBooking(UUID bookingId, Long userId) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found: " + bookingId));

        if (!booking.getCustomerId().equals(userId) && !booking.getProviderId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to cancel this booking");
        }

        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            return mapToBookingDto(booking);
        }

        if ("completed".equalsIgnoreCase(booking.getStatus()) || "rejected".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel a booking in terminal state: " + booking.getStatus());
        }

        booking.setStatus("cancelled");
        booking.setUpdatedAt(Instant.now());
        ServiceBooking saved = bookingRepository.save(booking);
        log.info("User {} cancelled booking {}", userId, bookingId);
        return mapToBookingDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceReviewDto> listReviews(UUID serviceId) {
        return reviewRepository.findByServiceIdOrderByCreatedAtDesc(serviceId).stream()
                .map(this::mapToReviewDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ServiceReviewDto createReview(UUID serviceId, CreateServiceReviewRequest request, Long authorId) {
        ServiceListing listing = listingRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + serviceId));

        if (authorId.equals(listing.getProviderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Providers cannot review their own service");
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be an integer between 1 and 5");
        }

        if (reviewRepository.existsByServiceIdAndAuthorId(serviceId, authorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this service");
        }

        // Customer eligibility: must have a booking for this service
        boolean hasBooking = bookingRepository.existsByServiceIdAndCustomerIdAndStatus(serviceId, authorId, "confirmed")
                || bookingRepository.existsByServiceIdAndCustomerIdAndStatus(serviceId, authorId, "completed")
                || bookingRepository.existsByServiceIdAndCustomerIdAndStatus(serviceId, authorId, "requested");

        if (!hasBooking) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customers who have booked this service can submit a review");
        }

        String authorName = userRepository.findById(authorId)
                .map(u -> (u.getFirstName() != null ? u.getFirstName() + " " + (u.getLastName() != null ? u.getLastName() : "") : u.getUsername()).trim())
                .orElse("Verified Client");

        ServiceReview review = ServiceReview.builder()
                .serviceId(serviceId)
                .bookingId(request.getBookingId())
                .authorId(authorId)
                .authorName(authorName)
                .rating(request.getRating())
                .reviewText(request.getReviewText())
                .build();

        ServiceReview saved = reviewRepository.save(review);

        // Authoritative aggregate calculation
        Double avgRating = reviewRepository.getAverageRatingByServiceId(serviceId);
        Long totalReviews = reviewRepository.countByServiceId(serviceId);

        listing.setAverageRating(avgRating != null ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        listing.setReviewCount(totalReviews != null ? totalReviews.intValue() : 0);
        listingRepository.save(listing);

        log.info("Customer {} created review {} with rating {} for service {}", authorId, saved.getId(), saved.getRating(), serviceId);
        return mapToReviewDto(saved);
    }

    private ServiceListingDto mapToListingDto(ServiceListing s) {
        String providerName = userRepository.findById(s.getProviderId())
                .map(u -> (u.getFirstName() != null ? u.getFirstName() + " " + (u.getLastName() != null ? u.getLastName() : "") : u.getUsername()).trim())
                .orElse("Verified Provider");

        return ServiceListingDto.builder()
                .id(s.getId())
                .providerId(s.getProviderId())
                .provider(providerName)
                .providerRole("Verified Specialist")
                .title(s.getTitle())
                .description(s.getDescription())
                .category(s.getCategory())
                .serviceArea(s.getServiceArea())
                .serviceRadiusKm(s.getServiceRadiusKm())
                .startingPrice(s.getStartingPrice())
                .price(s.getStartingPrice())
                .priceCents(s.getPriceCents())
                .currency(s.getCurrency())
                .duration(s.getDuration())
                .pricingUnit(s.getPricingUnit())
                .deliveryMode(s.getDeliveryMode())
                .pricingType(s.getPricingType())
                .highlights(s.getHighlights())
                .state(s.getState())
                .averageRating(s.getAverageRating())
                .reviewCount(s.getReviewCount())
                .remote(true)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private ServiceBookingDto mapToBookingDto(ServiceBooking b) {
        String providerName = userRepository.findById(b.getProviderId())
                .map(u -> (u.getFirstName() != null ? u.getFirstName() + " " + (u.getLastName() != null ? u.getLastName() : "") : u.getUsername()).trim())
                .orElse("Verified Provider");

        return ServiceBookingDto.builder()
                .id(b.getId())
                .orderId(b.getOrderId())
                .serviceId(b.getServiceId())
                .customerId(b.getCustomerId())
                .providerId(b.getProviderId())
                .provider(providerName)
                .title(b.getTitle())
                .category(b.getCategory())
                .tierName(b.getTierName())
                .price(b.getPrice())
                .amountCents(b.getAmountCents())
                .currency(b.getCurrency())
                .deliveryMode(b.getDeliveryMode())
                .status(b.getStatus())
                .orderStatus(b.getStatus())
                .startsAt(b.getStartsAt())
                .serviceAddress(b.getServiceAddress())
                .notes(b.getNotes())
                .milestones(b.getMilestones())
                .paymentOrderId(b.getPaymentOrderId())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    private ServiceReviewDto mapToReviewDto(ServiceReview r) {
        return ServiceReviewDto.builder()
                .id(r.getId())
                .serviceId(r.getServiceId())
                .bookingId(r.getBookingId())
                .authorId(r.getAuthorId())
                .authorName(r.getAuthorName())
                .rating(r.getRating())
                .reviewText(r.getReviewText())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
