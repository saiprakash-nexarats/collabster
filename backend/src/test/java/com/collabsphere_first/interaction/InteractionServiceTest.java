package com.collabsphere_first.interaction;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.interaction.dto.*;
import com.collabsphere_first.interaction.entity.ContentInteractionEvent;
import com.collabsphere_first.interaction.entity.ContentLike;
import com.collabsphere_first.interaction.entity.ContentSave;
import com.collabsphere_first.interaction.enums.InteractionEventType;
import com.collabsphere_first.interaction.exception.ContentInaccessibleException;
import com.collabsphere_first.interaction.exception.InvalidInteractionException;
import com.collabsphere_first.interaction.repository.ContentInteractionEventRepository;
import com.collabsphere_first.interaction.repository.ContentLikeRepository;
import com.collabsphere_first.interaction.repository.ContentSaveRepository;
import com.collabsphere_first.interaction.resolver.ContentResolutionService;
import com.collabsphere_first.interaction.resolver.ResolvedContent;
import com.collabsphere_first.interaction.service.InteractionServiceImpl;
import com.collabsphere_first.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InteractionService Tests")
class InteractionServiceTest {

    @Mock
    private ContentLikeRepository contentLikeRepository;

    @Mock
    private ContentSaveRepository contentSaveRepository;

    @Mock
    private ContentInteractionEventRepository contentInteractionEventRepository;

    @Mock
    private ContentResolutionService contentResolutionService;

    @InjectMocks
    private InteractionServiceImpl interactionService;

    private User user;
    private UUID platformPostId;
    private UUID communityPostId;
    private ContentRef platformRef;
    private ContentRef communityRef;

    @BeforeEach
    void setUp() {
        user = User.builder().id(10L).firstName("Test").lastName("User").username("testuser").build();
        platformPostId = UUID.randomUUID();
        communityPostId = UUID.randomUUID();
        platformRef = ContentRef.platformPost(platformPostId);
        communityRef = ContentRef.communityPost(communityPostId);

        interactionService = new InteractionServiceImpl(
                contentLikeRepository,
                contentSaveRepository,
                contentInteractionEventRepository,
                contentResolutionService,
                new com.collabsphere_first.interaction.adapter.ContentInteractionAdapterRegistry(List.of(
                        new com.collabsphere_first.interaction.adapter.PlatformPostInteractionAdapter(contentLikeRepository, contentSaveRepository, contentResolutionService),
                        new com.collabsphere_first.interaction.adapter.CommunityPostInteractionAdapter(contentLikeRepository, contentSaveRepository, contentResolutionService)
                ))
        );
    }

    // ==========================================
    // LIKE TESTS
    // ==========================================
    @Nested
    @DisplayName("Like & Unlike Tests")
    class LikeTests {

        @Test
        @DisplayName("Successfully like a PlatformPost")
        void likeContent_PlatformPost_Success() {
            ResolvedContent resolved = ResolvedContent.of(platformRef, 20L, null, "Platform post summary");
            when(contentResolutionService.resolveVisible(platformRef, user)).thenReturn(resolved);
            when(contentLikeRepository.existsByUserIdAndContentTypeAndContentId(10L, ContentType.PLATFORM_POST, platformPostId)).thenReturn(false);
            when(contentLikeRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, platformPostId)).thenReturn(5L);

            LikeResponse response = interactionService.likeContent(user, platformRef);

            assertNotNull(response);
            assertTrue(response.isLiked());
            assertEquals(5L, response.getLikeCount());
            verify(contentLikeRepository).save(any(ContentLike.class));
        }

        @Test
        @DisplayName("Successfully like a CommunityPost")
        void likeContent_CommunityPost_Success() {
            ResolvedContent resolved = ResolvedContent.of(communityRef, 30L, "Community Title", "Community post body");
            when(contentResolutionService.resolveVisible(communityRef, user)).thenReturn(resolved);
            when(contentLikeRepository.existsByUserIdAndContentTypeAndContentId(10L, ContentType.COMMUNITY_POST, communityPostId)).thenReturn(false);
            when(contentLikeRepository.countByContentTypeAndContentId(ContentType.COMMUNITY_POST, communityPostId)).thenReturn(12L);

            LikeResponse response = interactionService.likeContent(user, communityRef);

            assertNotNull(response);
            assertTrue(response.isLiked());
            assertEquals(12L, response.getLikeCount());
            verify(contentLikeRepository).save(any(ContentLike.class));
        }

        @Test
        @DisplayName("Duplicate like is idempotent")
        void likeContent_Duplicate_Idempotent() {
            ResolvedContent resolved = ResolvedContent.of(platformRef, 20L, null, "Summary");
            when(contentResolutionService.resolveVisible(platformRef, user)).thenReturn(resolved);
            when(contentLikeRepository.existsByUserIdAndContentTypeAndContentId(10L, ContentType.PLATFORM_POST, platformPostId)).thenReturn(true);
            when(contentLikeRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, platformPostId)).thenReturn(5L);

            LikeResponse response = interactionService.likeContent(user, platformRef);

            assertNotNull(response);
            assertTrue(response.isLiked());
            verify(contentLikeRepository, never()).save(any(ContentLike.class));
        }

        @Test
        @DisplayName("Like inaccessible content throws ContentInaccessibleException")
        void likeContent_Inaccessible_ThrowsException() {
            when(contentResolutionService.resolveVisible(platformRef, user))
                    .thenThrow(new ContentInaccessibleException("Content not accessible"));

            assertThrows(ContentInaccessibleException.class, () -> interactionService.likeContent(user, platformRef));
            verify(contentLikeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Unlike content removes record and returns updated count")
        void unlikeContent_Success() {
            when(contentLikeRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, platformPostId)).thenReturn(4L);

            LikeResponse response = interactionService.unlikeContent(user, platformRef);

            assertNotNull(response);
            assertFalse(response.isLiked());
            assertEquals(4L, response.getLikeCount());
            verify(contentLikeRepository).deleteByUserIdAndContentTypeAndContentId(10L, ContentType.PLATFORM_POST, platformPostId);
        }
    }

    // ==========================================
    // SAVE TESTS
    // ==========================================
    @Nested
    @DisplayName("Save & Unsave Tests")
    class SaveTests {

        @Test
        @DisplayName("Save content adds bookmark")
        void saveContent_Success() {
            ResolvedContent resolved = ResolvedContent.of(platformRef, 20L, null, "Platform post summary");
            when(contentResolutionService.resolveVisible(platformRef, user)).thenReturn(resolved);
            when(contentSaveRepository.existsByUserIdAndContentTypeAndContentId(10L, ContentType.PLATFORM_POST, platformPostId)).thenReturn(false);
            when(contentSaveRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, platformPostId)).thenReturn(3L);

            SaveResponse response = interactionService.saveContent(user, platformRef);

            assertNotNull(response);
            assertTrue(response.isSaved());
            assertEquals(3L, response.getSaveCount());
            verify(contentSaveRepository).save(any(ContentSave.class));
        }

        @Test
        @DisplayName("Duplicate save is idempotent")
        void saveContent_Duplicate_Idempotent() {
            ResolvedContent resolved = ResolvedContent.of(platformRef, 20L, null, "Summary");
            when(contentResolutionService.resolveVisible(platformRef, user)).thenReturn(resolved);
            when(contentSaveRepository.existsByUserIdAndContentTypeAndContentId(10L, ContentType.PLATFORM_POST, platformPostId)).thenReturn(true);
            when(contentSaveRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, platformPostId)).thenReturn(3L);

            SaveResponse response = interactionService.saveContent(user, platformRef);

            assertNotNull(response);
            assertTrue(response.isSaved());
            verify(contentSaveRepository, never()).save(any());
        }

        @Test
        @DisplayName("Unsave content removes bookmark")
        void unsaveContent_Success() {
            when(contentSaveRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, platformPostId)).thenReturn(2L);

            SaveResponse response = interactionService.unsaveContent(user, platformRef);

            assertNotNull(response);
            assertFalse(response.isSaved());
            verify(contentSaveRepository).deleteByUserIdAndContentTypeAndContentId(10L, ContentType.PLATFORM_POST, platformPostId);
        }

        @Test
        @DisplayName("Get saved content returns paginated items with hydration")
        void getSavedContent_Success() {
            ContentSave s1 = ContentSave.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .contentType(ContentType.PLATFORM_POST)
                    .contentId(platformPostId)
                    .createdAt(Instant.now())
                    .build();
            when(contentSaveRepository.findInitialSavedPosts(eq(10L), any(Pageable.class))).thenReturn(List.of(s1));
            when(contentResolutionService.resolveVisible(any(), eq(user)))
                    .thenReturn(ResolvedContent.of(platformRef, 20L, null, "Summary"));

            SavedItemsCursorPageResponse response = interactionService.getSavedContent(user, null, 20);

            assertNotNull(response);
            assertEquals(1, response.getItems().size());
            assertEquals(platformRef, response.getItems().get(0).getContentRef());
        }
    }

    // ==========================================
    // INTERACTION EVENTS
    // ==========================================
    @Nested
    @DisplayName("Interaction Events & Telemetry Tests")
    class EventTests {

        @Test
        @DisplayName("Record SHARE event")
        void recordShare_Success() {
            when(contentResolutionService.resolveVisible(platformRef, user))
                    .thenReturn(ResolvedContent.of(platformRef, 20L, null, "Summary"));

            interactionService.recordShare(user, platformRef);

            verify(contentInteractionEventRepository).save(any(ContentInteractionEvent.class));
        }

        @Test
        @DisplayName("Record VIEW event with deduplication")
        void recordView_Deduplication_SkipsDuplicate() {
            InteractionEventRequest req = InteractionEventRequest.builder()
                    .contentRef(platformRef)
                    .eventType(InteractionEventType.VIEW)
                    .sessionId("sess-1")
                    .build();

            when(contentResolutionService.resolveVisible(platformRef, user))
                    .thenReturn(ResolvedContent.of(platformRef, 20L, null, "Summary"));
            when(contentInteractionEventRepository.existsRecentView(eq(10L), eq(ContentType.PLATFORM_POST), eq(platformPostId), any(Instant.class)))
                    .thenReturn(true);

            interactionService.recordEvent(user, req);

            verify(contentInteractionEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Record WATCH_TIME event with valid duration")
        void recordWatchTime_Success() {
            InteractionEventRequest req = InteractionEventRequest.builder()
                    .contentRef(platformRef)
                    .eventType(InteractionEventType.WATCH_TIME)
                    .durationMs(4500L)
                    .build();

            when(contentResolutionService.resolveVisible(platformRef, user))
                    .thenReturn(ResolvedContent.of(platformRef, 20L, null, "Summary"));

            interactionService.recordEvent(user, req);

            verify(contentInteractionEventRepository).save(any(ContentInteractionEvent.class));
        }

        @Test
        @DisplayName("Reject WATCH_TIME event with negative duration")
        void recordWatchTime_NegativeDuration_ThrowsException() {
            InteractionEventRequest req = InteractionEventRequest.builder()
                    .contentRef(platformRef)
                    .eventType(InteractionEventType.WATCH_TIME)
                    .durationMs(-500L)
                    .build();

            assertThrows(InvalidInteractionException.class, () -> interactionService.recordEvent(user, req));
            verify(contentInteractionEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Reject WATCH_TIME event with duration exceeding 24 hours")
        void recordWatchTime_ExcessiveDuration_ThrowsException() {
            InteractionEventRequest req = InteractionEventRequest.builder()
                    .contentRef(platformRef)
                    .eventType(InteractionEventType.WATCH_TIME)
                    .durationMs(90_000_000L)
                    .build();

            assertThrows(InvalidInteractionException.class, () -> interactionService.recordEvent(user, req));
            verify(contentInteractionEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Record HIDE event")
        void recordHide_Success() {
            InteractionEventRequest req = InteractionEventRequest.builder()
                    .contentRef(platformRef)
                    .eventType(InteractionEventType.HIDE)
                    .build();

            when(contentResolutionService.resolveVisible(platformRef, user))
                    .thenReturn(ResolvedContent.of(platformRef, 20L, null, "Summary"));

            interactionService.recordEvent(user, req);

            verify(contentInteractionEventRepository).save(any(ContentInteractionEvent.class));
        }

        @Test
        @DisplayName("Record NOT_INTERESTED event")
        void recordNotInterested_Success() {
            InteractionEventRequest req = InteractionEventRequest.builder()
                    .contentRef(platformRef)
                    .eventType(InteractionEventType.NOT_INTERESTED)
                    .build();

            when(contentResolutionService.resolveVisible(platformRef, user))
                    .thenReturn(ResolvedContent.of(platformRef, 20L, null, "Summary"));

            interactionService.recordEvent(user, req);

            verify(contentInteractionEventRepository).save(any(ContentInteractionEvent.class));
        }

        @Test
        @DisplayName("Record batch events up to 50 items")
        void recordBatchEvents_Success() {
            List<InteractionEventRequest> events = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                events.add(InteractionEventRequest.builder()
                        .contentRef(platformRef)
                        .eventType(InteractionEventType.VIEW)
                        .build());
            }

            when(contentResolutionService.resolveVisible(platformRef, user))
                    .thenReturn(ResolvedContent.of(platformRef, 20L, null, "Summary"));
            when(contentInteractionEventRepository.existsRecentView(eq(10L), any(), any(), any()))
                    .thenReturn(false);

            int recorded = interactionService.recordBatchEvents(user, events);

            assertEquals(5, recorded);
            verify(contentInteractionEventRepository, times(5)).save(any(ContentInteractionEvent.class));
        }

        @Test
        @DisplayName("Batch events exceeding 50 items is rejected")
        void recordBatchEvents_Exceeding50_ThrowsException() {
            List<InteractionEventRequest> events = new ArrayList<>();
            for (int i = 0; i < 51; i++) {
                events.add(InteractionEventRequest.builder()
                        .contentRef(platformRef)
                        .eventType(InteractionEventType.VIEW)
                        .build());
            }

            assertThrows(InvalidInteractionException.class, () -> interactionService.recordBatchEvents(user, events));
            verify(contentInteractionEventRepository, never()).save(any());
        }
    }

    // ==========================================
    // STATUS & COUNTS
    // ==========================================
    @Nested
    @DisplayName("Status & Counts Tests")
    class StatusTests {

        @Test
        @DisplayName("Query like and save counts")
        void counts_Success() {
            when(contentLikeRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, platformPostId)).thenReturn(42L);
            when(contentSaveRepository.countByContentTypeAndContentId(ContentType.PLATFORM_POST, platformPostId)).thenReturn(17L);

            assertEquals(42L, interactionService.getLikeCount(platformRef));
            assertEquals(17L, interactionService.getSaveCount(platformRef));
        }
    }
}
