package com.collabsphere_first.payment.gateway.impl;

import com.collabsphere_first.payment.gateway.PaymentGatewayProvider;
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
    private final ConcurrentMap<String, GatewayReconciliationResult> knownOrders = new ConcurrentHashMap<>();

    public SandboxPaymentGatewayProvider(
            @Value("${collobster.payment.sandbox.webhook-secret:collobster_sandbox_webhook_secret_key_2026_test}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public GatewayOrderResult createOrder(long amountCents, String currency, String orderNumber, String description) {
        String gatewayOrderId = "sandbox_ord_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        knownOrders.put(gatewayOrderId, new GatewayReconciliationResult(true, "created", amountCents, currency));
        log.info("[SANDBOX GATEWAY] Created order {} for amount {} {}", gatewayOrderId, amountCents, currency);
        return new GatewayOrderResult(gatewayOrderId, "created");
    }

    @Override
    public GatewayVerificationResult verifyPayment(String gatewayPaymentId, String gatewayOrderId) {
        if (gatewayPaymentId == null || gatewayPaymentId.isBlank()) {
            return new GatewayVerificationResult(false, null, "failed", 0L, "INR");
        }

        // Must match sandbox convention or pay_ format
        boolean formatValid = gatewayPaymentId.startsWith("sandbox_pay_")
                || gatewayPaymentId.startsWith("pay_")
                || gatewayPaymentId.startsWith("CL-");

        if (!formatValid) {
            log.warn("[SANDBOX GATEWAY] Rejected invalid payment id: {}", gatewayPaymentId);
            return new GatewayVerificationResult(false, gatewayPaymentId, "invalid_payment_id", 0L, "INR");
        }

        GatewayReconciliationResult known = gatewayOrderId != null ? knownOrders.get(gatewayOrderId) : null;
        long amount = known != null ? known.amountCents() : 0L;
        String currency = known != null ? known.currency() : "INR";

        if (known != null) {
            knownOrders.put(gatewayOrderId, new GatewayReconciliationResult(true, "paid", amount, currency));
        }

        log.info("[SANDBOX GATEWAY] Verified payment {} for order {}", gatewayPaymentId, gatewayOrderId);
        return new GatewayVerificationResult(true, gatewayPaymentId, "paid", amount, currency);
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
        String gatewayPayoutId = "sandbox_pout_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[SANDBOX GATEWAY] Processed payout {} for {} {} to {}", gatewayPayoutId, amountCents, currency, destinationReference);
        return new GatewayPayoutResult(true, gatewayPayoutId, "completed");
    }

    @Override
    public GatewayReconciliationResult reconcile(String gatewayOrderId) {
        if (gatewayOrderId == null) {
            return new GatewayReconciliationResult(false, "not_found", 0L, "INR");
        }
        GatewayReconciliationResult known = knownOrders.get(gatewayOrderId);
        if (known != null) {
            return known;
        }
        // Fallback for mock sandbox order
        return new GatewayReconciliationResult(true, "paid", 0L, "INR");
    }
}
