package com.collabsphere_first.recommendation.feedback.enums;

public enum FeedbackOutboxStatus {
    PENDING,
    IN_PROGRESS,
    RETRY,
    DELIVERED,
    DEAD_LETTER,
    FAILED
}
