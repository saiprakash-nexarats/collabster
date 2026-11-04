package com.collabsphere_first.community.service;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.CommunityAuditLog;
import com.collabsphere_first.community.repository.CommunityAuditLogRepository;
import com.collabsphere_first.community.dto.CommunityAuditLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityAuditLogService {
    private final CommunityAuditLogRepository repository;

    public void record(Community community, Long actorId, String action, String targetType, String targetId, String metadata) {
        repository.save(CommunityAuditLog.builder()
                .communityId(community.getId())
                .actorId(actorId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .metadata(metadata)
                .build());
    }

    public Page<CommunityAuditLogDto> list(java.util.UUID communityId, Pageable pageable) {
        return repository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable).map(log -> CommunityAuditLogDto.builder()
                .id(log.getId()).communityId(log.getCommunityId()).actorId(log.getActorId())
                .action(log.getAction()).targetType(log.getTargetType()).targetId(log.getTargetId())
                .metadata(log.getMetadata()).createdAt(log.getCreatedAt()).build());
    }
}
