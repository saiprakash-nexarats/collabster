package com.collabsphere_first.payment.service.impl;

import com.collabsphere_first.payment.dto.*;
import com.collabsphere_first.payment.entity.*;
import com.collabsphere_first.payment.gateway.PaymentGatewayProvider;
import com.collabsphere_first.payment.repository.*;
import com.collabsphere_first.payment.service.PaymentService;
import com.collabsphere_first.services.entity.ServiceBooking;
import com.collabsphere_first.services.repository.ServiceBookingRepository;
import com.collabsphere_first.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("INR", "USD", "EUR", "GBP");

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final FinancialLedgerRepository financialLedgerRepository;
    private final ServiceBookingRepository serviceBookingRepository;
    private final UserRepository userRepository;
    private final PaymentGatewayProvider gatewayProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentOrderDto createOrder(CreatePaymentOrderRequest request, Long userId) {
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero");
        }

        String currency = request.getCurrency() != null ? request.getCurrency().toUpperCase(Locale.ROOT) : "INR";
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment currency: " + currency);
        }

        long authoritativeAmount = request.getAmountCents();

        // If booking is attached, enforce server-calculated booking price
        if (request.getBookingId() != null) {
            ServiceBooking booking = serviceBookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service booking not found: " + request.getBookingId()));

            if (!booking.getCustomerId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to create payments for this booking");
            }

            String bookingStatus = booking.getStatus() != null ? booking.getStatus().toLowerCase(Locale.ROOT) : "requested";
            if (List.of("cancelled", "rejected", "expired").contains(bookingStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create payment for booking in state '" + booking.getStatus() + "'");
            }
            if ("completed".equalsIgnoreCase(bookingStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is already completed");
            }

            if (booking.getAmountCents() > 0) {
                authoritativeAmount = booking.getAmountCents();
            }
            if (booking.getCurrency() != null) {
                currency = booking.getCurrency().toUpperCase(Locale.ROOT);
            }
        }

        // Idempotency key protection scoped to user and payload
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            String key = request.getIdempotencyKey().trim();
            Optional<PaymentOrder> existing = paymentOrderRepository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                PaymentOrder existingOrder = existing.get();
                if (!existingOrder.getUserId().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key collision across user boundary");
                }

                boolean amountMatches = existingOrder.getAmountCents().equals(authoritativeAmount);
                boolean currencyMatches = existingOrder.getCurrency().equalsIgnoreCase(currency);
                boolean bookingMatches = (existingOrder.getBookingId() == null && request.getBookingId() == null)
                        || (existingOrder.getBookingId() != null && existingOrder.getBookingId().equals(request.getBookingId()));

                if (!amountMatches || !currencyMatches || !bookingMatches) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key reused with mismatched request parameters");
                }

                log.info("Returning existing order {} for idempotency key {}", existingOrder.getOrderNumber(), key);
                return mapToOrderDto(existingOrder);
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

        PaymentOrder savedOrder = paymentOrderRepository.save(order);
        final PaymentOrder saved = savedOrder != null ? savedOrder : order;

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

        String paymentId = request != null ? request.getGatewayPaymentId() : null;
        if (paymentId == null || paymentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required gateway payment identifier");
        }

        // Idempotency check: If already paid with this payment id, return immediately
        if ("paid".equalsIgnoreCase(order.getStatus())) {
            if (paymentId.trim().equals(order.getGatewayPaymentId()) || order.getGatewayPaymentId() == null) {
                log.info("Payment order {} is already paid with reference {} (idempotent confirmation)", orderId, paymentId);
                return mapToOrderDto(order);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment order is already paid with a different payment reference");
        }

        // Authoritative verification with gateway provider
        var verification = gatewayProvider.verifyPayment(paymentId.trim(), order.getGatewayOrderId());
        if (!verification.verified()) {
            order.setStatus("failed");
            paymentOrderRepository.save(order);

            paymentTransactionRepository.save(PaymentTransaction.builder()
                    .orderId(orderId)
                    .transactionType("PAYMENT")
                    .amountCents(order.getAmountCents())
                    .currency(order.getCurrency())
                    .gatewayProvider(gatewayProvider.getProviderName())
                    .gatewayTransactionId(paymentId.trim())
                    .status("FAILED")
                    .rawResponse("Verification failed: " + verification.status())
                    .build());

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed with provider: " + verification.status());
        }

        // Shared settlement engine handles order transition, duplicate payment-ID check, and ledger posting
        return settlePaymentOrder(order, paymentId.trim(), verification.amountCents(), verification.currency(), "Client Checkout Confirmation");
    }

    @Transactional
    public PaymentOrderDto settlePaymentOrder(
            PaymentOrder order,
            String gatewayPaymentId,
            long verifiedAmountCents,
            String verifiedCurrency,
            String settlementSource) {

        // 1. Idempotency check: Already paid with same gateway payment ID
        if ("paid".equalsIgnoreCase(order.getStatus())) {
            if (gatewayPaymentId != null && gatewayPaymentId.equals(order.getGatewayPaymentId())) {
                log.info("Payment order {} is already paid with reference {} (idempotent settlement)", order.getId(), gatewayPaymentId);
                return mapToOrderDto(order);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment order is already paid with a different payment reference");
        }

        // 2. Reject terminal invalid states
        if ("cancelled".equalsIgnoreCase(order.getStatus()) || "refunded".equalsIgnoreCase(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment order is in terminal state '" + order.getStatus() + "' and cannot be settled");
        }

        // 3. Amount and currency comparison
        if (verifiedAmountCents != order.getAmountCents()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verified payment amount (" + verifiedAmountCents + " cents) does not match order amount (" + order.getAmountCents() + " cents)");
        }
        if (!verifiedCurrency.equalsIgnoreCase(order.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verified payment currency (" + verifiedCurrency + ") does not match order currency (" + order.getCurrency() + ")");
        }

        // 4. Unique gateway payment reference check
        if (gatewayPaymentId != null && !gatewayPaymentId.isBlank()) {
            Optional<PaymentOrder> existingWithPayment = paymentOrderRepository.findByGatewayPaymentIdAndStatus(gatewayPaymentId, "paid");
            if (existingWithPayment.isPresent() && !existingWithPayment.get().getId().equals(order.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Gateway payment identifier " + gatewayPaymentId + " has already been applied to order " + existingWithPayment.get().getOrderNumber());
            }
        }

        // 5. Update order state
        order.setStatus("paid");
        order.setGatewayPaymentId(gatewayPaymentId);
        order.setPaidAt(Instant.now());
        PaymentOrder savedOrder = paymentOrderRepository.save(order);
        final PaymentOrder saved = savedOrder != null ? savedOrder : order;

        // 6. Record success transaction
        paymentTransactionRepository.save(PaymentTransaction.builder()
                .orderId(order.getId())
                .transactionType("PAYMENT")
                .amountCents(order.getAmountCents())
                .currency(order.getCurrency())
                .gatewayProvider(order.getGatewayProvider() != null ? order.getGatewayProvider() : gatewayProvider.getProviderName())
                .gatewayTransactionId(gatewayPaymentId)
                .status("SUCCESS")
                .rawResponse(settlementSource)
                .build());

        // 7. Settle linked booking & credit provider escrow ledger
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

        log.info("Payment order {} confirmed as PAID via {} ({})", order.getId(), gatewayPaymentId, settlementSource);
        return mapToOrderDto(saved);
    }

    @Override
    @Transactional
    public WebhookResponse processWebhook(String rawPayload, String signature) {
        // Raw-body signature verification (do NOT log sensitive signature)
        if (!gatewayProvider.verifyWebhookSignature(rawPayload, signature)) {
            log.warn("Invalid payment webhook signature rejected");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }

        String eventId;
        String eventType;
        String gatewayOrderId;
        String gatewayPaymentId;
        Long payloadAmountCents = null;
        String payloadCurrency = null;

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            eventId = root.path("id").asText(null);
            eventType = root.path("event").asText(root.path("eventType").asText("payment.captured"));

            JsonNode payloadObj = root.path("payload").path("payment").path("entity");
            if (payloadObj.isMissingNode()) {
                payloadObj = root;
            }
            gatewayOrderId = payloadObj.path("order_id").asText(null);
            gatewayPaymentId = payloadObj.path("id").asText(null);

            if (payloadObj.has("amount")) {
                payloadAmountCents = payloadObj.path("amount").asLong();
            } else if (payloadObj.has("amount_cents")) {
                payloadAmountCents = payloadObj.path("amount_cents").asLong();
            }

            if (payloadObj.has("currency")) {
                payloadCurrency = payloadObj.path("currency").asText("INR");
            }
        } catch (Exception e) {
            log.error("Failed to parse webhook JSON payload");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed webhook JSON payload");
        }

        if (eventId == null || eventId.isBlank()) {
            eventId = "evt_" + UUID.nameUUIDFromBytes(rawPayload.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
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
                .signature(null) // Omit raw signature from storage and logs
                .processed(false)
                .build();
        paymentWebhookEventRepository.save(webhookEvent);

        // Explicit supported event types
        List<String> supportedCaptureEvents = List.of("payment.captured", "payment.authorized", "order.paid");
        if (!supportedCaptureEvents.contains(eventType.toLowerCase(Locale.ROOT))) {
            log.info("Ignoring non-capture webhook event type: {}", eventType);
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessedAt(Instant.now());
            paymentWebhookEventRepository.save(webhookEvent);
            return WebhookResponse.builder()
                    .status("ignored")
                    .eventId(eventId)
                    .processed(true)
                    .build();
        }

        if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
            log.warn("Webhook event {} has no order_id", eventId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook payload missing order_id");
        }

        PaymentOrder order = paymentOrderRepository.findByGatewayOrderId(gatewayOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No payment order found for gateway order: " + gatewayOrderId));

        long amountToVerify = payloadAmountCents != null ? payloadAmountCents : order.getAmountCents();
        String currencyToVerify = payloadCurrency != null ? payloadCurrency : order.getCurrency();

        // Process through shared payment-settlement engine
        settlePaymentOrder(order, gatewayPaymentId, amountToVerify, currencyToVerify, "Webhook Event " + eventId);

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

        String currency = request.getCurrency() != null ? request.getCurrency().toUpperCase(Locale.ROOT) : "INR";
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payout currency: " + currency);
        }

        // PESSIMISTIC LOCK: Lock provider row in PostgreSQL to serialize concurrent payout requests
        userRepository.findByIdForUpdate(providerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider user not found: " + providerId));

        // Idempotency check under lock
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            String key = request.getIdempotencyKey().trim();
            Optional<PayoutRequest> existingOpt = payoutRequestRepository.findByIdempotencyKey(key);
            if (existingOpt.isPresent()) {
                PayoutRequest existing = existingOpt.get();
                if (!existing.getProviderId().equals(providerId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key collision across user boundary");
                }
                if (!existing.getAmountCents().equals(request.getAmountCents())
                        || !existing.getDestinationReference().equalsIgnoreCase(request.getDestinationReference().trim())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key reused with different payout parameters");
                }
                return mapToPayoutDto(existing);
            }
        }

        long settledBalance = financialLedgerRepository.calculateUserBalanceCents(providerId);
        long pendingReservations = payoutRequestRepository.calculatePendingReservations(providerId);
        long availableBalance = settledBalance - pendingReservations;

        if (request.getAmountCents() > availableBalance) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient eligible balance for payout. Available: " + availableBalance + " cents, Requested: " + request.getAmountCents() + " cents");
        }

        // Create PayoutRequest in PENDING status (reserves funds)
        PayoutRequest payout = PayoutRequest.builder()
                .providerId(providerId)
                .amountCents(request.getAmountCents())
                .currency(currency)
                .method(request.getMethod() != null ? request.getMethod() : "bank_account")
                .destinationReference(request.getDestinationReference().trim())
                .status("pending")
                .idempotencyKey(request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank() ? request.getIdempotencyKey().trim() : null)
                .notes(request.getNotes())
                .build();
        PayoutRequest savedPayout = payoutRequestRepository.save(payout);
        PayoutRequest saved = savedPayout != null ? savedPayout : payout;

        // Process with gateway
        var gatewayResult = gatewayProvider.processPayout(request.getAmountCents(), currency, request.getDestinationReference().trim());

        if (gatewayResult.success() && "completed".equalsIgnoreCase(gatewayResult.status())) {
            saved.setStatus("completed");
            saved.setGatewayPayoutId(gatewayResult.gatewayPayoutId());
            saved.setProcessedAt(Instant.now());
            payoutRequestRepository.save(saved);

            // Settle debit in ledger
            financialLedgerRepository.save(FinancialLedgerEntry.builder()
                    .userId(providerId)
                    .entryType("PAYOUT_DEBIT")
                    .amountCents(request.getAmountCents())
                    .currency(currency)
                    .balanceAfterCents(settledBalance - request.getAmountCents())
                    .referenceType("PAYOUT_REQUEST")
                    .referenceId(saved.getId())
                    .description("Payout withdrawal to " + request.getDestinationReference().trim())
                    .build());

            log.info("Completed payout request {} for provider {} for amount {} {}", saved.getId(), providerId, request.getAmountCents(), currency);
        } else if ("failed".equalsIgnoreCase(gatewayResult.status())) {
            saved.setStatus("failed");
            saved.setProcessedAt(Instant.now());
            payoutRequestRepository.save(saved);
            log.warn("Gateway rejected payout request {}: {}", saved.getId(), gatewayResult.status());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gateway rejected payout processing");
        } else {
            // Processing
            saved.setStatus("processing");
            saved.setGatewayPayoutId(gatewayResult.gatewayPayoutId());
            payoutRequestRepository.save(saved);
            log.info("Payout request {} is processing asynchronously", saved.getId());
        }

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
                .idempotencyKey(p.getIdempotencyKey())
                .notes(p.getNotes())
                .processedAt(p.getProcessedAt())
                .remote(true)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
