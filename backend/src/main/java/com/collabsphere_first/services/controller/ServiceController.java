package com.collabsphere_first.services.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.services.dto.*;
import com.collabsphere_first.services.service.ServiceManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/v1/services", "/api/v1/services"})
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceManagementService serviceManagementService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceListingDto>>> listServices(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query) {
        List<ServiceListingDto> listings = serviceManagementService.listServices(category, query);
        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<ServiceBookingDto>>> listBookings() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<ServiceBookingDto> bookings = serviceManagementService.listUserBookings(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceListingDto>> createListing(
            @Valid @RequestBody CreateServiceListingRequest request) {
        Long providerId = currentUserService.getCurrentUser().getId();
        ServiceListingDto listing = serviceManagementService.createListing(request, providerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(listing));
    }

    @PostMapping("/{id}/bookings")
    public ResponseEntity<ApiResponse<ServiceBookingDto>> bookService(
            @PathVariable UUID id,
            @Valid @RequestBody CreateServiceBookingRequest request) {
        Long customerId = currentUserService.getCurrentUser().getId();
        ServiceBookingDto booking = serviceManagementService.bookService(id, request, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(booking));
    }

    @PostMapping("/bookings/{id}/confirm")
    public ResponseEntity<ApiResponse<ServiceBookingDto>> confirmBooking(
            @PathVariable UUID id) {
        Long providerId = currentUserService.getCurrentUser().getId();
        ServiceBookingDto booking = serviceManagementService.confirmBooking(id, providerId);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<ServiceBookingDto>> cancelBooking(
            @PathVariable UUID id) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ServiceBookingDto booking = serviceManagementService.cancelBooking(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<List<ServiceReviewDto>>> listReviews(
            @PathVariable UUID id) {
        List<ServiceReviewDto> reviews = serviceManagementService.listReviews(id);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<ServiceReviewDto>> createReview(
            @PathVariable UUID id,
            @Valid @RequestBody CreateServiceReviewRequest request) {
        Long authorId = currentUserService.getCurrentUser().getId();
        ServiceReviewDto review = serviceManagementService.createReview(id, request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(review));
    }
}
