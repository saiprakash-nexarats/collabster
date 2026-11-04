package com.collabsphere_first.notification.delivery.push;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PushSendResult {
    boolean success;
    String providerMessageId;
    String errorCode;
    String errorMessage;
    boolean shouldRevokeToken;
    boolean retryable;
    boolean skipped;

    public static PushSendResult success(String providerMessageId) {
        return PushSendResult.builder()
                .success(true)
                .providerMessageId(providerMessageId)
                .build();
    }

    public static PushSendResult retryableFailure(String errorCode, String errorMessage) {
        return PushSendResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryable(true)
                .shouldRevokeToken(false)
                .build();
    }

    public static PushSendResult permanentFailure(String errorCode, String errorMessage, boolean shouldRevokeToken) {
        return PushSendResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryable(false)
                .shouldRevokeToken(shouldRevokeToken)
                .build();
    }

    public static PushSendResult skipped(String reason) {
        return PushSendResult.builder()
                .success(false)
                .skipped(true)
                .errorCode("SKIPPED")
                .errorMessage(reason)
                .build();
    }
}
