package com.collabsphere_first.notification.service;

import com.collabsphere_first.notification.dto.NotificationDeviceResponse;
import com.collabsphere_first.notification.dto.RegisterDeviceRequest;
import com.collabsphere_first.notification.entity.NotificationDevice;
import com.collabsphere_first.notification.exception.NotificationAccessDeniedException;
import com.collabsphere_first.notification.exception.NotificationNotFoundException;
import com.collabsphere_first.notification.repository.NotificationDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeviceServiceImpl implements NotificationDeviceService {

    private final NotificationDeviceRepository deviceRepository;

    @Override
    @Transactional
    public NotificationDeviceResponse registerDevice(Long userId, RegisterDeviceRequest request) {
        String token = request.getToken();
        String tokenHash = hashToken(token);
        Instant now = Instant.now();

        Optional<NotificationDevice> existingOpt = deviceRepository.findByTokenHash(tokenHash);
        NotificationDevice device;

        if (existingOpt.isPresent()) {
            device = existingOpt.get();
            if (!device.getUserId().equals(userId)) {
                // Token reassignment: device was transferred/re-logged by another user
                log.info("Reassigning notification device from user {} to user {}",
                        device.getUserId(), userId);
                device.reassignToUser(userId, now);
            } else {
                device.touch(now);
            }
            device.setPlatform(request.getPlatform());
            device.setProvider(request.getProvider());
            device.setToken(token);
        } else {
            device = NotificationDevice.builder()
                    .userId(userId)
                    .platform(request.getPlatform())
                    .provider(request.getProvider())
                    .token(token)
                    .tokenHash(tokenHash)
                    .enabled(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .lastSeenAt(now)
                    .build();
        }

        NotificationDevice saved;
        try {
            saved = deviceRepository.save(device);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.info("Concurrent device registration detected for token hash; re-fetching for reassignment");
            NotificationDevice existing = deviceRepository.findByTokenHash(tokenHash)
                    .orElseThrow(() -> ex);
            if (!existing.getUserId().equals(userId)) {
                existing.reassignToUser(userId, now);
            } else {
                existing.touch(now);
            }
            existing.setPlatform(request.getPlatform());
            existing.setProvider(request.getProvider());
            existing.setToken(token);
            saved = deviceRepository.save(existing);
        }

        log.info("Registered notification device (deviceId={}, platform={}, provider={})",
                saved.getId(), saved.getPlatform(), saved.getProvider());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void revokeDevice(UUID deviceId, Long userId) {
        NotificationDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotificationNotFoundException("Device not found with id: " + deviceId));

        if (!device.getUserId().equals(userId)) {
            throw new NotificationAccessDeniedException("You do not have permission to revoke this device");
        }

        device.revoke(Instant.now());
        deviceRepository.save(device);
        log.info("Revoked notification device (deviceId={}, userId={})", deviceId, userId);
    }

    @Override
    @Transactional
    public void revokeDeviceByToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String tokenHash = hashToken(token);
        int rows = deviceRepository.revokeByTokenHash(tokenHash, Instant.now());
        if (rows > 0) {
            log.info("Revoked notification device by token hash");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDeviceResponse> getUserDevices(Long userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        if (userId == null) {
            return;
        }
        int revokedCount = deviceRepository.revokeAllByUserId(userId, Instant.now());
        log.info("Revoked {} notification devices for user {}", revokedCount, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDevice> getActiveDevicesForUser(Long userId) {
        return deviceRepository.findByUserIdAndEnabledTrue(userId);
    }

    private NotificationDeviceResponse mapToResponse(NotificationDevice device) {
        return NotificationDeviceResponse.builder()
                .id(device.getId())
                .platform(device.getPlatform())
                .provider(device.getProvider())
                .enabled(device.isEnabled())
                .createdAt(device.getCreatedAt())
                .lastSeenAt(device.getLastSeenAt())
                .build();
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
