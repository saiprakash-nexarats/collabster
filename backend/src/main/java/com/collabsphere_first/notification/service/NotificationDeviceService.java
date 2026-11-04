package com.collabsphere_first.notification.service;

import com.collabsphere_first.notification.dto.NotificationDeviceResponse;
import com.collabsphere_first.notification.dto.RegisterDeviceRequest;
import com.collabsphere_first.notification.entity.NotificationDevice;

import java.util.List;
import java.util.UUID;

public interface NotificationDeviceService {
    NotificationDeviceResponse registerDevice(Long userId, RegisterDeviceRequest request);
    void revokeDevice(UUID deviceId, Long userId);
    void revokeDeviceByToken(String token);
    void revokeAllForUser(Long userId);
    List<NotificationDeviceResponse> getUserDevices(Long userId);
    List<NotificationDevice> getActiveDevicesForUser(Long userId);
}
