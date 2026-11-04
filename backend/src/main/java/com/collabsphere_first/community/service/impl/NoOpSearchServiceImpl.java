package com.collabsphere_first.community.service.impl;

import com.collabsphere_first.community.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Default no-op implementation of SearchService.
 * Fallback when Elasticsearch / external search is disabled — allows DiscoveryService
 * to default cleanly to PostgreSQL JPA queries.
 */
@Slf4j
@Service
public class NoOpSearchServiceImpl implements SearchService {

    @Override
    public <T> List<T> search(String query, String indexName, Map<String, Object> filters, Class<T> targetClass) {
        log.debug("[SEARCH NO-OP] Query '{}' on index '{}' falling back to JPA repository", query, indexName);
        return Collections.emptyList();
    }
}
