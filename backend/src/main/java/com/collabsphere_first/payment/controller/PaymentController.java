package com.collabsphere_first.payment.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.payment.dto.*;
import com.collabsphere_first.payment.service.PaymentService;
import com.collabsphere_first.security.service.CurrentUserService;
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
@RequestMapping({"/v1/payments", "/api/v1/payments"})
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<PaymentOrderDto>> createOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        PaymentOrderDto order = paymentService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(order));
    }

    @PostMapping("/orders/{id}/confirm")
    public ResponseEntity<ApiResponse<PaymentOrderDto>> confirmOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) ConfirmPaymentOrderRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        ConfirmPaymentOrderRequest req = request != null ? request : new ConfirmPaymentOrderRequest();
        PaymentOrderDto order = paymentService.confirmOrder(id, req, userId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/webhook")
    public ResponseEntity<WebhookResponse> webhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        log.info("Received payment webhook with signature: {}", signature);
        WebhookResponse response = paymentService.processWebhook(rawPayload, signature);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<PaymentOrderDto>>> listOrders() {
        Long userId = currentUserService.getCurrentUser().getId();
        List<PaymentOrderDto> orders = paymentService.listUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/orders/{id}/reconciliation")
    public ResponseEntity<ApiResponse<PaymentReconciliationDto>> reconcileOrder(
            @PathVariable UUID id) {
        Long userId = currentUserService.getCurrentUser().getId();
        PaymentReconciliationDto reconciliation = paymentService.reconcileOrder(id, userId);
        return ResponseEntity.ok(ApiResponse.success(reconciliation));
    }

    @PostMapping("/payouts")
    public ResponseEntity<ApiResponse<PayoutRequestDto>> requestPayout(
            @Valid @RequestBody CreatePayoutRequest request) {
        Long providerId = currentUserService.getCurrentUser().getId();
        PayoutRequestDto payout = paymentService.requestPayout(request, providerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(payout));
    }

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<List<PayoutRequestDto>>> listPayouts() {
        Long providerId = currentUserService.getCurrentUser().getId();
        List<PayoutRequestDto> payouts = paymentService.listUserPayouts(providerId);
        return ResponseEntity.ok(ApiResponse.success(payouts));
    }
}
