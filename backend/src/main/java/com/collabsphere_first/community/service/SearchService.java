package com.collabsphere_first.community.service;

import java.util.List;
import java.util.Map;

/**
 * Optional search indexing interface.
 * If no implementation bean is provided, search falls back to JPA repository queries.
 */
public interface SearchService {
    <T> List<T> search(String query, String indexName, Map<String, Object> filters, Class<T> targetClass);
}
