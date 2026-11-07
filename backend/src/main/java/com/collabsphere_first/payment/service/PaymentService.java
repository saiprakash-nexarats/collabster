package com.collabsphere_first.payment.service;

import com.collabsphere_first.payment.dto.*;

import java.util.List;
import java.util.UUID;

public interface PaymentService {
    PaymentOrderDto createOrder(CreatePaymentOrderRequest request, Long userId);
    PaymentOrderDto confirmOrder(UUID orderId, ConfirmPaymentOrderRequest request, Long userId);
    WebhookResponse processWebhook(String rawPayload, String signature);
    List<PaymentOrderDto> listUserOrders(Long userId);
    PaymentReconciliationDto reconcileOrder(UUID orderId, Long userId);
    PayoutRequestDto requestPayout(CreatePayoutRequest request, Long providerId);
    List<PayoutRequestDto> listUserPayouts(Long providerId);
}
