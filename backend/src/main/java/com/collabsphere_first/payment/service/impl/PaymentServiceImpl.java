package com.collabsphere_first.payment.service.impl;

import com.collabsphere_first.payment.dto.*;
import com.collabsphere_first.payment.entity.*;
import com.collabsphere_first.payment.gateway.PaymentGatewayProvider;
import com.collabsphere_first.payment.repository.*;
import com.collabsphere_first.payment.service.PaymentService;
import com.collabsphere_first.services.entity.ServiceBooking;
import com.collabsphere_first.services.repository.ServiceBookingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final FinancialLedgerRepository financialLedgerRepository;
    private final ServiceBookingRepository serviceBookingRepository;
    private final PaymentGatewayProvider gatewayProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentOrderDto createOrder(CreatePaymentOrderRequest request, Long userId) {
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero");
        }

        // Idempotency key protection
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var existing = paymentOrderRepository.findByIdempotencyKey(request.getIdempotencyKey().trim());
            if (existing.isPresent()) {
                log.info("Returning existing order for idempotency key {}", request.getIdempotencyKey());
                return mapToOrderDto(existing.get());
            }
        }

        long authoritativeAmount = request.getAmountCents();
        String currency = request.getCurrency() != null ? request.getCurrency().toUpperCase(Locale.ROOT) : "INR";

        // If booking is attached, enforce server-calculated booking price
        if (request.getBookingId() != null) {
            ServiceBooking booking = serviceBookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service booking not found: " + request.getBookingId()));

            if (!booking.getCustomerId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to create payments for this booking");
            }

            if (booking.getAmountCents() > 0) {
                authoritativeAmount = booking.getAmountCents();
            }
            if (booking.getCurrency() != null) {
                currency = booking.getCurrency().toUpperCase(Locale.ROOT);
            }
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        String description = request.getDescription() != null ? request.getDescription() : "Collobster Marketplace Payment";

        // Provider checkout order creation
        var gatewayResult = gatewayProvider.createOrder(authoritativeAmount, currency, orderNumber, description);

        PaymentOrder order = PaymentOrder.builder()
                .userId(userId)
                .orderNumber(orderNumber)
                .idempotencyKey(request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank() ? request.getIdempotencyKey().trim() : null)
                .amountCents(authoritativeAmount)
                .currency(currency)
                .status("created")
                .description(description)
                .bookingId(request.getBookingId())
                .gatewayProvider(gatewayProvider.getProviderName())
                .gatewayOrderId(gatewayResult.gatewayOrderId())
                .build();

        PaymentOrder saved = paymentOrderRepository.save(order);

        if (request.getBookingId() != null) {
            serviceBookingRepository.findById(request.getBookingId()).ifPresent(b -> {
                b.setPaymentOrderId(saved.getId());
                serviceBookingRepository.save(b);
            });
        }

        log.info("Created payment order {} (id {}) for user {} for amount {} {}", orderNumber, saved.getId(), userId, authoritativeAmount, currency);
        return mapToOrderDto(saved);
    }

    @Override
    @Transactional
    public PaymentOrderDto confirmOrder(UUID orderId, ConfirmPaymentOrderRequest request, Long userId) {
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to confirm this payment order");
        }

        if ("paid".equalsIgnoreCase(order.getStatus())) {
            log.info("Payment order {} is already paid (idempotent confirmation)", orderId);
            return mapToOrderDto(order);
        }

        if ("cancelled".equalsIgnoreCase(order.getStatus()) || "failed".equalsIgnoreCase(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment order is in terminal state '" + order.getStatus() + "' and cannot be paid");
        }

        String paymentId = request.getGatewayPaymentId();
        if (paymentId == null || paymentId.isBlank()) {
            paymentId = "sandbox_pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        // Authoritative verification with gateway provider
        var verification = gatewayProvider.verifyPayment(paymentId, order.getGatewayOrderId());
        if (!verification.verified()) {
            order.setStatus("failed");
            paymentOrderRepository.save(order);

            paymentTransactionRepository.save(PaymentTransaction.builder()
                    .orderId(orderId)
                    .transactionType("PAYMENT")
                    .amountCents(order.getAmountCents())
                    .currency(order.getCurrency())
                    .gatewayProvider(gatewayProvider.getProviderName())
                    .gatewayTransactionId(paymentId)
                    .status("FAILED")
                    .rawResponse("Verification failed: " + verification.status())
                    .build());

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed with provider");
        }

        order.setStatus("paid");
        order.setGatewayPaymentId(paymentId);
        order.setPaidAt(Instant.now());
        PaymentOrder updated = paymentOrderRepository.save(order);

        paymentTransactionRepository.save(PaymentTransaction.builder()
                .orderId(orderId)
                .transactionType("PAYMENT")
                .amountCents(order.getAmountCents())
                .currency(order.getCurrency())
                .gatewayProvider(gatewayProvider.getProviderName())
                .gatewayTransactionId(paymentId)
                .status("SUCCESS")
                .rawResponse("Verified and paid")
                .build());

        // Update linked service booking status
        if (order.getBookingId() != null) {
            serviceBookingRepository.findById(order.getBookingId()).ifPresent(b -> {
                b.setStatus("confirmed");
                b.setUpdatedAt(Instant.now());
                serviceBookingRepository.save(b);

                // Credit the provider's escrow ledger
                long currentProviderBalance = financialLedgerRepository.calculateUserBalanceCents(b.getProviderId());
                financialLedgerRepository.save(FinancialLedgerEntry.builder()
                        .userId(b.getProviderId())
                        .entryType("PAYMENT_CREDIT")
                        .amountCents(order.getAmountCents())
                        .currency(order.getCurrency())
                        .balanceAfterCents(currentProviderBalance + order.getAmountCents())
                        .referenceType("PAYMENT_ORDER")
                        .referenceId(order.getId())
                        .description("Payment credit for service booking " + b.getOrderId())
                        .build());
            });
        }

        log.info("Payment order {} confirmed as PAID via {}", orderId, paymentId);
        return mapToOrderDto(updated);
    }

    @Override
    @Transactional
    public WebhookResponse processWebhook(String rawPayload, String signature) {
        if (!gatewayProvider.verifyWebhookSignature(rawPayload, signature)) {
            log.warn("Invalid webhook signature rejected");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }

        String eventId = null;
        String eventType = "payment.updated";
        String gatewayOrderId = null;
        String gatewayPaymentId = null;

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            eventId = root.path("id").asText(null);
            eventType = root.path("event").asText(root.path("eventType").asText("payment.updated"));
            JsonNode payloadObj = root.path("payload").path("payment").path("entity");
            if (payloadObj.isMissingNode()) {
                payloadObj = root;
            }
            gatewayOrderId = payloadObj.path("order_id").asText(null);
            gatewayPaymentId = payloadObj.path("id").asText(null);
        } catch (Exception e) {
            log.error("Failed to parse webhook JSON payload: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed webhook JSON payload");
        }

        if (eventId == null || eventId.isBlank()) {
            eventId = "evt_" + UUID.nameUUIDFromBytes(rawPayload.getBytes()).toString().replace("-", "");
        }

        // Webhook Deduplication / Replay Prevention
        if (paymentWebhookEventRepository.existsByEventId(eventId)) {
            log.info("Webhook event {} already processed (replay rejected)", eventId);
            return WebhookResponse.builder()
                    .status("ok")
                    .eventId(eventId)
                    .processed(true)
                    .build();
        }

        PaymentWebhookEvent webhookEvent = PaymentWebhookEvent.builder()
                .eventId(eventId)
                .provider(gatewayProvider.getProviderName())
                .eventType(eventType)
                .payload(rawPayload)
                .signature(signature)
                .processed(false)
                .build();
        paymentWebhookEventRepository.save(webhookEvent);

        // Process payment capture if matching order exists
        if (gatewayOrderId != null) {
            Optional<PaymentOrder> orderOpt = paymentOrderRepository.findByGatewayOrderId(gatewayOrderId);
            if (orderOpt.isPresent()) {
                PaymentOrder order = orderOpt.get();
                if (!"paid".equalsIgnoreCase(order.getStatus())) {
                    order.setStatus("paid");
                    order.setGatewayPaymentId(gatewayPaymentId);
                    order.setPaidAt(Instant.now());
                    paymentOrderRepository.save(order);
                    log.info("Webhook {} updated order {} to PAID", eventId, order.getId());
                }
            }
        }

        webhookEvent.setProcessed(true);
        webhookEvent.setProcessedAt(Instant.now());
        paymentWebhookEventRepository.save(webhookEvent);

        return WebhookResponse.builder()
                .status("ok")
                .eventId(eventId)
                .processed(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentOrderDto> listUserOrders(Long userId) {
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToOrderDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentReconciliationDto reconcileOrder(UUID orderId, Long userId) {
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view reconciliation for this order");
        }

        var gatewayReconciliation = gatewayProvider.reconcile(order.getGatewayOrderId());
        boolean match = order.getStatus().equalsIgnoreCase(gatewayReconciliation.status());

        return PaymentReconciliationDto.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getStatus())
                .gatewayStatus(gatewayReconciliation.status())
                .amountCents(order.getAmountCents())
                .currency(order.getCurrency())
                .reconciled(true)
                .match(match)
                .reconciledAt(Instant.now())
                .build();
    }

    @Override
    @Transactional
    public PayoutRequestDto requestPayout(CreatePayoutRequest request, Long providerId) {
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payout amount must be greater than zero");
        }

        if (request.getDestinationReference() == null || request.getDestinationReference().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination account reference is required");
        }

        long availableBalance = financialLedgerRepository.calculateUserBalanceCents(providerId);
        if (request.getAmountCents() > availableBalance) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient eligible balance for payout. Available: " + availableBalance + " cents, Requested: " + request.getAmountCents() + " cents");
        }

        String currency = request.getCurrency() != null ? request.getCurrency().toUpperCase(Locale.ROOT) : "INR";
        var gatewayResult = gatewayProvider.processPayout(request.getAmountCents(), currency, request.getDestinationReference().trim());

        PayoutRequest payout = PayoutRequest.builder()
                .providerId(providerId)
                .amountCents(request.getAmountCents())
                .currency(currency)
                .method(request.getMethod() != null ? request.getMethod() : "bank_account")
                .destinationReference(request.getDestinationReference().trim())
                .status("completed")
                .gatewayPayoutId(gatewayResult.gatewayPayoutId())
                .notes(request.getNotes())
                .processedAt(Instant.now())
                .build();

        PayoutRequest saved = payoutRequestRepository.save(payout);

        // Deduct from ledger
        financialLedgerRepository.save(FinancialLedgerEntry.builder()
                .userId(providerId)
                .entryType("PAYOUT_DEBIT")
                .amountCents(request.getAmountCents())
                .currency(currency)
                .balanceAfterCents(availableBalance - request.getAmountCents())
                .referenceType("PAYOUT_REQUEST")
                .referenceId(saved.getId())
                .description("Payout to " + request.getDestinationReference())
                .build());

        log.info("Completed payout request {} for provider {} for amount {} {}", saved.getId(), providerId, request.getAmountCents(), currency);
        return mapToPayoutDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutRequestDto> listUserPayouts(Long providerId) {
        return payoutRequestRepository.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
                .map(this::mapToPayoutDto)
                .collect(Collectors.toList());
    }

    private PaymentOrderDto mapToOrderDto(PaymentOrder o) {
        return PaymentOrderDto.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .orderNumber(o.getOrderNumber())
                .amountCents(o.getAmountCents())
                .currency(o.getCurrency())
                .status(o.getStatus())
                .description(o.getDescription())
                .bookingId(o.getBookingId())
                .gatewayProvider(o.getGatewayProvider())
                .gatewayOrderId(o.getGatewayOrderId())
                .gatewayPaymentId(o.getGatewayPaymentId())
                .paidAt(o.getPaidAt())
                .remote(true)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private PayoutRequestDto mapToPayoutDto(PayoutRequest p) {
        return PayoutRequestDto.builder()
                .id(p.getId())
                .providerId(p.getProviderId())
                .amountCents(p.getAmountCents())
                .currency(p.getCurrency())
                .method(p.getMethod())
                .destinationReference(p.getDestinationReference())
                .status(p.getStatus())
                .gatewayPayoutId(p.getGatewayPayoutId())
                .notes(p.getNotes())
                .processedAt(p.getProcessedAt())
                .remote(true)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
