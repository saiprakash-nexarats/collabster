package com.collabsphere_first.community.service.impl;

import com.collabsphere_first.community.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Default no-op implementation of AnalyticsService.
 * Ensures Spring startup succeeds without requiring an external analytics provider.
 */
@Slf4j
@Service
public class NoOpAnalyticsServiceImpl implements AnalyticsService {

    @Override
    public void trackEvent(String eventName, Map<String, Object> properties) {
        log.debug("[ANALYTICS NO-OP] Event tracked: {} with props: {}", eventName, properties);
    }
}
