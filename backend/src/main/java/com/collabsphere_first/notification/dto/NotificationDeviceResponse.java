package com.collabsphere_first.notification.dto;

import com.collabsphere_first.notification.enums.DevicePlatform;
import com.collabsphere_first.notification.enums.DeviceProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeviceResponse {
    private UUID id;
    private DevicePlatform platform;
    private DeviceProvider provider;
    private boolean enabled;
    private Instant createdAt;
    private Instant lastSeenAt;
}
