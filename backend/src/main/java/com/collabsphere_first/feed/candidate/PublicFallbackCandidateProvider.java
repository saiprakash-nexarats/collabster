package com.collabsphere_first.feed.candidate;

import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.service.PlatformPostFeedReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicFallbackCandidateProvider implements FeedCandidateProvider {

    private final PlatformPostFeedReadService platformPostFeedReadService;

    @Override
    public FeedSourceType sourceType() {
        return FeedSourceType.PUBLIC_FALLBACK;
    }

    @Override
    public List<FeedCandidate> collect(FeedCandidateContext context) {
        Set<Long> excludedAuthors = new HashSet<>();
        if (context.viewerId() != null) {
            excludedAuthors.add(context.viewerId());
        }
        excludedAuthors.addAll(context.blockedUserIds());
        excludedAuthors.addAll(context.mutedUserIds());

        int limit = context.feedProperties().getCandidates().getPublicFallbackLimit();
        List<PlatformPost> posts = platformPostFeedReadService.findCandidatePublicPosts(
                excludedAuthors,
                limit
        );

        List<FeedCandidate> candidates = new ArrayList<>();
        for (PlatformPost post : posts) {
            candidates.add(new FeedCandidate(
                    post.toContentRef(),
                    post.getAuthor().getId(),
                    post.getCreatedAt(),
                    new HashSet<>(Collections.singleton(FeedSourceType.PUBLIC_FALLBACK))
            ));
        }
        return candidates;
    }
}
