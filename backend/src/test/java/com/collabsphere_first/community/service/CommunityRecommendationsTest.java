package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CommunityDto;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.CommunityCategory;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.mapper.CommunityMapper;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.service.impl.DiscoveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityRecommendationsTest {

    @Mock private CommunityRepository communityRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private CommunityMapper communityMapper;

    @InjectMocks
    private DiscoveryServiceImpl discoveryService;

    private Long userId = 42L;
    private Community commTech;
    private Community commDesign;
    private Community commJoined;

    @BeforeEach
    void setUp() {
        commTech = Community.builder()
                .id(UUID.randomUUID())
                .name("Tech Pioneers")
                .slug("tech-pioneers")
                .category(CommunityCategory.TECHNOLOGY)
                .tags(List.of("java", "spring", "backend"))
                .visibility(Visibility.PUBLIC)
                .communityStatus(CommunityStatus.ACTIVE)
                .activityScore(java.math.BigDecimal.valueOf(80))
                .memberCount(100)
                .createdAt(Instant.now())
                .build();

        commDesign = Community.builder()
                .id(UUID.randomUUID())
                .name("UI Designers")
                .slug("ui-designers")
                .category(CommunityCategory.DESIGN)
                .tags(List.of("figma", "ui"))
                .visibility(Visibility.PUBLIC)
                .communityStatus(CommunityStatus.ACTIVE)
                .activityScore(java.math.BigDecimal.valueOf(40))
                .memberCount(50)
                .createdAt(Instant.now())
                .build();

        commJoined = Community.builder()
                .id(UUID.randomUUID())
                .name("Existing Joined Club")
                .slug("existing-joined-club")
                .category(CommunityCategory.BUSINESS)
                .tags(List.of("business"))
                .visibility(Visibility.PUBLIC)
                .communityStatus(CommunityStatus.ACTIVE)
                .activityScore(java.math.BigDecimal.valueOf(95))
                .memberCount(200)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Recommendations exclude communities where user is already an active member")
    void recommendations_excludeJoinedCommunities() {
        Membership existingMembership = Membership.builder()
                .community(commJoined)
                .userId(userId)
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();

        when(membershipRepository.findByUserId(userId))
                .thenReturn(List.of(existingMembership));

        when(communityRepository.findByVisibilityAndCommunityStatus(
                any(Visibility.class), any(CommunityStatus.class), any()))
                .thenReturn(new PageImpl<>(List.of(commTech, commDesign, commJoined)));

        when(communityMapper.toDto(any(Community.class))).thenAnswer(i -> {
            Community c = i.getArgument(0);
            return CommunityDto.builder().id(c.getId()).name(c.getName()).build();
        });

        Page<CommunityDto> recs = discoveryService.recommendCommunities(userId, 0, 10);

        assertThat(recs.getContent()).hasSize(2);
        assertThat(recs.getContent()).noneMatch(c -> c.getId().equals(commJoined.getId()));
    }

    @Test
    @DisplayName("Recommendations score higher when categories and tags overlap with user interests")
    void recommendations_rankByInterestOverlap() {
        Membership interestMembership = Membership.builder()
                .community(Community.builder()
                        .id(UUID.randomUUID())
                        .category(CommunityCategory.TECHNOLOGY)
                        .tags(List.of("backend", "cloud"))
                        .build())
                .userId(userId)
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();

        when(membershipRepository.findByUserId(userId))
                .thenReturn(List.of(interestMembership));

        when(communityRepository.findByVisibilityAndCommunityStatus(
                any(Visibility.class), any(CommunityStatus.class), any()))
                .thenReturn(new PageImpl<>(List.of(commDesign, commTech)));

        when(communityMapper.toDto(any(Community.class))).thenAnswer(i -> {
            Community c = i.getArgument(0);
            return CommunityDto.builder().id(c.getId()).name(c.getName()).build();
        });

        Page<CommunityDto> recs = discoveryService.recommendCommunities(userId, 0, 10);

        assertThat(recs.getContent()).hasSize(2);
        // commTech shares category ENGINEERING and tag 'backend' -> ranked first!
        assertThat(recs.getContent().get(0).getName()).isEqualTo("Tech Pioneers");
    }
}
