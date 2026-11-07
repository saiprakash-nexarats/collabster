package com.collabsphere_first.services.service;

import com.collabsphere_first.services.dto.*;

import java.util.List;
import java.util.UUID;

public interface ServiceManagementService {
    List<ServiceListingDto> listServices(String category, String query);
    ServiceListingDto getService(UUID id);
    ServiceListingDto createListing(CreateServiceListingRequest request, Long providerId);
    List<ServiceBookingDto> listUserBookings(Long userId);
    ServiceBookingDto bookService(UUID serviceId, CreateServiceBookingRequest request, Long customerId);
    ServiceBookingDto confirmBooking(UUID bookingId, Long providerId);
    ServiceBookingDto cancelBooking(UUID bookingId, Long userId);
    List<ServiceReviewDto> listReviews(UUID serviceId);
    ServiceReviewDto createReview(UUID serviceId, CreateServiceReviewRequest request, Long authorId);
}

