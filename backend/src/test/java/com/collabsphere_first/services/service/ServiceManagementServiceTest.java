package com.collabsphere_first.services.service;

import com.collabsphere_first.services.dto.*;
import com.collabsphere_first.services.entity.ServiceBooking;
import com.collabsphere_first.services.entity.ServiceListing;
import com.collabsphere_first.services.entity.ServiceReview;
import com.collabsphere_first.services.repository.ServiceBookingRepository;
import com.collabsphere_first.services.repository.ServiceListingRepository;
import com.collabsphere_first.services.repository.ServiceReviewRepository;
import com.collabsphere_first.services.service.impl.ServiceManagementServiceImpl;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceManagementServiceTest {

    @Mock
    private ServiceListingRepository listingRepository;

    @Mock
    private ServiceBookingRepository bookingRepository;

    @Mock
    private ServiceReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ServiceManagementServiceImpl serviceManagementService;

    private Long providerId;
    private Long customerId;
    private Long otherUserId;
    private UUID serviceId;
    private ServiceListing listing;

    @BeforeEach
    void setUp() {
        providerId = 100L;
        customerId = 200L;
        otherUserId = 300L;
        serviceId = UUID.randomUUID();

        listing = ServiceListing.builder()
                .id(serviceId)
                .providerId(providerId)
                .title("Full Stack Web Development")
                .description("Production-grade React & Spring Boot applications")
                .category("software")
                .serviceArea("Remote (Global)")
                .serviceRadiusKm(15)
                .startingPrice(new BigDecimal("4999.00"))
                .priceCents(499900L)
                .currency("INR")
                .duration("7 days")
                .pricingUnit("Package")
                .deliveryMode("Remote (Global)")
                .pricingType("Milestone-based Package")
                .highlights("Code repo, deployment pipeline")
                .state("active")
                .averageRating(new BigDecimal("4.80"))
                .reviewCount(5)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createListing: creates listing with valid attributes and default values")
    void createListing_success() {
        CreateServiceListingRequest request = CreateServiceListingRequest.builder()
                .title("Cloud Infrastructure Audit")
                .description("Complete AWS/GCP security & cost review")
                .category("devops")
                .price(new BigDecimal("7500.00"))
                .duration("3 days")
                .deliveryMode("Remote")
                .build();

        when(listingRepository.save(any(ServiceListing.class))).thenAnswer(invocation -> {
            ServiceListing entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ServiceListingDto dto = serviceManagementService.createListing(request, providerId);

        assertThat(dto).isNotNull();
        assertThat(dto.getTitle()).isEqualTo("Cloud Infrastructure Audit");
        assertThat(dto.getCategory()).isEqualTo("devops");
        assertThat(dto.getPriceCents()).isEqualTo(750000L);
        assertThat(dto.getState()).isEqualTo("active");
        verify(listingRepository).save(any(ServiceListing.class));
    }

    @Test
    @DisplayName("createListing: rejects blank title with 400 Bad Request")
    void createListing_blankTitle_throwsBadRequest() {
        CreateServiceListingRequest request = CreateServiceListingRequest.builder()
                .title("   ")
                .price(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> serviceManagementService.createListing(request, providerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Service title cannot be blank");
    }

    @Test
    @DisplayName("createListing: rejects negative price with 400 Bad Request")
    void createListing_negativePrice_throwsBadRequest() {
        CreateServiceListingRequest request = CreateServiceListingRequest.builder()
                .title("Free Consulting")
                .priceCents(-500L)
                .build();

        assertThatThrownBy(() -> serviceManagementService.createListing(request, providerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("price cannot be negative");
    }

    @Test
    @DisplayName("listServices: filters by category and search query")
    void listServices_filtersCorrectly() {
        when(listingRepository.findByCategoryAndStateOrderByCreatedAtDesc("software", "active"))
                .thenReturn(List.of(listing));

        List<ServiceListingDto> results = serviceManagementService.listServices("software", "React");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDescription()).contains("React");
    }

    @Test
    @DisplayName("getService: returns existing listing DTO")
    void getService_success() {
        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));

        ServiceListingDto dto = serviceManagementService.getService(serviceId);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(serviceId);
        assertThat(dto.getTitle()).isEqualTo("Full Stack Web Development");
    }

    @Test
    @DisplayName("getService: throws 404 if listing not found")
    void getService_notFound() {
        when(listingRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceManagementService.getService(serviceId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("bookService: creates booking successfully for valid customer")
    void bookService_success() {
        Instant futureTime = Instant.now().plus(2, ChronoUnit.DAYS);
        CreateServiceBookingRequest request = CreateServiceBookingRequest.builder()
                .startsAt(futureTime)
                .notes("Need high availability setup")
                .milestones("Design, Deployment")
                .build();

        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByServiceIdAndStartsAtAndStatusNotIn(eq(serviceId), eq(futureTime), anyList()))
                .thenReturn(false);
        when(bookingRepository.save(any(ServiceBooking.class))).thenAnswer(invocation -> {
            ServiceBooking b = invocation.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        ServiceBookingDto dto = serviceManagementService.bookService(serviceId, request, customerId);

        assertThat(dto).isNotNull();
        assertThat(dto.getServiceId()).isEqualTo(serviceId);
        assertThat(dto.getCustomerId()).isEqualTo(customerId);
        assertThat(dto.getProviderId()).isEqualTo(providerId);
        assertThat(dto.getStatus()).isEqualTo("requested");
        assertThat(dto.getOrderId()).startsWith("CL-ORD-");
    }

    @Test
    @DisplayName("bookService: client price manipulation is ignored; server authoritative listing price is enforced")
    void bookService_clientPriceIgnored_enforcesAuthoritativeListingPrice() {
        // Client attempts to pay 1.00 instead of 4999.00
        CreateServiceBookingRequest request = CreateServiceBookingRequest.builder()
                .price(new BigDecimal("1.00"))
                .amountCents(100L)
                .currency("USD")
                .build();

        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));
        when(bookingRepository.save(any(ServiceBooking.class))).thenAnswer(i -> {
            ServiceBooking b = i.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        ServiceBookingDto dto = serviceManagementService.bookService(serviceId, request, customerId);

        assertThat(dto.getPrice()).isEqualTo(listing.getStartingPrice());
        assertThat(dto.getAmountCents()).isEqualTo(listing.getPriceCents());
        assertThat(dto.getCurrency()).isEqualTo(listing.getCurrency());
    }

    @Test
    @DisplayName("bookService: rejects unsupported service currency with 400 Bad Request")
    void bookService_unsupportedCurrency_throwsBadRequest() {
        listing.setCurrency("BITCOIN");
        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));

        CreateServiceBookingRequest request = CreateServiceBookingRequest.builder().build();

        assertThatThrownBy(() -> serviceManagementService.bookService(serviceId, request, customerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported service currency: BITCOIN");
    }

    @Test
    @DisplayName("bookService: rejects self-booking by provider with 400 Bad Request")
    void bookService_selfBooking_throwsBadRequest() {
        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));

        CreateServiceBookingRequest request = CreateServiceBookingRequest.builder().build();

        assertThatThrownBy(() -> serviceManagementService.bookService(serviceId, request, providerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Providers cannot book their own services");
    }

    @Test
    @DisplayName("bookService: rejects past appointment times with 400 Bad Request")
    void bookService_pastTime_throwsBadRequest() {
        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));

        Instant pastTime = Instant.now().minus(1, ChronoUnit.DAYS);
        CreateServiceBookingRequest request = CreateServiceBookingRequest.builder()
                .startsAt(pastTime)
                .build();

        assertThatThrownBy(() -> serviceManagementService.bookService(serviceId, request, customerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot book appointments in the past");
    }

    @Test
    @DisplayName("bookService: rejects conflicting slot time with 409 Conflict")
    void bookService_slotConflict_throwsConflict() {
        Instant slotTime = Instant.now().plus(1, ChronoUnit.DAYS);
        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByServiceIdAndStartsAtAndStatusNotIn(eq(serviceId), eq(slotTime), anyList()))
                .thenReturn(true);

        CreateServiceBookingRequest request = CreateServiceBookingRequest.builder()
                .startsAt(slotTime)
                .build();

        assertThatThrownBy(() -> serviceManagementService.bookService(serviceId, request, customerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("confirmBooking: provider can confirm requested booking")
    void confirmBooking_providerSuccess() {
        UUID bookingId = UUID.randomUUID();
        ServiceBooking booking = ServiceBooking.builder()
                .id(bookingId)
                .serviceId(serviceId)
                .providerId(providerId)
                .customerId(customerId)
                .status("requested")
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.updateStatusAtomically(eq(bookingId), eq("requested"), eq("confirmed"), any(Instant.class)))
                .thenReturn(1);

        ServiceBookingDto dto = serviceManagementService.confirmBooking(bookingId, providerId);

        assertThat(dto.getStatus()).isEqualTo("confirmed");
    }

    @Test
    @DisplayName("confirmBooking: customer or third party cannot confirm booking")
    void confirmBooking_nonProvider_throwsForbidden() {
        UUID bookingId = UUID.randomUUID();
        ServiceBooking booking = ServiceBooking.builder()
                .id(bookingId)
                .serviceId(serviceId)
                .providerId(providerId)
                .customerId(customerId)
                .status("requested")
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> serviceManagementService.confirmBooking(bookingId, customerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only the designated service provider can confirm this booking");
    }

    @Test
    @DisplayName("cancelBooking: customer or provider can cancel requested/confirmed booking")
    void cancelBooking_success() {
        UUID bookingId = UUID.randomUUID();
        ServiceBooking booking = ServiceBooking.builder()
                .id(bookingId)
                .serviceId(serviceId)
                .providerId(providerId)
                .customerId(customerId)
                .status("confirmed")
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(ServiceBooking.class))).thenAnswer(i -> i.getArgument(0));

        ServiceBookingDto dto = serviceManagementService.cancelBooking(bookingId, customerId);

        assertThat(dto.getStatus()).isEqualTo("cancelled");
    }

    @Test
    @DisplayName("cancelBooking: unauthorized user cannot cancel booking")
    void cancelBooking_unauthorized_throwsForbidden() {
        UUID bookingId = UUID.randomUUID();
        ServiceBooking booking = ServiceBooking.builder()
                .id(bookingId)
                .serviceId(serviceId)
                .providerId(providerId)
                .customerId(customerId)
                .status("confirmed")
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> serviceManagementService.cancelBooking(bookingId, otherUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not authorized to cancel");
    }

    @Test
    @DisplayName("cancelBooking: rejects cancelling completed booking")
    void cancelBooking_completed_throwsBadRequest() {
        UUID bookingId = UUID.randomUUID();
        ServiceBooking booking = ServiceBooking.builder()
                .id(bookingId)
                .serviceId(serviceId)
                .providerId(providerId)
                .customerId(customerId)
                .status("completed")
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> serviceManagementService.cancelBooking(bookingId, customerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    @DisplayName("createReview: valid customer can submit rating and review, updating listing aggregate")
    void createReview_success() {
        CreateServiceReviewRequest request = CreateServiceReviewRequest.builder()
                .rating(5)
                .reviewText("Outstanding work, highly recommended!")
                .build();

        User customer = User.builder()
                .id(customerId)
                .firstName("Sai")
                .lastName("Prakash")
                .username("saiprakash")
                .build();

        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));
        when(reviewRepository.existsByServiceIdAndAuthorId(serviceId, customerId)).thenReturn(false);
        when(bookingRepository.existsByServiceIdAndCustomerIdAndStatus(serviceId, customerId, "confirmed")).thenReturn(true);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(reviewRepository.save(any(ServiceReview.class))).thenAnswer(invocation -> {
            ServiceReview r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(Instant.now());
            return r;
        });
        when(reviewRepository.getAverageRatingByServiceId(serviceId)).thenReturn(4.90);
        when(reviewRepository.countByServiceId(serviceId)).thenReturn(6L);

        ServiceReviewDto reviewDto = serviceManagementService.createReview(serviceId, request, customerId);

        assertThat(reviewDto).isNotNull();
        assertThat(reviewDto.getRating()).isEqualTo(5);
        assertThat(reviewDto.getAuthorName()).isEqualTo("Sai Prakash");
        verify(listingRepository).save(listing);
        assertThat(listing.getAverageRating()).isEqualTo(new BigDecimal("4.90"));
        assertThat(listing.getReviewCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("createReview: provider cannot review own service")
    void createReview_providerSelfReview_throwsBadRequest() {
        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));

        CreateServiceReviewRequest request = CreateServiceReviewRequest.builder()
                .rating(5)
                .build();

        assertThatThrownBy(() -> serviceManagementService.createReview(serviceId, request, providerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Providers cannot review their own service");
    }

    @Test
    @DisplayName("createReview: rejects invalid rating outside 1 to 5")
    void createReview_invalidRating_throwsBadRequest() {
        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));

        CreateServiceReviewRequest request = CreateServiceReviewRequest.builder()
                .rating(6)
                .build();

        assertThatThrownBy(() -> serviceManagementService.createReview(serviceId, request, customerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Rating must be an integer between 1 and 5");
    }

    @Test
    @DisplayName("createReview: non-customer without booking cannot review")
    void createReview_noBooking_throwsForbidden() {
        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));
        when(reviewRepository.existsByServiceIdAndAuthorId(serviceId, customerId)).thenReturn(false);
        when(bookingRepository.existsByServiceIdAndCustomerIdAndStatus(serviceId, customerId, "confirmed")).thenReturn(false);
        when(bookingRepository.existsByServiceIdAndCustomerIdAndStatus(serviceId, customerId, "completed")).thenReturn(false);

        CreateServiceReviewRequest request = CreateServiceReviewRequest.builder()
                .rating(5)
                .build();

        assertThatThrownBy(() -> serviceManagementService.createReview(serviceId, request, customerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only customers who have booked this service can submit a review");
    }

    @Test
    @DisplayName("createReview: duplicate review returns 409 Conflict")
    void createReview_duplicate_throwsConflict() {
        when(listingRepository.findById(serviceId)).thenReturn(Optional.of(listing));
        when(reviewRepository.existsByServiceIdAndAuthorId(serviceId, customerId)).thenReturn(true);

        CreateServiceReviewRequest request = CreateServiceReviewRequest.builder()
                .rating(5)
                .build();

        assertThatThrownBy(() -> serviceManagementService.createReview(serviceId, request, customerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already reviewed");
    }
}
