package com.collabsphere_first.community.service;

import java.util.Map;

/**
 * Optional analytics tracking interface.
 * If no implementation bean is provided, calls to this service are safely skipped.
 */
public interface AnalyticsService {
    void trackEvent(String eventName, Map<String, Object> properties);
}
