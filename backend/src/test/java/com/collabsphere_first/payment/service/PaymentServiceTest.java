package com.collabsphere_first.payment.service;

import com.collabsphere_first.payment.dto.*;
import com.collabsphere_first.payment.entity.*;
import com.collabsphere_first.payment.gateway.PaymentGatewayProvider;
import com.collabsphere_first.payment.repository.*;
import com.collabsphere_first.payment.service.impl.PaymentServiceImpl;
import com.collabsphere_first.services.entity.ServiceBooking;
import com.collabsphere_first.services.repository.ServiceBookingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentWebhookEventRepository paymentWebhookEventRepository;

    @Mock
    private PayoutRequestRepository payoutRequestRepository;

    @Mock
    private FinancialLedgerRepository financialLedgerRepository;

    @Mock
    private ServiceBookingRepository serviceBookingRepository;

    @Mock
    private PaymentGatewayProvider gatewayProvider;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Long userId;
    private Long providerId;
    private UUID orderId;
    private PaymentOrder order;

    @BeforeEach
    void setUp() {
        userId = 200L;
        providerId = 100L;
        orderId = UUID.randomUUID();

        order = PaymentOrder.builder()
                .id(orderId)
                .userId(userId)
                .orderNumber("ORD-1234567890AB")
                .amountCents(500000L)
                .currency("INR")
                .status("created")
                .description("Service Package Booking")
                .gatewayProvider("SandboxMock")
                .gatewayOrderId("sandbox_ord_test123")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createOrder: creates payment order via gateway and persists with authoritative amount")
    void createOrder_success() {
        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .amountCents(500000L)
                .currency("INR")
                .description("Development milestone")
                .build();

        when(gatewayProvider.getProviderName()).thenReturn("SandboxMock");
        when(gatewayProvider.createOrder(eq(500000L), eq("INR"), anyString(), anyString()))
                .thenReturn(new PaymentGatewayProvider.GatewayOrderResult("sandbox_ord_999", "created"));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> {
            PaymentOrder o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        PaymentOrderDto dto = paymentService.createOrder(request, userId);

        assertThat(dto).isNotNull();
        assertThat(dto.getAmountCents()).isEqualTo(500000L);
        assertThat(dto.getCurrency()).isEqualTo("INR");
        assertThat(dto.getStatus()).isEqualTo("created");
        assertThat(dto.getGatewayOrderId()).isEqualTo("sandbox_ord_999");
    }

    @Test
    @DisplayName("createOrder: returns existing order when idempotency key is matched")
    void createOrder_idempotent() {
        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .amountCents(500000L)
                .idempotencyKey("idem-key-abc")
                .build();

        when(paymentOrderRepository.findByIdempotencyKey("idem-key-abc")).thenReturn(Optional.of(order));

        PaymentOrderDto dto = paymentService.createOrder(request, userId);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(orderId);
        verify(gatewayProvider, never()).createOrder(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("createOrder: rejects non-positive amount with 400 Bad Request")
    void createOrder_invalidAmount_throwsBadRequest() {
        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .amountCents(0L)
                .build();

        assertThatThrownBy(() -> paymentService.createOrder(request, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("createOrder: enforces authoritative booking price when booking is attached")
    void createOrder_withBooking_enforcesBookingPrice() {
        UUID bookingId = UUID.randomUUID();
        ServiceBooking booking = ServiceBooking.builder()
                .id(bookingId)
                .customerId(userId)
                .providerId(providerId)
                .amountCents(750000L)
                .currency("INR")
                .build();

        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .bookingId(bookingId)
                .amountCents(100L) // Client attempts to supply lower amount
                .build();

        when(serviceBookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(gatewayProvider.getProviderName()).thenReturn("SandboxMock");
        when(gatewayProvider.createOrder(eq(750000L), eq("INR"), anyString(), anyString()))
                .thenReturn(new PaymentGatewayProvider.GatewayOrderResult("sandbox_ord_bk", "created"));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> {
            PaymentOrder o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        PaymentOrderDto dto = paymentService.createOrder(request, userId);

        assertThat(dto.getAmountCents()).isEqualTo(750000L); // Enforces server booking price
    }

    @Test
    @DisplayName("confirmOrder: verifies payment with gateway, marks paid, updates booking and adds ledger credit")
    void confirmOrder_success() {
        UUID bookingId = UUID.randomUUID();
        order.setBookingId(bookingId);

        ServiceBooking booking = ServiceBooking.builder()
                .id(bookingId)
                .customerId(userId)
                .providerId(providerId)
                .orderId("CL-ORD-123")
                .status("requested")
                .build();

        ConfirmPaymentOrderRequest request = ConfirmPaymentOrderRequest.builder()
                .gatewayPaymentId("pay_12345")
                .build();

        when(paymentOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(gatewayProvider.verifyPayment("pay_12345", "sandbox_ord_test123"))
                .thenReturn(new PaymentGatewayProvider.GatewayVerificationResult(true, "pay_12345", "paid", 500000L, "INR"));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(i -> i.getArgument(0));
        when(serviceBookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(financialLedgerRepository.calculateUserBalanceCents(providerId)).thenReturn(100000L);

        PaymentOrderDto dto = paymentService.confirmOrder(orderId, request, userId);

        assertThat(dto.getStatus()).isEqualTo("paid");
        assertThat(dto.getGatewayPaymentId()).isEqualTo("pay_12345");
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
        verify(financialLedgerRepository).save(any(FinancialLedgerEntry.class));
        assertThat(booking.getStatus()).isEqualTo("confirmed");
    }

    @Test
    @DisplayName("confirmOrder: returns already paid order idempotently without double-crediting")
    void confirmOrder_alreadyPaid_idempotent() {
        order.setStatus("paid");
        when(paymentOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ConfirmPaymentOrderRequest request = ConfirmPaymentOrderRequest.builder()
                .gatewayPaymentId("pay_12345")
                .build();

        PaymentOrderDto dto = paymentService.confirmOrder(orderId, request, userId);

        assertThat(dto.getStatus()).isEqualTo("paid");
        verify(financialLedgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmOrder: throws 403 Forbidden when confirming other user's order")
    void confirmOrder_unauthorizedUser_throwsForbidden() {
        when(paymentOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ConfirmPaymentOrderRequest request = ConfirmPaymentOrderRequest.builder().build();

        assertThatThrownBy(() -> paymentService.confirmOrder(orderId, request, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    @DisplayName("confirmOrder: throws 400 Bad Request when gateway verification fails")
    void confirmOrder_verificationFailed_throwsBadRequest() {
        when(paymentOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(gatewayProvider.verifyPayment("pay_fake", "sandbox_ord_test123"))
                .thenReturn(new PaymentGatewayProvider.GatewayVerificationResult(false, "pay_fake", "failed", 0L, "INR"));

        ConfirmPaymentOrderRequest request = ConfirmPaymentOrderRequest.builder()
                .gatewayPaymentId("pay_fake")
                .build();

        assertThatThrownBy(() -> paymentService.confirmOrder(orderId, request, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Payment verification failed");

        assertThat(order.getStatus()).isEqualTo("failed");
    }

    @Test
    @DisplayName("processWebhook: processes valid signature and updates order status")
    void processWebhook_success() {
        String payload = "{\"id\":\"evt_100\",\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_wb_1\",\"order_id\":\"sandbox_ord_test123\"}}}}";
        String signature = "sig_valid";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(paymentWebhookEventRepository.existsByEventId("evt_100")).thenReturn(false);
        when(gatewayProvider.getProviderName()).thenReturn("SandboxMock");
        when(paymentOrderRepository.findByGatewayOrderId("sandbox_ord_test123")).thenReturn(Optional.of(order));

        WebhookResponse response = paymentService.processWebhook(payload, signature);

        assertThat(response.getStatus()).isEqualTo("ok");
        assertThat(response.getEventId()).isEqualTo("evt_100");
        assertThat(response.getProcessed()).isTrue();
        assertThat(order.getStatus()).isEqualTo("paid");
    }

    @Test
    @DisplayName("processWebhook: rejects invalid signature with 401 Unauthorized")
    void processWebhook_invalidSignature_throwsUnauthorized() {
        String payload = "{\"id\":\"evt_100\"}";
        when(gatewayProvider.verifyWebhookSignature(payload, "bad_sig")).thenReturn(false);

        assertThatThrownBy(() -> paymentService.processWebhook(payload, "bad_sig"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid webhook signature");
    }

    @Test
    @DisplayName("processWebhook: duplicate event replay is deduplicated")
    void processWebhook_replayDeduplicated() {
        String payload = "{\"id\":\"evt_duplicate\"}";
        when(gatewayProvider.verifyWebhookSignature(payload, "sig")).thenReturn(true);
        when(paymentWebhookEventRepository.existsByEventId("evt_duplicate")).thenReturn(true);

        WebhookResponse response = paymentService.processWebhook(payload, "sig");

        assertThat(response.getProcessed()).isTrue();
        verify(paymentOrderRepository, never()).findByGatewayOrderId(anyString());
    }

    @Test
    @DisplayName("reconcileOrder: reconciles order status against gateway provider")
    void reconcileOrder_success() {
        order.setStatus("paid");
        when(paymentOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(gatewayProvider.reconcile("sandbox_ord_test123"))
                .thenReturn(new PaymentGatewayProvider.GatewayReconciliationResult(true, "paid", 500000L, "INR"));

        PaymentReconciliationDto dto = paymentService.reconcileOrder(orderId, userId);

        assertThat(dto.getReconciled()).isTrue();
        assertThat(dto.getMatch()).isTrue();
        assertThat(dto.getOrderStatus()).isEqualTo("paid");
        assertThat(dto.getGatewayStatus()).isEqualTo("paid");
    }

    @Test
    @DisplayName("requestPayout: processes valid payout request, records payout and deducts ledger")
    void requestPayout_success() {
        CreatePayoutRequest request = CreatePayoutRequest.builder()
                .amountCents(50000L)
                .currency("INR")
                .destinationReference("HDFC0001234-ACC987654")
                .notes("Weekly earnings")
                .build();

        when(financialLedgerRepository.calculateUserBalanceCents(providerId)).thenReturn(100000L);
        when(gatewayProvider.processPayout(eq(50000L), eq("INR"), anyString()))
                .thenReturn(new PaymentGatewayProvider.GatewayPayoutResult(true, "pout_sandbox_1", "completed"));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenAnswer(i -> {
            PayoutRequest p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PayoutRequestDto dto = paymentService.requestPayout(request, providerId);

        assertThat(dto).isNotNull();
        assertThat(dto.getAmountCents()).isEqualTo(50000L);
        assertThat(dto.getStatus()).isEqualTo("completed");
        verify(financialLedgerRepository).save(any(FinancialLedgerEntry.class));
    }

    @Test
    @DisplayName("requestPayout: rejects payout request exceeding available ledger balance")
    void requestPayout_insufficientBalance_throwsBadRequest() {
        CreatePayoutRequest request = CreatePayoutRequest.builder()
                .amountCents(200000L)
                .destinationReference("ACC123")
                .build();

        when(financialLedgerRepository.calculateUserBalanceCents(providerId)).thenReturn(50000L);

        assertThatThrownBy(() -> paymentService.requestPayout(request, providerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient eligible balance");
    }
}
