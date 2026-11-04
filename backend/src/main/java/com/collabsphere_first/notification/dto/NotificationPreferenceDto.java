package com.collabsphere_first.notification.dto;

import com.collabsphere_first.notification.enums.NotificationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDto {
    private NotificationCategory category;
    private boolean inAppEnabled;
    private boolean pushEnabled;
    private boolean emailEnabled;
}
