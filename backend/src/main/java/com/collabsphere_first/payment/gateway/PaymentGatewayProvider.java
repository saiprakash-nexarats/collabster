package com.collabsphere_first.payment.gateway;

public interface PaymentGatewayProvider {
    String getProviderName();
    GatewayOrderResult createOrder(long amountCents, String currency, String orderNumber, String description);
    GatewayVerificationResult verifyPayment(String gatewayPaymentId, String gatewayOrderId);
    boolean verifyWebhookSignature(String rawPayload, String signature);
    GatewayPayoutResult processPayout(long amountCents, String currency, String destinationReference);
    GatewayReconciliationResult reconcile(String gatewayOrderId);

    record GatewayOrderResult(String gatewayOrderId, String status) {}
    record GatewayVerificationResult(boolean verified, String gatewayPaymentId, String status, long amountCents, String currency) {}
    record GatewayPayoutResult(boolean success, String gatewayPayoutId, String status) {}
    record GatewayReconciliationResult(boolean exists, String status, long amountCents, String currency) {}
}

