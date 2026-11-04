package com.collabsphere_first.community.service.impl;

import com.collabsphere_first.community.dto.CommunityFeedCandidate;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.repository.PostRepository;
import com.collabsphere_first.community.service.CommunityFeedReadService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityFeedReadServiceImpl implements CommunityFeedReadService {

    private final MembershipRepository membershipRepository;
    private final PostRepository postRepository;
    private final com.collabsphere_first.community.service.CommunityContentAccessService communityContentAccessService;

    @Override
    public List<CommunityFeedCandidate> findRecentEligiblePosts(User currentUser, int limit) {
        if (currentUser == null || limit <= 0) {
            return Collections.emptyList();
        }

        List<Membership> memberships = membershipRepository.findByUserId(currentUser.getId());
        Set<UUID> activeCommunityIds = memberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .map(m -> m.getCommunity().getId())
                .collect(Collectors.toSet());

        if (activeCommunityIds.isEmpty()) {
            return Collections.emptyList();
        }

        int boundedLimit = Math.min(limit, 500);
        List<Post> posts = postRepository.findRecentApprovedPostsByCommunities(
                activeCommunityIds,
                ModerationStatus.VISIBLE,
                PageRequest.of(0, boundedLimit)
        );

        List<CommunityFeedCandidate> candidates = new ArrayList<>();
        for (Post p : posts) {
            candidates.add(new CommunityFeedCandidate(
                    p.getId(),
                    p.getAuthorId(),
                    p.getCreatedAt(),
                    p.getCommunity().getId(),
                    p.getCommunity().getName(),
                    p.getCommunity().getSlug()
            ));
        }
        return candidates;
    }

    @Override
    public Map<UUID, PostDto> batchGetEligiblePosts(Collection<UUID> postIds, User currentUser) {
        if (postIds == null || postIds.isEmpty() || currentUser == null) {
            return Collections.emptyMap();
        }
        return communityContentAccessService.getVisiblePosts(postIds, currentUser.getId());
    }
}
