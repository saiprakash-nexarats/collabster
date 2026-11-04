package com.collabsphere_first.notification.dto;

import com.collabsphere_first.notification.enums.NotificationCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationPreferenceRequest {
    @NotNull(message = "Category is required")
    private NotificationCategory category;
    private Boolean inAppEnabled;
    private Boolean pushEnabled;
    private Boolean emailEnabled;
}
