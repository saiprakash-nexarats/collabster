package com.collabsphere_first.interaction.adapter;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.interaction.dto.LikeResponse;
import com.collabsphere_first.interaction.dto.SaveResponse;
import com.collabsphere_first.interaction.entity.ContentLike;
import com.collabsphere_first.interaction.entity.ContentSave;
import com.collabsphere_first.interaction.repository.ContentLikeRepository;
import com.collabsphere_first.interaction.repository.ContentSaveRepository;
import com.collabsphere_first.interaction.resolver.ContentResolutionService;
import com.collabsphere_first.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Component
public class PlatformPostInteractionAdapter implements ContentInteractionAdapter {

    private final ContentLikeRepository contentLikeRepository;
    private final ContentSaveRepository contentSaveRepository;
    private final ContentResolutionService contentResolutionService;
    private final Optional<com.collabsphere_first.recommendation.feedback.service.RecommendationFeedbackOutboxService> feedbackOutboxService;

    @org.springframework.beans.factory.annotation.Autowired
    public PlatformPostInteractionAdapter(
            ContentLikeRepository contentLikeRepository,
            ContentSaveRepository contentSaveRepository,
            ContentResolutionService contentResolutionService,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            com.collabsphere_first.recommendation.feedback.service.RecommendationFeedbackOutboxService feedbackOutboxService
    ) {
        this.contentLikeRepository = contentLikeRepository;
        this.contentSaveRepository = contentSaveRepository;
        this.contentResolutionService = contentResolutionService;
        this.feedbackOutboxService = Optional.ofNullable(feedbackOutboxService);
    }

    public PlatformPostInteractionAdapter(
            ContentLikeRepository contentLikeRepository,
            ContentSaveRepository contentSaveRepository,
            ContentResolutionService contentResolutionService
    ) {
        this(contentLikeRepository, contentSaveRepository, contentResolutionService, null);
    }

    @Override
    public ContentType supports() {
        return ContentType.PLATFORM_POST;
    }

    @Override
    @Transactional
    public LikeResponse like(User user, UUID contentId) {
        ContentRef ref = new ContentRef(ContentType.PLATFORM_POST, contentId);
        contentResolutionService.resolveVisible(ref, user);

        if (!contentLikeRepository.existsByUserIdAndContentTypeAndContentId(user.getId(), ContentType.PLATFORM_POST, contentId)) {
            ContentLike like = ContentLike.builder()
                    .user(user)
                    .contentType(ContentType.PLATFORM_POST)
                    .contentId(contentId)
                    .build();
            contentLikeRepository.save(like);
            log.info("User {} liked platform post {}", user.getId(), contentId);

            feedbackOutboxService.ifPresent(service -> service.recordFeedback(
                    com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType.LIKE,
                    user.getId(),
                    ref,
                    java.time.Instant.now(),
                    com.collabsphere_first.recommendation.feedback.enums.FeedbackSurface.UNKNOWN,
                    null,
                    null
            ));
        }

        long count = contentLikeRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, contentId);
        return LikeResponse.builder()
                .contentRef(ref)
                .liked(true)
                .likeCount(count)
                .build();
    }

    @Override
    @Transactional
    public LikeResponse unlike(User user, UUID contentId) {
        ContentRef ref = new ContentRef(ContentType.PLATFORM_POST, contentId);
        boolean existed = contentLikeRepository.existsByUserIdAndContentTypeAndContentId(user.getId(), ContentType.PLATFORM_POST, contentId);
        contentLikeRepository.deleteByUserIdAndContentTypeAndContentId(user.getId(), ContentType.PLATFORM_POST, contentId);
        log.info("User {} unliked platform post {}", user.getId(), contentId);

        if (existed) {
            feedbackOutboxService.ifPresent(service -> service.recordFeedback(
                    com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType.UNLIKE,
                    user.getId(),
                    ref,
                    java.time.Instant.now(),
                    com.collabsphere_first.recommendation.feedback.enums.FeedbackSurface.UNKNOWN,
                    null,
                    null
            ));
        }

        long count = contentLikeRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, contentId);
        return LikeResponse.builder()
                .contentRef(ref)
                .liked(false)
                .likeCount(count)
                .build();
    }

    @Override
    @Transactional
    public SaveResponse save(User user, UUID contentId) {
        ContentRef ref = new ContentRef(ContentType.PLATFORM_POST, contentId);
        contentResolutionService.resolveVisible(ref, user);

        if (!contentSaveRepository.existsByUserIdAndContentTypeAndContentId(user.getId(), ContentType.PLATFORM_POST, contentId)) {
            ContentSave save = ContentSave.builder()
                    .user(user)
                    .contentType(ContentType.PLATFORM_POST)
                    .contentId(contentId)
                    .build();
            contentSaveRepository.save(save);
            log.info("User {} saved platform post {}", user.getId(), contentId);

            feedbackOutboxService.ifPresent(service -> service.recordFeedback(
                    com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType.SAVE,
                    user.getId(),
                    ref,
                    java.time.Instant.now(),
                    com.collabsphere_first.recommendation.feedback.enums.FeedbackSurface.UNKNOWN,
                    null,
                    null
            ));
        }

        long count = contentSaveRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, contentId);
        return SaveResponse.builder()
                .contentRef(ref)
                .saved(true)
                .saveCount(count)
                .build();
    }

    @Override
    @Transactional
    public SaveResponse unsave(User user, UUID contentId) {
        ContentRef ref = new ContentRef(ContentType.PLATFORM_POST, contentId);
        boolean existed = contentSaveRepository.existsByUserIdAndContentTypeAndContentId(user.getId(), ContentType.PLATFORM_POST, contentId);
        contentSaveRepository.deleteByUserIdAndContentTypeAndContentId(user.getId(), ContentType.PLATFORM_POST, contentId);
        log.info("User {} unsaved platform post {}", user.getId(), contentId);

        if (existed) {
            feedbackOutboxService.ifPresent(service -> service.recordFeedback(
                    com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType.UNSAVE,
                    user.getId(),
                    ref,
                    java.time.Instant.now(),
                    com.collabsphere_first.recommendation.feedback.enums.FeedbackSurface.UNKNOWN,
                    null,
                    null
            ));
        }

        long count = contentSaveRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, contentId);
        return SaveResponse.builder()
                .contentRef(ref)
                .saved(false)
                .saveCount(count)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> getLikeCounts(Collection<UUID> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = contentLikeRepository.countLikesByContentTypeAndIds(ContentType.PLATFORM_POST, contentIds);
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            UUID id = (UUID) row[1];
            Long count = (Long) row[2];
            counts.put(id, count != null ? count : 0L);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> getSaveCounts(Collection<UUID> contentIds) {
        // Can be computed if needed; default empty
        return Collections.emptyMap();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getLikedContentIds(Long userId, Collection<UUID> contentIds) {
        if (userId == null || contentIds == null || contentIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Object[]> rows = contentLikeRepository.findLikedContent(userId, ContentType.PLATFORM_POST, contentIds);
        Set<UUID> liked = new HashSet<>();
        for (Object[] row : rows) {
            liked.add((UUID) row[1]);
        }
        return liked;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getSavedContentIds(Long userId, Collection<UUID> contentIds) {
        if (userId == null || contentIds == null || contentIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Object[]> rows = contentSaveRepository.findSavedContent(userId, ContentType.PLATFORM_POST, contentIds);
        Set<UUID> saved = new HashSet<>();
        for (Object[] row : rows) {
            saved.add((UUID) row[1]);
        }
        return saved;
    }
}
