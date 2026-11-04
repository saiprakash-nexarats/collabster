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
public class SelfPostCandidateProvider implements FeedCandidateProvider {

    private final PlatformPostFeedReadService platformPostFeedReadService;

    @Override
    public FeedSourceType sourceType() {
        return FeedSourceType.SELF;
    }

    @Override
    public List<FeedCandidate> collect(FeedCandidateContext context) {
        Long viewerId = context.viewerId();
        if (viewerId == null) {
            return Collections.emptyList();
        }

        int limit = context.feedProperties().getCandidates().getSelfLimit();
        // Self feed includes PUBLIC and FOLLOWERS_ONLY posts; PRIVATE posts are excluded from Home Feed per architecture contract
        List<PlatformPost> posts = platformPostFeedReadService.findCandidatePostsByAuthors(
                List.of(viewerId),
                List.of(PostVisibility.PUBLIC, PostVisibility.FOLLOWERS_ONLY),
                limit
        );

        List<FeedCandidate> candidates = new ArrayList<>();
        for (PlatformPost post : posts) {
            candidates.add(new FeedCandidate(
                    post.toContentRef(),
                    post.getAuthor().getId(),
                    post.getCreatedAt(),
                    new HashSet<>(Collections.singleton(FeedSourceType.SELF))
            ));
        }
        return candidates;
    }
}
