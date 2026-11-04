package com.collabsphere_first.interaction.service;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.pagination.CursorUtils;
import com.collabsphere_first.common.pagination.CursorUtils.CursorPayload;
import com.collabsphere_first.interaction.dto.*;
import com.collabsphere_first.interaction.entity.ContentInteractionEvent;
import com.collabsphere_first.interaction.entity.ContentSave;
import com.collabsphere_first.interaction.enums.InteractionEventType;
import com.collabsphere_first.interaction.exception.ContentInaccessibleException;
import com.collabsphere_first.interaction.exception.InvalidInteractionException;
import com.collabsphere_first.interaction.repository.ContentInteractionEventRepository;
import com.collabsphere_first.interaction.repository.ContentLikeRepository;
import com.collabsphere_first.interaction.repository.ContentSaveRepository;
import com.collabsphere_first.interaction.resolver.ContentResolutionService;
import com.collabsphere_first.interaction.resolver.ResolvedContent;
import com.collabsphere_first.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class InteractionServiceImpl implements InteractionService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_BATCH_EVENTS = 50;
    private static final long VIEW_DEDUPLICATION_WINDOW_SECONDS = 60L;
    private static final long MAX_WATCH_TIME_MS = 86_400_000L; // 24 hours

    private final ContentLikeRepository contentLikeRepository;
    private final ContentSaveRepository contentSaveRepository;
    private final ContentInteractionEventRepository contentInteractionEventRepository;
    private final ContentResolutionService contentResolutionService;
    private final com.collabsphere_first.interaction.adapter.ContentInteractionAdapterRegistry adapterRegistry;
    private final Optional<com.collabsphere_first.recommendation.feedback.service.RecommendationFeedbackOutboxService> feedbackOutboxService;

    @org.springframework.beans.factory.annotation.Autowired
    public InteractionServiceImpl(
            ContentLikeRepository contentLikeRepository,
            ContentSaveRepository contentSaveRepository,
            ContentInteractionEventRepository contentInteractionEventRepository,
            ContentResolutionService contentResolutionService,
            com.collabsphere_first.interaction.adapter.ContentInteractionAdapterRegistry adapterRegistry,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            com.collabsphere_first.recommendation.feedback.service.RecommendationFeedbackOutboxService feedbackOutboxService
    ) {
        this.contentLikeRepository = contentLikeRepository;
        this.contentSaveRepository = contentSaveRepository;
        this.contentInteractionEventRepository = contentInteractionEventRepository;
        this.contentResolutionService = contentResolutionService;
        this.adapterRegistry = adapterRegistry != null ? adapterRegistry : new com.collabsphere_first.interaction.adapter.ContentInteractionAdapterRegistry(List.of(
                new com.collabsphere_first.interaction.adapter.PlatformPostInteractionAdapter(contentLikeRepository, contentSaveRepository, contentResolutionService, feedbackOutboxService),
                new com.collabsphere_first.interaction.adapter.CommunityPostInteractionAdapter(contentLikeRepository, contentSaveRepository, contentResolutionService, feedbackOutboxService)
        ));
        this.feedbackOutboxService = Optional.ofNullable(feedbackOutboxService);
    }

    public InteractionServiceImpl(
            ContentLikeRepository contentLikeRepository,
            ContentSaveRepository contentSaveRepository,
            ContentInteractionEventRepository contentInteractionEventRepository,
            ContentResolutionService contentResolutionService,
            com.collabsphere_first.interaction.adapter.ContentInteractionAdapterRegistry adapterRegistry
    ) {
        this(
                contentLikeRepository,
                contentSaveRepository,
                contentInteractionEventRepository,
                contentResolutionService,
                adapterRegistry,
                null
        );
    }

    public InteractionServiceImpl(
            ContentLikeRepository contentLikeRepository,
            ContentSaveRepository contentSaveRepository,
            ContentInteractionEventRepository contentInteractionEventRepository,
            ContentResolutionService contentResolutionService
    ) {
        this(
                contentLikeRepository,
                contentSaveRepository,
                contentInteractionEventRepository,
                contentResolutionService,
                null,
                null
        );
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }

    @Override
    public LikeResponse likeContent(User user, ContentRef contentRef) {
        if (contentRef == null || contentRef.type() == null || contentRef.id() == null) {
            throw new InvalidInteractionException("ContentRef must contain non-null type and id");
        }
        com.collabsphere_first.interaction.adapter.ContentInteractionAdapter adapter = adapterRegistry.getAdapter(contentRef.type());
        return adapter.like(user, contentRef.id());
    }

    @Override
    public LikeResponse unlikeContent(User user, ContentRef contentRef) {
        if (contentRef == null || contentRef.type() == null || contentRef.id() == null) {
            throw new InvalidInteractionException("ContentRef must contain non-null type and id");
        }
        com.collabsphere_first.interaction.adapter.ContentInteractionAdapter adapter = adapterRegistry.getAdapter(contentRef.type());
        return adapter.unlike(user, contentRef.id());
    }

    @Override
    public SaveResponse saveContent(User user, ContentRef contentRef) {
        if (contentRef == null || contentRef.type() == null || contentRef.id() == null) {
            throw new InvalidInteractionException("ContentRef must contain non-null type and id");
        }
        com.collabsphere_first.interaction.adapter.ContentInteractionAdapter adapter = adapterRegistry.getAdapter(contentRef.type());
        return adapter.save(user, contentRef.id());
    }

    @Override
    public SaveResponse unsaveContent(User user, ContentRef contentRef) {
        if (contentRef == null || contentRef.type() == null || contentRef.id() == null) {
            throw new InvalidInteractionException("ContentRef must contain non-null type and id");
        }
        com.collabsphere_first.interaction.adapter.ContentInteractionAdapter adapter = adapterRegistry.getAdapter(contentRef.type());
        return adapter.unsave(user, contentRef.id());
    }

    @Override
    @Transactional(readOnly = true)
    public SavedItemsCursorPageResponse getSavedContent(User user, String cursor, int limit) {
        int pageSize = sanitizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        List<ContentSave> saves;
        if (cursor == null || cursor.isBlank()) {
            saves = contentSaveRepository.findInitialSavedPosts(user.getId(), pageRequest);
        } else {
            CursorPayload payload = CursorUtils.decode(cursor);
            saves = contentSaveRepository.findSavedPostsAfterCursor(user.getId(), payload.timestamp(), payload.uuid(), pageRequest);
        }

        boolean hasNext = saves.size() > pageSize;
        List<ContentSave> pagedList = hasNext ? saves.subList(0, pageSize) : saves;

        String nextCursor = null;
        if (hasNext && !pagedList.isEmpty()) {
            ContentSave last = pagedList.get(pagedList.size() - 1);
            nextCursor = CursorUtils.encode(last.getCreatedAt(), last.getId());
        }

        List<SavedItemResponse> items = new ArrayList<>();
        for (ContentSave save : pagedList) {
            ContentRef ref = new ContentRef(save.getContentType(), save.getContentId());
            try {
                ResolvedContent resolved = contentResolutionService.resolveVisible(ref, user);
                items.add(SavedItemResponse.builder()
                        .contentRef(ref)
                        .savedAt(save.getCreatedAt())
                        .title(resolved.title())
                        .summary(resolved.summary())
                        .authorId(resolved.authorId())
                        .build());
            } catch (Exception ex) {
                // If content was deleted or blocked after being saved, indicate unavailable state safely
                items.add(SavedItemResponse.builder()
                        .contentRef(ref)
                        .savedAt(save.getCreatedAt())
                        .title(null)
                        .summary("[Content unavailable]")
                        .authorId(null)
                        .build());
            }
        }

        return SavedItemsCursorPageResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .limit(pageSize)
                .build();
    }

    @Override
    public void recordShare(User user, ContentRef contentRef) {
        contentResolutionService.resolveVisible(contentRef, user);

        ContentInteractionEvent event = ContentInteractionEvent.builder()
                .user(user)
                .contentType(contentRef.type())
                .contentId(contentRef.id())
                .eventType(InteractionEventType.SHARE)
                .build();
        ContentInteractionEvent saved = contentInteractionEventRepository.save(event);
        log.info("User {} shared content {}", user.getId(), contentRef);

        feedbackOutboxService.ifPresent(service -> service.recordFeedbackWithEventId(
                saved.getId(),
                com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType.SHARE,
                user.getId(),
                contentRef,
                saved.getCreatedAt(),
                com.collabsphere_first.recommendation.feedback.enums.FeedbackSurface.UNKNOWN,
                null,
                null
        ));
    }

    @Override
    public void recordEvent(User user, InteractionEventRequest request) {
        if (request == null || request.getContentRef() == null || request.getEventType() == null) {
            throw new InvalidInteractionException("Event request must contain non-null contentRef and eventType");
        }

        ContentRef contentRef = request.getContentRef();

        // Validate duration bounds
        if (request.getEventType() == InteractionEventType.WATCH_TIME) {
            if (request.getDurationMs() == null || request.getDurationMs() < 0 || request.getDurationMs() > MAX_WATCH_TIME_MS) {
                throw new InvalidInteractionException("durationMs must be between 0 and " + MAX_WATCH_TIME_MS + " ms for WATCH_TIME events");
            }
        }

        // Validate content accessibility & block checks
        contentResolutionService.resolveVisible(contentRef, user);

        // Deduplication rule for VIEW events: Prevent mobile view-count inflation within 60 seconds
        if (request.getEventType() == InteractionEventType.VIEW) {
            Instant cutoff = Instant.now().minusSeconds(VIEW_DEDUPLICATION_WINDOW_SECONDS);
            if (contentInteractionEventRepository.existsRecentView(user.getId(), contentRef.type(), contentRef.id(), cutoff)) {
                log.debug("Deduplicated VIEW event for user {} on content {}", user.getId(), contentRef);
                return;
            }
        }

        ContentInteractionEvent event = ContentInteractionEvent.builder()
                .user(user)
                .contentType(contentRef.type())
                .contentId(contentRef.id())
                .eventType(request.getEventType())
                .durationMs(request.getDurationMs())
                .sessionId(request.getSessionId())
                .build();

        ContentInteractionEvent saved = contentInteractionEventRepository.save(event);
        log.debug("Recorded {} event for user {} on content {}", request.getEventType(), user.getId(), contentRef);

        feedbackOutboxService.ifPresent(service -> {
            com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType fbType = mapFeedbackEventType(request.getEventType());
            if (fbType != null) {
                service.recordFeedbackWithEventId(
                        saved.getId(),
                        fbType,
                        user.getId(),
                        contentRef,
                        saved.getCreatedAt(),
                        com.collabsphere_first.recommendation.feedback.enums.FeedbackSurface.UNKNOWN,
                        request.getSessionId(),
                        request.getDurationMs()
                );
            }
        });
    }

    private com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType mapFeedbackEventType(InteractionEventType eventType) {
        if (eventType == null) return null;
        return switch (eventType) {
            case VIEW -> com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType.VIEW;
            case SHARE -> com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType.SHARE;
            case WATCH_TIME -> com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType.WATCH_TIME;
            case HIDE -> com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType.HIDE;
            case NOT_INTERESTED -> com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType.NOT_INTERESTED;
        };
    }

    @Override
    public int recordBatchEvents(User user, List<InteractionEventRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }

        if (requests.size() > MAX_BATCH_EVENTS) {
            throw new InvalidInteractionException("Batch events cannot exceed " + MAX_BATCH_EVENTS + " items per request");
        }

        int recorded = 0;
        for (InteractionEventRequest request : requests) {
            try {
                recordEvent(user, request);
                recorded++;
            } catch (ContentInaccessibleException ex) {
                log.warn("Skipping inaccessible content in batch: {}", ex.getMessage());
            }
        }
        return recorded;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLiked(Long userId, ContentRef contentRef) {
        if (userId == null || contentRef == null) return false;
        return contentLikeRepository.existsByUserIdAndContentTypeAndContentId(userId, contentRef.type(), contentRef.id());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSaved(Long userId, ContentRef contentRef) {
        if (userId == null || contentRef == null) return false;
        return contentSaveRepository.existsByUserIdAndContentTypeAndContentId(userId, contentRef.type(), contentRef.id());
    }

    @Override
    @Transactional(readOnly = true)
    public long getLikeCount(ContentRef contentRef) {
        if (contentRef == null) return 0;
        return contentLikeRepository.countByContentTypeAndContentId(contentRef.type(), contentRef.id());
    }

    @Override
    @Transactional(readOnly = true)
    public long getSaveCount(ContentRef contentRef) {
        if (contentRef == null) return 0;
        return contentSaveRepository.countByContentTypeAndContentId(contentRef.type(), contentRef.id());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Set<ContentRef> findSuppressedContentRefs(Long userId) {
        if (userId == null) return java.util.Collections.emptySet();
        java.util.List<Object[]> rawList = contentInteractionEventRepository.findSuppressedContent(userId);
        java.util.Set<ContentRef> suppressed = new java.util.HashSet<>();
        for (Object[] row : rawList) {
            com.collabsphere_first.common.content.ContentType type = (com.collabsphere_first.common.content.ContentType) row[0];
            java.util.UUID id = (java.util.UUID) row[1];
            suppressed.add(new ContentRef(type, id));
        }
        return suppressed;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<ContentRef, InteractionSummary> getInteractionSummaries(Long userId, java.util.Collection<ContentRef> contentRefs) {
        if (contentRefs == null || contentRefs.isEmpty()) {
            return java.util.Collections.emptyMap();
        }

        java.util.Map<com.collabsphere_first.common.content.ContentType, java.util.Set<java.util.UUID>> idsByType = new java.util.HashMap<>();
        for (ContentRef ref : contentRefs) {
            if (ref != null && ref.type() != null && ref.id() != null) {
                idsByType.computeIfAbsent(ref.type(), k -> new java.util.HashSet<>()).add(ref.id());
            }
        }

        java.util.Map<ContentRef, Long> likeCounts = new java.util.HashMap<>();
        java.util.Set<ContentRef> userLiked = new java.util.HashSet<>();
        java.util.Set<ContentRef> userSaved = new java.util.HashSet<>();

        for (java.util.Map.Entry<com.collabsphere_first.common.content.ContentType, java.util.Set<java.util.UUID>> entry : idsByType.entrySet()) {
            com.collabsphere_first.common.content.ContentType type = entry.getKey();
            java.util.Set<java.util.UUID> ids = entry.getValue();
            com.collabsphere_first.interaction.adapter.ContentInteractionAdapter adapter = adapterRegistry.getAdapter(type);

            java.util.Map<java.util.UUID, Long> counts = adapter.getLikeCounts(ids);
            counts.forEach((id, count) -> likeCounts.put(new ContentRef(type, id), count));

            if (userId != null) {
                java.util.Set<java.util.UUID> likedIds = adapter.getLikedContentIds(userId, ids);
                likedIds.forEach(id -> userLiked.add(new ContentRef(type, id)));

                java.util.Set<java.util.UUID> savedIds = adapter.getSavedContentIds(userId, ids);
                savedIds.forEach(id -> userSaved.add(new ContentRef(type, id)));
            }
        }

        java.util.Map<ContentRef, InteractionSummary> result = new java.util.HashMap<>();
        for (ContentRef ref : contentRefs) {
            if (ref != null) {
                boolean liked = userLiked.contains(ref);
                boolean saved = userSaved.contains(ref);
                long count = likeCounts.getOrDefault(ref, 0L);
                result.put(ref, new InteractionSummary(liked, saved, count));
            }
        }
        return result;
    }
}
