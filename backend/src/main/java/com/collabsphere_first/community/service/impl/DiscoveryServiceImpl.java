package com.collabsphere_first.community.service.impl;

import com.collabsphere_first.community.dto.CommunityDto;
import com.collabsphere_first.community.dto.CommunitySearchQuery;
import com.collabsphere_first.community.mapper.CommunityMapper;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.PostRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.service.DiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryServiceImpl implements DiscoveryService {

    private final CommunityRepository communityRepository;
    private final PostRepository postRepository;
    private final CommunityMapper communityMapper;
    private final MembershipRepository membershipRepository;

    @Value("${collabster.communities.activity-decay-factor:0.95}")
    private double activityDecayFactor;

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityDto> searchCommunities(CommunitySearchQuery searchQuery) {
        int page = Math.max(0, searchQuery.getPage());
        int size = Math.max(1, Math.min(searchQuery.getSize(), 100));
        Pageable pageable = PageRequest.of(page, size);
        // Discovery is intentionally public-only; private and invite-only data is
        // surfaced through the authenticated membership/invitation flows.
        if (searchQuery.getVisibility() != null && searchQuery.getVisibility() != Visibility.PUBLIC) {
            return Page.empty(pageable);
        }
        String query = searchQuery.getQuery() == null || searchQuery.getQuery().isBlank()
                ? null : searchQuery.getQuery().trim();
        String tag = searchQuery.getTag() == null || searchQuery.getTag().isBlank()
                ? null : searchQuery.getTag().trim();
        return communityRepository.discoverPublic(query,
                searchQuery.getType() == null ? null : searchQuery.getType().name(),
                searchQuery.getCategory() == null ? null : searchQuery.getCategory().name(), tag, pageable)
                .map(communityMapper::toDto);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public Page<CommunityDto> getTrendingCommunities(int page, int size) {
//        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
//        return communityRepository.findByVisibilityAndCommunityStatus(Visibility.PUBLIC, CommunityStatus.ACTIVE, pageable)
//                .map(communityMapper::toDto);
//    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityDto> getTrendingCommunities(int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(size, 100)),
                Sort.by(
                        Sort.Order.desc("activityScore"),
                        Sort.Order.desc("createdAt") // deterministic tie-breaker
                )
        );

        return communityRepository
                .findByVisibilityAndCommunityStatus(
                        Visibility.PUBLIC,
                        CommunityStatus.ACTIVE,
                        pageable
                )
                .map(communityMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityDto> recommendCommunities(Long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        List<Membership> memberships = membershipRepository.findByUserId(userId).stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .toList();
        Set<java.util.UUID> joined = memberships.stream().map(m -> m.getCommunity().getId())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> preferredTags = memberships.stream().flatMap(m -> m.getCommunity().getTags() == null
                        ? java.util.stream.Stream.<String>empty() : m.getCommunity().getTags().stream())
                .map(String::toLowerCase).collect(java.util.stream.Collectors.toSet());
        Set<com.collabsphere_first.community.enums.CommunityCategory> preferredCategories = memberships.stream()
                .map(m -> m.getCommunity().getCategory()).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<Community> candidates = communityRepository.findByVisibilityAndCommunityStatus(
                        Visibility.PUBLIC, CommunityStatus.ACTIVE,
                        PageRequest.of(0, 100, Sort.by(Sort.Order.desc("activityScore"), Sort.Order.desc("memberCount"))))
                .getContent();
        List<CommunityDto> ranked = candidates.stream().filter(c -> !joined.contains(c.getId()))
                .sorted(Comparator.<Community>comparingInt(c -> recommendationScore(c, preferredCategories, preferredTags))
                        .reversed().thenComparing(Community::getActivityScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(communityMapper::toDto).toList();
        int from = Math.min(safePage * safeSize, ranked.size());
        int to = Math.min(from + safeSize, ranked.size());
        return new org.springframework.data.domain.PageImpl<>(ranked.subList(from, to), PageRequest.of(safePage, safeSize), ranked.size());
    }

    private int recommendationScore(Community community,
                                    Set<com.collabsphere_first.community.enums.CommunityCategory> categories,
                                    Set<String> tags) {
        int score = categories.contains(community.getCategory()) ? 3 : 0;
        if (community.getTags() != null) score += (int) community.getTags().stream()
                .map(String::toLowerCase).filter(tags::contains).count();
        return score;
    }

    @Override
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void recalculateActivityScores() {
        log.info("Starting scheduled community activity score recalculation");
        List<Community> communities = communityRepository.findAll();
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);

        for (Community community : communities) {
            long recentPosts = postRepository.countByCommunityIdAndCreatedAtAfter(community.getId(), twentyFourHoursAgo);
            double currentScore = community.getActivityScore() != null ? community.getActivityScore().doubleValue() : 0.0;
            double updatedScore = (currentScore * activityDecayFactor) + (recentPosts * 1.5);
            community.setActivityScore(BigDecimal.valueOf(updatedScore).setScale(4, java.math.RoundingMode.HALF_UP));
        }

        communityRepository.saveAll(communities);
        log.info("Finished recalculated activity scores for {} communities", communities.size());
    }
}
