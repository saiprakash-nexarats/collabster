package com.collabsphere_first.payment.gateway.impl;

import com.collabsphere_first.payment.gateway.PaymentGatewayProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class SandboxPaymentGatewayProvider implements PaymentGatewayProvider {

    private static final String PROVIDER_NAME = "SANDBOX";
    private final String webhookSecret;
    private final String configuredProvider;
    private final String activeProfile;

    public record SandboxOrderRecord(
            String gatewayOrderId,
            long amountCents,
            String currency,
            String orderNumber,
            String status,
            String authorizedPaymentId
    ) {}

    public record SandboxPaymentRecord(
            String gatewayPaymentId,
            String gatewayOrderId,
            long amountCents,
            String currency,
            String status
    ) {}

    private final ConcurrentMap<String, SandboxOrderRecord> knownOrders = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SandboxPaymentRecord> knownPayments = new ConcurrentHashMap<>();

    public SandboxPaymentGatewayProvider(
            @Value("${collobster.payment.sandbox.webhook-secret:collobster_sandbox_webhook_secret_key_2026_test}") String webhookSecret,
            @Value("${collobster.payment.gateway.provider:SANDBOX}") String configuredProvider,
            @Value("${spring.profiles.active:test}") String activeProfile) {
        this.webhookSecret = webhookSecret;
        this.configuredProvider = configuredProvider;
        this.activeProfile = activeProfile;
    }

    @PostConstruct
    public void validateConfiguration() {
        if ("production".equalsIgnoreCase(activeProfile) || "prod".equalsIgnoreCase(activeProfile)) {
            if ("SANDBOX".equalsIgnoreCase(configuredProvider)) {
                log.error("[SECURITY] Sandbox payment gateway is not permitted in production mode without explicit configuration!");
                throw new IllegalStateException("Production environment detected: Live payment provider configuration required. Sandbox cannot be used as default live payment processor.");
            }
            if (webhookSecret == null || webhookSecret.contains("collobster_sandbox_webhook_secret_key")) {
                throw new IllegalStateException("Production environment cannot use default or test webhook secrets.");
            }
        }
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Payment gateway webhook secret must be configured");
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public GatewayOrderResult createOrder(long amountCents, String currency, String orderNumber, String description) {
        String gatewayOrderId = "sandbox_ord_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String authorizedPaymentId = "sandbox_pay_" + gatewayOrderId.substring("sandbox_ord_".length());

        SandboxOrderRecord orderRecord = new SandboxOrderRecord(gatewayOrderId, amountCents, currency, orderNumber, "created", authorizedPaymentId);
        knownOrders.put(gatewayOrderId, orderRecord);

        // Pre-register authorized transaction for this isolated sandbox order
        SandboxPaymentRecord paymentRecord = new SandboxPaymentRecord(authorizedPaymentId, gatewayOrderId, amountCents, currency, "captured");
        knownPayments.put(authorizedPaymentId, paymentRecord);

        log.info("[SANDBOX GATEWAY] Created order {} (auth payment {}) for amount {} {}", gatewayOrderId, authorizedPaymentId, amountCents, currency);
        return new GatewayOrderResult(gatewayOrderId, "created");
    }

    public void registerTestPayment(String gatewayPaymentId, String gatewayOrderId, long amountCents, String currency, String status) {
        knownPayments.put(gatewayPaymentId, new SandboxPaymentRecord(gatewayPaymentId, gatewayOrderId, amountCents, currency, status));
    }

    @Override
    public GatewayVerificationResult verifyPayment(String gatewayPaymentId, String gatewayOrderId) {
        if (gatewayPaymentId == null || gatewayPaymentId.isBlank()) {
            log.warn("[SANDBOX GATEWAY] Payment verification rejected: missing payment id");
            return new GatewayVerificationResult(false, null, "missing_payment_id", 0L, "INR");
        }

        if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
            log.warn("[SANDBOX GATEWAY] Payment verification rejected: missing order id");
            return new GatewayVerificationResult(false, gatewayPaymentId, "missing_order_id", 0L, "INR");
        }

        SandboxPaymentRecord payment = knownPayments.get(gatewayPaymentId.trim());
        if (payment == null) {
            log.warn("[SANDBOX GATEWAY] Rejected unknown transaction: {}", gatewayPaymentId);
            return new GatewayVerificationResult(false, gatewayPaymentId, "unknown_transaction", 0L, "INR");
        }

        if (!gatewayOrderId.equals(payment.gatewayOrderId())) {
            log.warn("[SANDBOX GATEWAY] Payment {} does not belong to order {}. Belongs to {}", gatewayPaymentId, gatewayOrderId, payment.gatewayOrderId());
            return new GatewayVerificationResult(false, gatewayPaymentId, "order_mismatch", 0L, "INR");
        }

        if (!"captured".equalsIgnoreCase(payment.status()) && !"paid".equalsIgnoreCase(payment.status())) {
            log.warn("[SANDBOX GATEWAY] Payment {} status is '{}', not captured/paid", gatewayPaymentId, payment.status());
            return new GatewayVerificationResult(false, gatewayPaymentId, payment.status(), payment.amountCents(), payment.currency());
        }

        SandboxOrderRecord order = knownOrders.get(gatewayOrderId);
        if (order != null) {
            knownOrders.put(gatewayOrderId, new SandboxOrderRecord(order.gatewayOrderId(), order.amountCents(), order.currency(), order.orderNumber(), "paid", gatewayPaymentId));
        }

        log.info("[SANDBOX GATEWAY] Verified payment {} for order {} (amount {} {})", gatewayPaymentId, gatewayOrderId, payment.amountCents(), payment.currency());
        return new GatewayVerificationResult(true, gatewayPaymentId, "paid", payment.amountCents(), payment.currency());
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signature) {
        if (signature == null || rawPayload == null || signature.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);

            // Constant time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error computing webhook signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public GatewayPayoutResult processPayout(long amountCents, String currency, String destinationReference) {
        if (amountCents <= 0) {
            return new GatewayPayoutResult(false, null, "failed");
        }
        if (destinationReference == null || destinationReference.isBlank()) {
            return new GatewayPayoutResult(false, null, "failed");
        }
        String gatewayPayoutId = "sandbox_pout_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[SANDBOX GATEWAY] Processed sandbox payout {} for {} {} to {}", gatewayPayoutId, amountCents, currency, destinationReference);
        return new GatewayPayoutResult(true, gatewayPayoutId, "completed");
    }

    @Override
    public GatewayReconciliationResult reconcile(String gatewayOrderId) {
        if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
            return new GatewayReconciliationResult(false, "not_found", 0L, "INR");
        }
        SandboxOrderRecord order = knownOrders.get(gatewayOrderId.trim());
        if (order == null) {
            return new GatewayReconciliationResult(false, "not_found", 0L, "INR");
        }
        return new GatewayReconciliationResult(true, order.status(), order.amountCents(), order.currency());
    }
}
