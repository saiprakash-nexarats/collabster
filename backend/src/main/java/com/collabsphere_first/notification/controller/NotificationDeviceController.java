package com.collabsphere_first.notification.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.notification.dto.NotificationDeviceResponse;
import com.collabsphere_first.notification.dto.RegisterDeviceRequest;
import com.collabsphere_first.notification.service.NotificationDeviceService;
import com.collabsphere_first.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-center/devices")
@RequiredArgsConstructor
public class NotificationDeviceController {

    private final NotificationDeviceService deviceService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationDeviceResponse>> registerDevice(
            @RequestBody @Valid RegisterDeviceRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        NotificationDeviceResponse response = deviceService.registerDevice(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Device registered successfully", response));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> revokeDevice(
            @PathVariable("deviceId") UUID deviceId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        deviceService.revokeDevice(deviceId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Device revoked successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDeviceResponse>>> getUserDevices() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<NotificationDeviceResponse> devices = deviceService.getUserDevices(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("User devices retrieved", devices));
    }
}
