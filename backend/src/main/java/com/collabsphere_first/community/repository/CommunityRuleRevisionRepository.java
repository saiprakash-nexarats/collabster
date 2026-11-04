package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityRuleRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommunityRuleRevisionRepository
        extends JpaRepository<CommunityRuleRevision, UUID> {

    @Query("""
        SELECT r
        FROM CommunityRuleRevision r
        WHERE r.rule.id = :ruleId
        ORDER BY r.version DESC
        """)
    List<CommunityRuleRevision> findByRuleIdOrderByVersionDesc(
            @Param("ruleId") UUID ruleId
    );
}