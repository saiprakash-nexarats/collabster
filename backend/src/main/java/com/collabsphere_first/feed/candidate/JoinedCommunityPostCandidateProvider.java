package com.collabsphere_first.feed.candidate;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.community.dto.CommunityFeedCandidate;
import com.collabsphere_first.community.service.CommunityFeedReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JoinedCommunityPostCandidateProvider implements FeedCandidateProvider {

    private final CommunityFeedReadService communityFeedReadService;

    @Override
    public FeedSourceType sourceType() {
        return FeedSourceType.JOINED_COMMUNITY;
    }

    @Override
    public List<FeedCandidate> collect(FeedCandidateContext context) {
        if (context.viewer() == null) {
            return Collections.emptyList();
        }

        int limit = context.feedProperties().getCandidates().getCommunityLimit();
        List<CommunityFeedCandidate> rawCandidates = communityFeedReadService.findRecentEligiblePosts(
                context.viewer(),
                limit
        );

        List<FeedCandidate> candidates = new ArrayList<>();
        for (CommunityFeedCandidate raw : rawCandidates) {
            // Exclude content authored by blocked or muted users
            if (raw.authorId() != null) {
                if (context.blockedUserIds().contains(raw.authorId()) || context.mutedUserIds().contains(raw.authorId())) {
                    continue;
                }
            }
            candidates.add(new FeedCandidate(
                    ContentRef.communityPost(raw.postId()),
                    raw.authorId(),
                    raw.createdAt(),
                    new HashSet<>(Collections.singleton(FeedSourceType.JOINED_COMMUNITY))
            ));
        }
        return candidates;
    }
}
