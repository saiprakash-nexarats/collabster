package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.entity.CommunityRule;
import com.collabsphere_first.community.entity.CommunityRuleRevision;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.repository.*;
import com.collabsphere_first.community.security.CommunityAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class CommunityRuleService {
    private final CommunityRuleRepository ruleRepository;
    private final CommunityRuleRevisionRepository revisionRepository;
    private final CommunityAccessService accessService;
    private final CommunityAuditLogService auditLogService;
    private final CommunityEventOutboxService outboxService;

    @Transactional(readOnly = true)
    public List<CommunityRuleDto> list(UUID communityId, Long userId) {
        var community = accessService.requireCommunity(communityId);
        if (community.getVisibility() != Visibility.PUBLIC) accessService.requireMember(communityId, userId);
        return ruleRepository.findByCommunityIdAndActiveTrueOrderByPositionAsc(communityId).stream().map(this::toDto).toList();
    }

    @Transactional
    public CommunityRuleDto create(UUID communityId, Long userId, CreateCommunityRuleRequest request) {
        accessService.requireRole(communityId, userId, MembershipRole.ADMIN);
        int position = request.getPosition() == null ? ruleRepository.countByCommunityId(communityId) : Math.max(0, request.getPosition());
        CommunityRule rule = ruleRepository.save(CommunityRule.builder().community(accessService.requireCommunity(communityId))
                .title(request.getTitle().trim()).body(request.getBody().trim()).position(position).createdBy(userId).build());
        saveRevision(rule, userId);
        auditLogService.record(rule.getCommunity(), userId, "RULE_CREATED", "RULE", rule.getId().toString(), "version=1");
        outboxService.record(communityId, "RULE_CREATED", rule.getId().toString(), java.util.Map.of("createdBy", userId, "version", 1));
        return toDto(rule);
    }

    @Transactional
    public CommunityRuleDto update(UUID communityId, UUID ruleId, Long userId, UpdateCommunityRuleRequest request) {
        accessService.requireRole(communityId, userId, MembershipRole.ADMIN);
        CommunityRule rule = find(communityId, ruleId);
        if (!rule.isActive()) throw new InvalidJoinException("Inactive rules cannot be edited");
        if (request.getTitle() != null) rule.setTitle(request.getTitle().trim());
        if (request.getBody() != null) rule.setBody(request.getBody().trim());
        if (request.getPosition() != null) rule.setPosition(Math.max(0, request.getPosition()));
        rule.setVersion(rule.getVersion() + 1);
        rule.setUpdatedAt(Instant.now());
        ruleRepository.save(rule);
        saveRevision(rule, userId);
        auditLogService.record(rule.getCommunity(), userId, "RULE_UPDATED", "RULE", rule.getId().toString(), "version=" + rule.getVersion());
        outboxService.record(communityId, "RULE_UPDATED", rule.getId().toString(), java.util.Map.of("updatedBy", userId, "version", rule.getVersion()));
        return toDto(rule);
    }

    @Transactional
    public void deactivate(UUID communityId, UUID ruleId, Long userId) {
        accessService.requireRole(communityId, userId, MembershipRole.ADMIN);
        CommunityRule rule = find(communityId, ruleId);
        if (!rule.isActive()) return;
        rule.setActive(false);
        rule.setVersion(rule.getVersion() + 1);
        rule.setUpdatedAt(Instant.now());
        ruleRepository.save(rule);
        saveRevision(rule, userId);
        auditLogService.record(rule.getCommunity(), userId, "RULE_DEACTIVATED", "RULE", rule.getId().toString(), "version=" + rule.getVersion());
        outboxService.record(communityId, "RULE_DEACTIVATED", rule.getId().toString(), java.util.Map.of("changedBy", userId, "version", rule.getVersion()));
    }

    @Transactional(readOnly = true)
    public List<CommunityRuleRevisionDto> revisions(UUID communityId, UUID ruleId, Long userId) {
        if (accessService.requireCommunity(communityId).getVisibility() != Visibility.PUBLIC) accessService.requireMember(communityId, userId);
        find(communityId, ruleId);
        return revisionRepository.findByRuleIdOrderByVersionDesc(ruleId).stream().map(r -> CommunityRuleRevisionDto.builder()
                .id(r.getId()).ruleId(ruleId).version(r.getVersion()).title(r.getTitle()).body(r.getBody())
                .active(r.isActive()).changedBy(r.getChangedBy()).createdAt(r.getCreatedAt()).build()).toList();
    }

    private CommunityRule find(UUID communityId, UUID ruleId) {

    System.out.println("========== RULE DEBUG ==========");
    System.out.println("communityId = " + communityId);
    System.out.println("ruleId      = " + ruleId);

    CommunityRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> {
                System.out.println("RULE NOT FOUND IN DATABASE: " + ruleId);
                return new InvalidJoinException("Rule not found");
            });

    System.out.println("RULE FOUND: " + rule.getId());
    System.out.println("RULE COMMUNITY: " + rule.getCommunity().getId());

    if (!rule.getCommunity().getId().equals(communityId)) {
        System.out.println("COMMUNITY MISMATCH!");
        System.out.println("Expected: " + communityId);
        System.out.println("Actual:   " + rule.getCommunity().getId());
        throw new InvalidJoinException("Rule does not belong to this community");
    }

    System.out.println("========== RULE DEBUG END ==========");

    return rule;
}
    private void saveRevision(CommunityRule rule, Long userId) {
        revisionRepository.save(CommunityRuleRevision.builder().rule(rule).version(rule.getVersion()).title(rule.getTitle())
                .body(rule.getBody()).active(rule.isActive()).changedBy(userId).createdAt(Instant.now()).build());
    }
    private CommunityRuleDto toDto(CommunityRule rule) {
        return CommunityRuleDto.builder().id(rule.getId()).communityId(rule.getCommunity().getId()).title(rule.getTitle())
                .body(rule.getBody()).position(rule.getPosition()).version(rule.getVersion()).active(rule.isActive())
                .updatedAt(rule.getUpdatedAt()).build();
    }
}
