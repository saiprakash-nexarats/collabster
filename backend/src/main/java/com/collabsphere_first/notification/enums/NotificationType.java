package com.collabsphere_first.notification.enums;

public enum NotificationType {
    NEW_FOLLOW(NotificationCategory.SOCIAL),
    CONNECTION_REQUESTED(NotificationCategory.SOCIAL),
    CONNECTION_ACCEPTED(NotificationCategory.SOCIAL),
    DIRECT_MESSAGE(NotificationCategory.MESSAGING),
    DIRECT_MESSAGE_REACTION(NotificationCategory.MESSAGING),
    COMMUNITY_INVITATION(NotificationCategory.COMMUNITY),
    COMMUNITY_POST_ACTIVITY(NotificationCategory.COMMUNITY),
    COMMUNITY_EVENT(NotificationCategory.COMMUNITY),
    COMMUNITY_MENTION(NotificationCategory.COMMUNITY),
    SECURITY_ALERT(NotificationCategory.SECURITY);

    private final NotificationCategory category;

    NotificationType(NotificationCategory category) {
        this.category = category;
    }

    public NotificationCategory getCategory() {
        return category;
    }
}
