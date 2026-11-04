package com.collabsphere_first.feed.candidate;

import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.service.PlatformPostFeedReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowingPostCandidateProvider implements FeedCandidateProvider {

    private final PlatformPostFeedReadService platformPostFeedReadService;

    @Override
    public FeedSourceType sourceType() {
        return FeedSourceType.FOLLOWING;
    }

    @Override
    public List<FeedCandidate> collect(FeedCandidateContext context) {
        Set<Long> followingIds = new HashSet<>(context.followingUserIds());
        if (followingIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Exclude blocked or muted users
        followingIds.removeAll(context.blockedUserIds());
        followingIds.removeAll(context.mutedUserIds());

        if (followingIds.isEmpty()) {
            return Collections.emptyList();
        }

        int limit = context.feedProperties().getCandidates().getFollowingLimit();
        List<PlatformPost> posts = platformPostFeedReadService.findCandidatePostsByAuthors(
                followingIds,
                List.of(PostVisibility.PUBLIC, PostVisibility.FOLLOWERS_ONLY),
                limit
        );

        List<FeedCandidate> candidates = new ArrayList<>();
        for (PlatformPost post : posts) {
            candidates.add(new FeedCandidate(
                    post.toContentRef(),
                    post.getAuthor().getId(),
                    post.getCreatedAt(),
                    new HashSet<>(Collections.singleton(FeedSourceType.FOLLOWING))
            ));
        }
        return candidates;
    }
}
