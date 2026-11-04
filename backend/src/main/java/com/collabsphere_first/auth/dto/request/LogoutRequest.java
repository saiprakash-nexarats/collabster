package com.collabsphere_first.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    /**
     * Optional notification device ID for current-device logout.
     * When supplied, only this specific device's push token is revoked.
     * Other devices remain active.
     */
    private UUID deviceId;

    /**
     * Optional raw push token for current-device logout fallback.
     */
    private String pushToken;
}
