package com.collabsphere_first.report.service;

public interface PlatformAuditLogService {

    void logAction(Long actorId, String action, String targetType, String targetId, String metadata);
}
