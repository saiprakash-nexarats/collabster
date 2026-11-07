package com.collabsphere_first.p3;

import com.collabsphere_first.learning.entity.Course;
import com.collabsphere_first.learning.entity.CourseCertificate;
import com.collabsphere_first.learning.entity.CourseEnrollment;
import com.collabsphere_first.learning.repository.CourseCertificateRepository;
import com.collabsphere_first.learning.repository.CourseEnrollmentRepository;
import com.collabsphere_first.learning.repository.CourseRepository;
import com.collabsphere_first.mentor.entity.MentorSlot;
import com.collabsphere_first.mentor.repository.MentorSlotRepository;
import com.collabsphere_first.payment.dto.*;
import com.collabsphere_first.payment.entity.*;
import com.collabsphere_first.payment.repository.*;
import com.collabsphere_first.payment.service.PaymentService;
import com.collabsphere_first.services.dto.*;
import com.collabsphere_first.services.entity.ServiceBooking;
import com.collabsphere_first.services.entity.ServiceListing;
import com.collabsphere_first.services.entity.ServiceReview;
import com.collabsphere_first.services.repository.ServiceBookingRepository;
import com.collabsphere_first.services.repository.ServiceListingRepository;
import com.collabsphere_first.services.repository.ServiceReviewRepository;
import com.collabsphere_first.services.service.ServiceManagementService;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.Socket;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "jwt.secret=c2FpLXByYWthc2gtbmV4YXJhdHMtY29sbGFic3Rlci10ZXN0LWp3dC1zZWNyZXQta2V5LXNob3VsZC1iZS1hdC1sZWFzdC0yNTYtYml0cy02NC1jaGFyYWN0ZXJzLWxvbmchIQ=="
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("isPostgresAvailable")
public class ServicesAndPaymentsPostgreSqlIntegrationTest {

    static boolean isPostgresAvailable() {
        try (Socket socket = new Socket("localhost", 5432)) {
            return true;
        } catch (Exception e) {
            if ("true".equalsIgnoreCase(System.getenv("CI"))) {
                throw new IllegalStateException("PostgreSQL 17 is required in CI environment but unreachable on localhost:5432", e);
            }
            return false;
        }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceListingRepository serviceListingRepository;

    @Autowired
    private ServiceBookingRepository serviceBookingRepository;

    @Autowired
    private ServiceReviewRepository serviceReviewRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentWebhookEventRepository paymentWebhookEventRepository;

    @Autowired
    private PayoutRequestRepository payoutRequestRepository;

    @Autowired
    private FinancialLedgerRepository financialLedgerRepository;

    @Autowired
    private MentorSlotRepository mentorSlotRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private CourseCertificateRepository courseCertificateRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ServiceManagementService serviceManagementService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User provider;
    private User customer;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            provider = userRepository.save(User.builder()
                    .firstName("Arun")
                    .lastName("Verma")
                    .email("provider-" + UUID.randomUUID() + "@collobster.test")
                    .password("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890")
                    .provider(AuthProvider.LOCAL)
                    .emailVerified(true)
                    .accountEnabled(true)
                    .build());

            customer = userRepository.save(User.builder()
                    .firstName("Divya")
                    .lastName("Sharma")
                    .email("customer-" + UUID.randomUUID() + "@collobster.test")
                    .password("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890")
                    .provider(AuthProvider.LOCAL)
                    .emailVerified(true)
                    .accountEnabled(true)
                    .build());
            return null;
        });
    }

    @Test
    @Transactional
    @DisplayName("Services persistence: listing, booking, and reviews with aggregate calculations in PostgreSQL")
    void testServicesPersistenceAndAggregates() {
        ServiceListing listing = serviceListingRepository.save(ServiceListing.builder()
                .providerId(provider.getId())
                .title("Full Stack Web Application Development")
                .description("Production Spring Boot & React engineering")
                .category("software")
                .serviceArea("Remote (Global)")
                .serviceRadiusKm(20)
                .startingPrice(new BigDecimal("15000.00"))
                .priceCents(1500000L)
                .currency("INR")
                .duration("10 days")
                .pricingUnit("Milestone")
                .deliveryMode("Remote")
                .pricingType("Milestone-based Package")
                .highlights("Full source code, 30-day bug warranty")
                .state("active")
                .averageRating(BigDecimal.ZERO)
                .reviewCount(0)
                .build());

        assertThat(listing.getId()).isNotNull();

        ServiceBooking booking = serviceBookingRepository.save(ServiceBooking.builder()
                .serviceId(listing.getId())
                .customerId(customer.getId())
                .providerId(provider.getId())
                .orderId("CL-ORD-77712")
                .title(listing.getTitle())
                .category(listing.getCategory())
                .tierName("Full Stack Pro")
                .price(new BigDecimal("15000.00"))
                .amountCents(1500000L)
                .currency("INR")
                .deliveryMode("Remote")
                .status("requested")
                .startsAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .notes("Need responsive design and API documentation")
                .milestones("Phase 1 Architecture, Phase 2 Implementation, Phase 3 Verification")
                .build());

        assertThat(booking.getId()).isNotNull();
        assertThat(serviceBookingRepository.findAllUserBookings(customer.getId())).hasSize(1);

        // Submit review
        ServiceReview review = serviceReviewRepository.save(ServiceReview.builder()
                .serviceId(listing.getId())
                .bookingId(booking.getId())
                .authorId(customer.getId())
                .authorName("Divya Sharma")
                .rating(5)
                .reviewText("Exceptional delivery and clean architecture!")
                .build());

        assertThat(review.getId()).isNotNull();

        Double avg = serviceReviewRepository.getAverageRatingByServiceId(listing.getId());
        Long totalReviews = serviceReviewRepository.countByServiceId(listing.getId());

        assertThat(avg).isEqualTo(5.0);
        assertThat(totalReviews).isEqualTo(1L);

        // Duplicate review by same author rejected by unique constraint
        assertThatThrownBy(() -> {
            serviceReviewRepository.saveAndFlush(ServiceReview.builder()
                    .serviceId(listing.getId())
                    .bookingId(booking.getId())
                    .authorId(customer.getId())
                    .authorName("Divya Sharma")
                    .rating(4)
                    .reviewText("Second review attempt")
                    .build());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    @DisplayName("Payments persistence: orders, transactions, ledger entries and balance aggregation")
    void testPaymentsPersistenceAndLedgerCalculation() {
        PaymentOrder order = paymentOrderRepository.save(PaymentOrder.builder()
                .userId(customer.getId())
                .orderNumber("ORD-PGTEST-1001")
                .amountCents(1500000L)
                .currency("INR")
                .status("created")
                .description("Service milestone payment")
                .gatewayProvider("SandboxMock")
                .gatewayOrderId("sandbox_ord_pg_1")
                .build());

        assertThat(order.getId()).isNotNull();

        PaymentTransaction tx = paymentTransactionRepository.save(PaymentTransaction.builder()
                .orderId(order.getId())
                .transactionType("PAYMENT")
                .amountCents(1500000L)
                .currency("INR")
                .gatewayProvider("SandboxMock")
                .gatewayTransactionId("pay_pg_tx_1")
                .status("SUCCESS")
                .rawResponse("Sandbox payment captured")
                .build());

        assertThat(tx.getId()).isNotNull();

        // Provider receives credit in ledger
        financialLedgerRepository.save(FinancialLedgerEntry.builder()
                .userId(provider.getId())
                .entryType("PAYMENT_CREDIT")
                .amountCents(1500000L)
                .currency("INR")
                .balanceAfterCents(1500000L)
                .referenceType("PAYMENT_ORDER")
                .referenceId(order.getId())
                .description("Earnings credit")
                .build());

        long balanceAfterCredit = financialLedgerRepository.calculateUserBalanceCents(provider.getId());
        assertThat(balanceAfterCredit).isEqualTo(1500000L);

        // Provider requests payout
        PayoutRequest payout = payoutRequestRepository.save(PayoutRequest.builder()
                .providerId(provider.getId())
                .amountCents(500000L)
                .currency("INR")
                .method("bank_account")
                .destinationReference("ICIC0001122-ACC3344")
                .status("completed")
                .gatewayPayoutId("pout_pg_1")
                .notes("Withdrawal")
                .processedAt(Instant.now())
                .build());

        assertThat(payout.getId()).isNotNull();

        financialLedgerRepository.save(FinancialLedgerEntry.builder()
                .userId(provider.getId())
                .entryType("PAYOUT_DEBIT")
                .amountCents(500000L)
                .currency("INR")
                .balanceAfterCents(1000000L)
                .referenceType("PAYOUT_REQUEST")
                .referenceId(payout.getId())
                .description("Payout withdrawal")
                .build());

        long balanceAfterPayout = financialLedgerRepository.calculateUserBalanceCents(provider.getId());
        assertThat(balanceAfterPayout).isEqualTo(1000000L);
    }

    @Test
    @Transactional
    @DisplayName("Webhook replay deduplication: unique event ID constraint in PostgreSQL")
    void testWebhookReplayDeduplication() {
        paymentWebhookEventRepository.save(PaymentWebhookEvent.builder()
                .eventId("evt_unique_1001")
                .provider("SandboxMock")
                .eventType("payment.captured")
                .payload("{\"event\":\"payment.captured\"}")
                .signature("sig_abc")
                .processed(true)
                .processedAt(Instant.now())
                .build());

        assertThat(paymentWebhookEventRepository.existsByEventId("evt_unique_1001")).isTrue();

        // Attempting to insert duplicate eventId throws DataIntegrityViolationException
        assertThatThrownBy(() -> {
            paymentWebhookEventRepository.saveAndFlush(PaymentWebhookEvent.builder()
                    .eventId("evt_unique_1001")
                    .provider("SandboxMock")
                    .eventType("payment.captured")
                    .payload("{\"event\":\"duplicate\"}")
                    .signature("sig_abc")
                    .processed(false)
                    .build());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("P1/P2 Closure: Real PostgreSQL concurrent booking test for mentor slots")
    void testConcurrentMentorSlotBookingOnPostgreSql() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        UUID slotId = tx.execute(status -> {
            MentorSlot slot = mentorSlotRepository.save(MentorSlot.builder()
                    .mentorId(provider.getId())
                    .slotTime(Instant.now().plus(3, ChronoUnit.DAYS))
                    .durationMinutes(60)
                    .booked(false)
                    .build());
            return slot.getId();
        });

        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    // Each thread executes atomic reservation in its own database transaction
                    Integer rowsUpdated = tx.execute(status ->
                            mentorSlotRepository.reserveSlotAtomically(slotId, Instant.now()));
                    if (rowsUpdated != null && rowsUpdated == 1) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(successCount.get())
                .as("Exactly one concurrent thread must succeed in booking the mentor slot")
                .isEqualTo(1);
        assertThat(rejectedCount.get())
                .as("All other concurrent attempts must be rejected with 0 rows updated")
                .isEqualTo(numThreads - 1);

        MentorSlot persistedSlot = mentorSlotRepository.findById(slotId).orElseThrow();
        assertThat(persistedSlot.getBooked()).isTrue();
    }

    @Test
    @DisplayName("P1/P2 Closure: Certificate claim idempotency under concurrent PostgreSQL transactions")
    void testConcurrentCertificateClaimIdempotencyOnPostgreSql() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        UUID courseId = tx.execute(status -> {
            Course course = courseRepository.save(Course.builder()
                    .instructorId(provider.getId())
                    .title("Cloud Resiliency Architecture")
                    .description("Distributed transaction patterns")
                    .category("Systems")
                    .difficulty("advanced")
                    .state("published")
                    .build());

            courseEnrollmentRepository.save(CourseEnrollment.builder()
                    .courseId(course.getId())
                    .userId(customer.getId())
                    .enrollmentState("completed")
                    .build());

            return course.getId();
        });

        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger issuedCount = new AtomicInteger(0);
        AtomicInteger idempotentCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    // Each thread attempts to claim certificate; idempotency check and unique constraint prevent duplicate insertion
                    Boolean success = tx.execute(status -> {
                        boolean alreadyClaimed = courseCertificateRepository.findByCourseIdAndUserId(courseId, customer.getId()).isPresent();
                        if (alreadyClaimed) {
                            return false;
                        }
                        courseCertificateRepository.saveAndFlush(CourseCertificate.builder()
                                .courseId(courseId)
                                .userId(customer.getId())
                                .recipientName("Divya Sharma")
                                .certificateNumber("CERT-CONCURRENT-" + UUID.randomUUID().toString().substring(0, 8))
                                .verificationHash("hash-" + index)
                                .build());
                        return true;
                    });
                    if (Boolean.TRUE.equals(success)) {
                        issuedCount.incrementAndGet();
                    } else {
                        idempotentCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    idempotentCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(courseCertificateRepository.findByCourseIdAndUserId(courseId, customer.getId())).isPresent();
        assertThat(issuedCount.get()).isEqualTo(1);
        assertThat(idempotentCount.get()).isEqualTo(numThreads - 1);
    }

    @Test
    @DisplayName("P3 Concurrency: Real PostgreSQL atomic booking status update under concurrent transactions")
    void testConcurrentServiceBookingStatusUpdateOnPostgreSql() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        UUID bookingId = tx.execute(status -> {
            ServiceListing listing = serviceListingRepository.save(ServiceListing.builder()
                    .providerId(provider.getId())
                    .title("Kubernetes Cluster Hardening")
                    .category("devops")
                    .startingPrice(new BigDecimal("9999.00"))
                    .priceCents(999900L)
                    .currency("INR")
                    .state("active")
                    .build());

            ServiceBooking booking = serviceBookingRepository.save(ServiceBooking.builder()
                    .serviceId(listing.getId())
                    .customerId(customer.getId())
                    .providerId(provider.getId())
                    .orderId("CL-ORD-CONC-1")
                    .title(listing.getTitle())
                    .price(listing.getStartingPrice())
                    .amountCents(listing.getPriceCents())
                    .currency("INR")
                    .status("requested")
                    .build());

            return booking.getId();
        });

        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    Integer rows = tx.execute(status ->
                            serviceBookingRepository.updateStatusAtomically(bookingId, "requested", "confirmed", Instant.now()));
                    if (rows != null && rows == 1) {
                        confirmedCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(confirmedCount.get())
                .as("Only one concurrent thread can transition requested -> confirmed atomically")
                .isEqualTo(1);
        assertThat(rejectedCount.get()).isEqualTo(numThreads - 1);

        ServiceBooking finalBooking = serviceBookingRepository.findById(bookingId).orElseThrow();
        assertThat(finalBooking.getStatus()).isEqualTo("confirmed");
    }

    @Test
    @DisplayName("P3 Concurrency: Real PostgreSQL payout balance reservation under concurrent transactions")
    void testConcurrentPayoutBalanceReservationOnPostgreSql() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Credit provider account with exactly 10,000 cents
        tx.execute(status -> {
            financialLedgerRepository.save(FinancialLedgerEntry.builder()
                    .userId(provider.getId())
                    .entryType("PAYMENT_CREDIT")
                    .amountCents(10000L)
                    .currency("INR")
                    .balanceAfterCents(10000L)
                    .description("Initial earnings for concurrency test")
                    .build());
            return null;
        });

        long initialBalance = financialLedgerRepository.calculateUserBalanceCents(provider.getId());
        assertThat(initialBalance).isEqualTo(10000L);

        // 5 concurrent threads each attempt to request 5,000 cents payout
        // Total requested = 25,000 cents; total available = 10,000 cents
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    CreatePayoutRequest request = CreatePayoutRequest.builder()
                            .amountCents(5000L)
                            .currency("INR")
                            .destinationReference("ICIC0001-CONC")
                            .build();

                    PayoutRequestDto dto = paymentService.requestPayout(request, provider.getId());
                    if (dto != null && "completed".equalsIgnoreCase(dto.getStatus())) {
                        successCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } catch (ResponseStatusException e) {
                    if (e.getStatusCode().is4xxClientError()) {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly 2 requests succeed (2 * 5,000 = 10,000 cents) and 3 are rejected with 400 Insufficient balance
        assertThat(successCount.get())
                .as("Exactly 2 payout requests of 5000 cents can succeed with 10000 cents balance")
                .isEqualTo(2);
        assertThat(rejectedCount.get())
                .as("Remaining 3 concurrent requests must be rejected to prevent overdraft")
                .isEqualTo(3);

        long finalBalance = financialLedgerRepository.calculateUserBalanceCents(provider.getId());
        assertThat(finalBalance).isEqualTo(0L);
    }

    @Test
    @DisplayName("P3 Concurrency: Real PostgreSQL service appointment slot double-booking rejection")
    void testConcurrentServiceSlotBookingOnPostgreSql() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        UUID serviceId = tx.execute(status -> {
            ServiceListing listing = serviceListingRepository.save(ServiceListing.builder()
                    .providerId(provider.getId())
                    .title("High-Performance Database Consulting")
                    .startingPrice(new BigDecimal("25000.00"))
                    .priceCents(2500000L)
                    .currency("INR")
                    .state("active")
                    .build());
            return listing.getId();
        });

        Instant slotTime = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger bookedCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    CreateServiceBookingRequest request = CreateServiceBookingRequest.builder()
                            .startsAt(slotTime)
                            .notes("Slot reservation concurrency test")
                            .build();

                    ServiceBookingDto dto = serviceManagementService.bookService(serviceId, request, customer.getId());
                    if (dto != null) {
                        bookedCount.incrementAndGet();
                    }
                } catch (ResponseStatusException e) {
                    if (e.getStatusCode().value() == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly 1 thread successfully books the slot; 4 threads encounter 409 Conflict
        assertThat(bookedCount.get())
                .as("Only one concurrent booking can claim the appointment time slot")
                .isEqualTo(1);
        assertThat(conflictCount.get())
                .as("Competing booking attempts must be rejected with 409 Conflict")
                .isEqualTo(numThreads - 1);

        long bookingsInDb = serviceBookingRepository.findAll().stream()
                .filter(b -> b.getServiceId().equals(serviceId) && slotTime.equals(b.getStartsAt()))
                .count();
        assertThat(bookingsInDb).isEqualTo(1L);
    }

    @Test
    @DisplayName("P3 Security: Authoritative pricing and payment confirmation security in PostgreSQL")
    void testPaymentSecurityNegativeCasesOnPostgreSql() {
        // 1. Create service listing with authoritative price 15,000 INR (1,500,000 cents)
        ServiceListing listing = serviceListingRepository.save(ServiceListing.builder()
                .providerId(provider.getId())
                .title("Security Vulnerability Assessment")
                .startingPrice(new BigDecimal("15000.00"))
                .priceCents(1500000L)
                .currency("INR")
                .state("active")
                .build());

        // 2. Client attempts price manipulation: price=1.00, amountCents=100
        CreateServiceBookingRequest bookingRequest = CreateServiceBookingRequest.builder()
                .price(new BigDecimal("1.00"))
                .amountCents(100L)
                .currency("INR")
                .notes("Testing price tampering")
                .build();

        ServiceBookingDto bookingDto = serviceManagementService.bookService(listing.getId(), bookingRequest, customer.getId());

        // Verified: Server ignores client price and persists authoritative listing price
        assertThat(bookingDto.getAmountCents()).isEqualTo(1500000L);
        assertThat(bookingDto.getPrice()).isEqualByComparingTo(new BigDecimal("15000.00"));

        // 3. Create payment order for booking; client again tries to pass amountCents=100
        CreatePaymentOrderRequest orderRequest = CreatePaymentOrderRequest.builder()
                .bookingId(bookingDto.getId())
                .amountCents(100L)
                .currency("INR")
                .build();

        PaymentOrderDto orderDto = paymentService.createOrder(orderRequest, customer.getId());
        assertThat(orderDto.getAmountCents()).isEqualTo(1500000L);

        // 4. Confirmation with missing payment ID throws 400
        ConfirmPaymentOrderRequest missingPaymentRequest = ConfirmPaymentOrderRequest.builder()
                .gatewayPaymentId("")
                .build();

        assertThatThrownBy(() -> paymentService.confirmOrder(orderDto.getId(), missingPaymentRequest, customer.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Missing required gateway payment identifier");

        // 5. Confirmation with arbitrary unknown payment ID throws 400
        ConfirmPaymentOrderRequest fakePaymentRequest = ConfirmPaymentOrderRequest.builder()
                .gatewayPaymentId("pay_arbitrary_unregistered_123")
                .build();

        assertThatThrownBy(() -> paymentService.confirmOrder(orderDto.getId(), fakePaymentRequest, customer.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Payment verification failed with provider");

        // 6. Valid sandbox payment confirmation
        String validSandboxPaymentId = "sandbox_pay_" + orderDto.getGatewayOrderId().substring("sandbox_ord_".length());
        ConfirmPaymentOrderRequest validRequest = ConfirmPaymentOrderRequest.builder()
                .gatewayPaymentId(validSandboxPaymentId)
                .build();

        PaymentOrderDto confirmed = paymentService.confirmOrder(orderDto.getId(), validRequest, customer.getId());
        assertThat(confirmed.getStatus()).isEqualTo("paid");
        assertThat(confirmed.getGatewayPaymentId()).isEqualTo(validSandboxPaymentId);

        // 7. Repeated confirmation with same payment ID is idempotent
        PaymentOrderDto idempotentConfirmed = paymentService.confirmOrder(orderDto.getId(), validRequest, customer.getId());
        assertThat(idempotentConfirmed.getStatus()).isEqualTo("paid");
    }
}
