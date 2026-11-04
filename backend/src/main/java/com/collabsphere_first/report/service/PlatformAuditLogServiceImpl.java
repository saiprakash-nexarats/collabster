package com.collabsphere_first.report.service;

import com.collabsphere_first.report.entity.PlatformAuditLog;
import com.collabsphere_first.report.repository.PlatformAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAuditLogServiceImpl implements PlatformAuditLogService {

    private final PlatformAuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Long actorId, String action, String targetType, String targetId, String metadata) {
        if (actorId == null) {
            log.warn("Cannot record privileged audit log without an authenticated actor ID");
            return;
        }

        PlatformAuditLog auditLog = PlatformAuditLog.builder()
                .id(UUID.randomUUID())
                .actorId(actorId)
                .action(action)
                .targetType(targetType != null ? targetType : "UNKNOWN")
                .targetId(targetId != null ? targetId : "UNKNOWN")
                .metadata(metadata)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Recorded platform privileged audit action: {} by actor {} on {} {}", action, actorId, targetType, targetId);
    }
}
