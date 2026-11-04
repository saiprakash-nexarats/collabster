package com.collabsphere_first.notification.service;

import com.collabsphere_first.notification.dto.NotificationPreferenceDto;
import com.collabsphere_first.notification.dto.UpdateNotificationPreferenceRequest;
import com.collabsphere_first.notification.enums.NotificationCategory;

import java.util.List;

public interface NotificationPreferenceService {
    List<NotificationPreferenceDto> getPreferences(Long userId);
    NotificationPreferenceDto updatePreference(Long userId, UpdateNotificationPreferenceRequest request);
    boolean isInAppEnabled(Long userId, NotificationCategory category);
    boolean isPushEnabled(Long userId, NotificationCategory category);
    boolean isEmailEnabled(Long userId, NotificationCategory category);
    java.util.Set<com.collabsphere_first.notification.enums.NotificationChannel> determineEligibleChannels(Long recipientUserId, NotificationCategory category, boolean conversationMuted);
}
