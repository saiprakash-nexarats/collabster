package com.collabsphere_first.notification.dto;

import com.collabsphere_first.notification.enums.DevicePlatform;
import com.collabsphere_first.notification.enums.DeviceProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceRequest {
    @NotNull(message = "Platform is required")
    private DevicePlatform platform;

    @NotNull(message = "Provider is required")
    private DeviceProvider provider;

    @NotBlank(message = "Device token is required")
    @Size(max = 1024, message = "Token cannot exceed 1024 characters")
    private String token;
}
