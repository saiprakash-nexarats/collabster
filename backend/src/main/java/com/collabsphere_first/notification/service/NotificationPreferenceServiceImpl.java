package com.collabsphere_first.notification.service;

import com.collabsphere_first.notification.dto.NotificationPreferenceDto;
import com.collabsphere_first.notification.dto.UpdateNotificationPreferenceRequest;
import com.collabsphere_first.notification.entity.NotificationPreference;
import com.collabsphere_first.notification.entity.NotificationPreferenceId;
import com.collabsphere_first.notification.enums.NotificationCategory;
import com.collabsphere_first.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final com.collabsphere_first.notification.config.NotificationProperties properties;

    @Override
    @Transactional(readOnly = true)
    public Set<com.collabsphere_first.notification.enums.NotificationChannel> determineEligibleChannels(
            Long recipientUserId, NotificationCategory category, boolean conversationMuted) {
        if (conversationMuted) {
            // Muted conversation suppresses all external pushes/emails AND realtime bell notifications
            return Collections.emptySet();
        }

        Set<com.collabsphere_first.notification.enums.NotificationChannel> channels = new HashSet<>();
        if (properties.getRealtime().isEnabled()) {
            channels.add(com.collabsphere_first.notification.enums.NotificationChannel.REALTIME);
        }
        if (isPushEnabled(recipientUserId, category)) {
            channels.add(com.collabsphere_first.notification.enums.NotificationChannel.PUSH);
        }
        if (isEmailEnabled(recipientUserId, category)) {
            channels.add(com.collabsphere_first.notification.enums.NotificationChannel.EMAIL);
        }
        return channels;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceDto> getPreferences(Long userId) {
        List<NotificationPreference> storedPreferences = preferenceRepository.findById_UserId(userId);
        Map<NotificationCategory, NotificationPreference> map = storedPreferences.stream()
                .collect(Collectors.toMap(p -> p.getId().getCategory(), p -> p));

        List<NotificationPreferenceDto> result = new ArrayList<>();
        for (NotificationCategory category : NotificationCategory.values()) {
            NotificationPreference pref = map.get(category);
            if (pref != null) {
                result.add(mapToDto(pref));
            } else {
                result.add(getDefaultPreference(category));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public NotificationPreferenceDto updatePreference(Long userId, UpdateNotificationPreferenceRequest request) {
        NotificationCategory category = request.getCategory();
        NotificationPreferenceId id = new NotificationPreferenceId(userId, category);

        NotificationPreference preference = preferenceRepository.findById(id)
                .orElseGet(() -> {
                    NotificationPreferenceDto defaultPref = getDefaultPreference(category);
                    return NotificationPreference.builder()
                            .id(id)
                            .inAppEnabled(defaultPref.isInAppEnabled())
                            .pushEnabled(defaultPref.isPushEnabled())
                            .emailEnabled(defaultPref.isEmailEnabled())
                            .updatedAt(Instant.now())
                            .build();
                });

        if (request.getInAppEnabled() != null) {
            // Section 29: User preference must not disable security-critical in-app notifications
            if (category == NotificationCategory.SECURITY && !request.getInAppEnabled()) {
                log.info("Ignored attempt to disable in-app notifications for SECURITY category (userId={})", userId);
                preference.setInAppEnabled(true);
            } else {
                preference.setInAppEnabled(request.getInAppEnabled());
            }
        }

        if (request.getPushEnabled() != null) {
            preference.setPushEnabled(request.getPushEnabled());
        }

        if (request.getEmailEnabled() != null) {
            preference.setEmailEnabled(request.getEmailEnabled());
        }

        preference.setUpdatedAt(Instant.now());
        NotificationPreference saved = preferenceRepository.save(preference);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInAppEnabled(Long userId, NotificationCategory category) {
        if (category == NotificationCategory.SECURITY) {
            return true;
        }
        NotificationPreferenceId id = new NotificationPreferenceId(userId, category);
        return preferenceRepository.findById(id)
                .map(NotificationPreference::isInAppEnabled)
                .orElseGet(() -> getDefaultPreference(category).isInAppEnabled());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPushEnabled(Long userId, NotificationCategory category) {
        NotificationPreferenceId id = new NotificationPreferenceId(userId, category);
        return preferenceRepository.findById(id)
                .map(NotificationPreference::isPushEnabled)
                .orElseGet(() -> getDefaultPreference(category).isPushEnabled());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailEnabled(Long userId, NotificationCategory category) {
        NotificationPreferenceId id = new NotificationPreferenceId(userId, category);
        return preferenceRepository.findById(id)
                .map(NotificationPreference::isEmailEnabled)
                .orElseGet(() -> getDefaultPreference(category).isEmailEnabled());
    }

    private NotificationPreferenceDto getDefaultPreference(NotificationCategory category) {
        return switch (category) {
            case SECURITY -> NotificationPreferenceDto.builder()
                    .category(category)
                    .inAppEnabled(true)
                    .pushEnabled(true)
                    .emailEnabled(true)
                    .build();
            case MESSAGING, SOCIAL, COMMUNITY, SYSTEM -> NotificationPreferenceDto.builder()
                    .category(category)
                    .inAppEnabled(true)
                    .pushEnabled(true)
                    .emailEnabled(false)
                    .build();
        };
    }

    private NotificationPreferenceDto mapToDto(NotificationPreference entity) {
        return NotificationPreferenceDto.builder()
                .category(entity.getId().getCategory())
                .inAppEnabled(entity.isInAppEnabled())
                .pushEnabled(entity.isPushEnabled())
                .emailEnabled(entity.isEmailEnabled())
                .build();
    }
}
