package com.collabsphere_first.post;

import com.collabsphere_first.post.dto.request.CreatePlatformPostRequest;
import com.collabsphere_first.post.dto.request.PlatformPostMediaRequest;
import com.collabsphere_first.post.dto.request.UpdatePlatformPostRequest;
import com.collabsphere_first.post.dto.response.PlatformPostCursorPageResponse;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostMediaType;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.exception.InvalidPlatformPostException;
import com.collabsphere_first.post.exception.PlatformPostAccessDeniedException;
import com.collabsphere_first.post.exception.PlatformPostNotFoundException;
import com.collabsphere_first.post.mapper.PlatformPostMapper;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.post.service.impl.PlatformPostServiceImpl;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformPostService Tests")
class PlatformPostServiceTest {

    @Mock
    private PlatformPostRepository postRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private com.collabsphere_first.network.service.NetworkAccessService networkAccessService;

    @Spy
    private PlatformPostMapper postMapper = new PlatformPostMapper();

    @InjectMocks
    private PlatformPostServiceImpl postService;

    private User author;
    private User otherUser;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(100L)
                .username("testauthor")
                .email("author@collobster.com")
                .firstName("Alice")
                .lastName("Author")
                .build();

        otherUser = User.builder()
                .id(200L)
                .username("otheruser")
                .email("other@collobster.com")
                .firstName("Bob")
                .lastName("Builder")
                .build();
    }

    @Nested
    @DisplayName("createPost Tests & Visibility Security")
    class CreatePostTests {

        @Test
        @DisplayName("Successfully creates post with PUBLIC visibility")
        void createPost_Public_Success() {
            when(currentUserService.getCurrentUser()).thenReturn(author);

            CreatePlatformPostRequest request = CreatePlatformPostRequest.builder()
                    .body("Hello world from PlatformPost!")
                    .visibility(PostVisibility.PUBLIC)
                    .build();

            UUID generatedId = UUID.randomUUID();
            when(postRepository.save(any(PlatformPost.class))).thenAnswer(invocation -> {
                PlatformPost p = invocation.getArgument(0);
                p.setId(generatedId);
                p.setCreatedAt(Instant.now());
                p.setUpdatedAt(Instant.now());
                return p;
            });

            PlatformPostResponse response = postService.createPost(request);

            assertNotNull(response);
            assertEquals(generatedId, response.getId());
            assertEquals("Hello world from PlatformPost!", response.getBody());
            assertEquals(PostVisibility.PUBLIC, response.getVisibility());
            assertEquals(PostStatus.ACTIVE, response.getStatus());
            assertEquals(author.getId(), response.getAuthor().getId());

            ArgumentCaptor<PlatformPost> captor = ArgumentCaptor.forClass(PlatformPost.class);
            verify(postRepository).save(captor.capture());
            PlatformPost captured = captor.getValue();
            assertEquals(author, captured.getAuthor());
        }

        @Test
        @DisplayName("Successfully creates post with PRIVATE visibility")
        void createPost_Private_Success() {
            when(currentUserService.getCurrentUser()).thenReturn(author);

            CreatePlatformPostRequest request = CreatePlatformPostRequest.builder()
                    .body("My private journal entry")
                    .visibility(PostVisibility.PRIVATE)
                    .build();

            UUID generatedId = UUID.randomUUID();
            when(postRepository.save(any(PlatformPost.class))).thenAnswer(invocation -> {
                PlatformPost p = invocation.getArgument(0);
                p.setId(generatedId);
                p.setCreatedAt(Instant.now());
                p.setUpdatedAt(Instant.now());
                return p;
            });

            PlatformPostResponse response = postService.createPost(request);

            assertNotNull(response);
            assertEquals(PostVisibility.PRIVATE, response.getVisibility());
        }

        @Test
        @DisplayName("Allows createPost with FOLLOWERS_ONLY visibility")
        void createPost_FollowersOnly_Success() {
            when(currentUserService.getCurrentUser()).thenReturn(author);
            when(postRepository.save(any(PlatformPost.class))).thenAnswer(invocation -> {
                PlatformPost p = invocation.getArgument(0);
                p.setId(UUID.randomUUID());
                p.setCreatedAt(Instant.now());
                p.setUpdatedAt(Instant.now());
                return p;
            });

            CreatePlatformPostRequest request = CreatePlatformPostRequest.builder()
                    .body("Followers only post")
                    .visibility(PostVisibility.FOLLOWERS_ONLY)
                    .build();

            PlatformPostResponse response = postService.createPost(request);

            assertNotNull(response);
            assertEquals(PostVisibility.FOLLOWERS_ONLY, response.getVisibility());
            verify(postRepository).save(any(PlatformPost.class));
        }

        @Test
        @DisplayName("Rejects completely empty post")
        void createPost_RejectsEmpty() {
            when(currentUserService.getCurrentUser()).thenReturn(author);

            CreatePlatformPostRequest emptyReq = CreatePlatformPostRequest.builder()
                    .body("   ")
                    .media(Collections.emptyList())
                    .build();

            assertThrows(InvalidPlatformPostException.class, () -> postService.createPost(emptyReq));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects post with more than 10 media items")
        void createPost_RejectsExcessMedia() {
            when(currentUserService.getCurrentUser()).thenReturn(author);

            List<PlatformPostMediaRequest> mediaList = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                mediaList.add(PlatformPostMediaRequest.builder()
                        .mediaType(PostMediaType.IMAGE)
                        .storageKey("key_" + i + ".png")
                        .mimeType("image/png")
                        .displayOrder(i)
                        .build());
            }

            CreatePlatformPostRequest request = CreatePlatformPostRequest.builder()
                    .body("Valid body")
                    .media(mediaList)
                    .build();

            assertThrows(InvalidPlatformPostException.class, () -> postService.createPost(request));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects duplicate displayOrder in media attachments")
        void createPost_RejectsDuplicateDisplayOrder() {
            when(currentUserService.getCurrentUser()).thenReturn(author);

            PlatformPostMediaRequest m1 = PlatformPostMediaRequest.builder()
                    .mediaType(PostMediaType.IMAGE).storageKey("k1.png").mimeType("image/png").displayOrder(0).build();
            PlatformPostMediaRequest m2 = PlatformPostMediaRequest.builder()
                    .mediaType(PostMediaType.IMAGE).storageKey("k2.png").mimeType("image/png").displayOrder(0).build();

            CreatePlatformPostRequest request = CreatePlatformPostRequest.builder()
                    .body("Valid body")
                    .media(List.of(m1, m2))
                    .build();

            assertThrows(InvalidPlatformPostException.class, () -> postService.createPost(request));
        }

        @Test
        @DisplayName("Rejects path traversal and dangerous storage keys")
        void createPost_RejectsMaliciousStorageKeys() {
            when(currentUserService.getCurrentUser()).thenReturn(author);

            // Path traversal
            PlatformPostMediaRequest badKey1 = PlatformPostMediaRequest.builder()
                    .mediaType(PostMediaType.IMAGE).storageKey("../../secret.txt").mimeType("image/png").displayOrder(0).build();
            assertThrows(InvalidPlatformPostException.class, () -> postService.createPost(
                    CreatePlatformPostRequest.builder().body("test").media(List.of(badKey1)).build()));

            // Absolute path /etc/passwd
            PlatformPostMediaRequest badKey2 = PlatformPostMediaRequest.builder()
                    .mediaType(PostMediaType.IMAGE).storageKey("/etc/passwd").mimeType("image/png").displayOrder(0).build();
            assertThrows(InvalidPlatformPostException.class, () -> postService.createPost(
                    CreatePlatformPostRequest.builder().body("test").media(List.of(badKey2)).build()));

            // Windows absolute path C:\test.jpg
            PlatformPostMediaRequest badKey3 = PlatformPostMediaRequest.builder()
                    .mediaType(PostMediaType.IMAGE).storageKey("C:\\test.jpg").mimeType("image/png").displayOrder(0).build();
            assertThrows(InvalidPlatformPostException.class, () -> postService.createPost(
                    CreatePlatformPostRequest.builder().body("test").media(List.of(badKey3)).build()));
        }
    }

    @Nested
    @DisplayName("getPostById Tests & Security Invariants")
    class GetPostByIdTests {

        @Test
        @DisplayName("Allows anyone to retrieve a public active post")
        void getPostById_PublicPost_Success() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Public content")
                    .visibility(PostVisibility.PUBLIC)
                    .status(PostStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            PlatformPostResponse response = postService.getPostById(postId);

            assertNotNull(response);
            assertEquals(postId, response.getId());
            assertEquals("Public content", response.getBody());
        }

        @Test
        @DisplayName("Allows author to retrieve their own private post")
        void getPostById_PrivatePost_AuthorSuccess() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Private journal")
                    .visibility(PostVisibility.PRIVATE)
                    .status(PostStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(author);

            PlatformPostResponse response = postService.getPostById(postId);

            assertNotNull(response);
            assertEquals(postId, response.getId());
        }

        @Test
        @DisplayName("Denies non-author access to a private post (IDOR protection)")
        void getPostById_PrivatePost_ForbiddenForOther() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Private journal")
                    .visibility(PostVisibility.PRIVATE)
                    .status(PostStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser);

            assertThrows(PlatformPostAccessDeniedException.class, () -> postService.getPostById(postId));
        }

        @Test
        @DisplayName("FOLLOWERS_ONLY post can be viewed by author")
        void getPostById_FollowersOnly_AuthorAllowed() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Followers post")
                    .visibility(PostVisibility.FOLLOWERS_ONLY)
                    .status(PostStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(author);

            PlatformPostResponse response = postService.getPostById(postId);
            assertNotNull(response);
            assertEquals(postId, response.getId());
        }

        @Test
        @DisplayName("FOLLOWERS_ONLY post can be viewed by follower who is not blocked")
        void getPostById_FollowersOnly_FollowerAllowed() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Followers post")
                    .visibility(PostVisibility.FOLLOWERS_ONLY)
                    .status(PostStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser);
            when(networkAccessService.isBlockedEitherDirection(otherUser.getId(), author.getId())).thenReturn(false);
            when(networkAccessService.isFollowing(otherUser.getId(), author.getId())).thenReturn(true);

            PlatformPostResponse response = postService.getPostById(postId);
            assertNotNull(response);
            assertEquals(postId, response.getId());
        }

        @Test
        @DisplayName("FOLLOWERS_ONLY post rejected with 403 for non-follower")
        void getPostById_FollowersOnly_NonFollowerForbidden() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Followers post")
                    .visibility(PostVisibility.FOLLOWERS_ONLY)
                    .status(PostStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser);
            when(networkAccessService.isBlockedEitherDirection(otherUser.getId(), author.getId())).thenReturn(false);
            when(networkAccessService.isFollowing(otherUser.getId(), author.getId())).thenReturn(false);

            PlatformPostAccessDeniedException ex = assertThrows(PlatformPostAccessDeniedException.class,
                    () -> postService.getPostById(postId));
            assertEquals("Only followers can view this post.", ex.getMessage());
        }

        @Test
        @DisplayName("FOLLOWERS_ONLY post rejected with 403 for blocked follower")
        void getPostById_FollowersOnly_BlockedForbidden() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Followers post")
                    .visibility(PostVisibility.FOLLOWERS_ONLY)
                    .status(PostStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser);
            when(networkAccessService.isBlockedEitherDirection(otherUser.getId(), author.getId())).thenReturn(true);

            PlatformPostAccessDeniedException ex = assertThrows(PlatformPostAccessDeniedException.class,
                    () -> postService.getPostById(postId));
            assertEquals("You do not have permission to view this post.", ex.getMessage());
        }

        @Test
        @DisplayName("Returns 404 / NotFound for non-existent post")
        void getPostById_NotFound() {
            UUID postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PlatformPostNotFoundException.class, () -> postService.getPostById(postId));
        }

        @Test
        @DisplayName("Returns 404 / NotFound for soft-deleted post")
        void getPostById_SoftDeleted() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Old text")
                    .visibility(PostVisibility.PUBLIC)
                    .status(PostStatus.DELETED)
                    .deletedAt(Instant.now())
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThrows(PlatformPostNotFoundException.class, () -> postService.getPostById(postId));
        }
    }

    @Nested
    @DisplayName("updatePost Tests & IDOR Protection")
    class UpdatePostTests {

        @Test
        @DisplayName("Author can successfully update post body and visibility to PUBLIC")
        void updatePost_AuthorSuccess() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Initial body")
                    .visibility(PostVisibility.PUBLIC)
                    .status(PostStatus.ACTIVE)
                    .createdAt(Instant.now().minusSeconds(300))
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(author);
            when(postRepository.save(any(PlatformPost.class))).thenAnswer(i -> {
                PlatformPost p = i.getArgument(0);
                p.setUpdatedAt(Instant.now());
                return p;
            });

            UpdatePlatformPostRequest updateReq = UpdatePlatformPostRequest.builder()
                    .body("Updated post content")
                    .visibility(PostVisibility.PRIVATE)
                    .build();

            PlatformPostResponse response = postService.updatePost(postId, updateReq);

            assertNotNull(response);
            assertEquals("Updated post content", response.getBody());
            assertEquals(PostVisibility.PRIVATE, response.getVisibility());
            assertTrue(response.isEdited());
        }

        @Test
        @DisplayName("Updating post visibility to FOLLOWERS_ONLY succeeds")
        void updatePost_ToFollowersOnly_Success() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Initial body")
                    .visibility(PostVisibility.PUBLIC)
                    .status(PostStatus.ACTIVE)
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(author);
            when(postRepository.save(any(PlatformPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UpdatePlatformPostRequest updateReq = UpdatePlatformPostRequest.builder()
                    .visibility(PostVisibility.FOLLOWERS_ONLY)
                    .build();

            PlatformPostResponse response = postService.updatePost(postId, updateReq);
            assertNotNull(response);
            assertEquals(PostVisibility.FOLLOWERS_ONLY, response.getVisibility());
            verify(postRepository).save(any(PlatformPost.class));
        }

        @Test
        @DisplayName("Rejects update by non-author with 403 Forbidden (IDOR check)")
        void updatePost_NonAuthor_Forbidden() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Initial body")
                    .visibility(PostVisibility.PUBLIC)
                    .status(PostStatus.ACTIVE)
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser);

            UpdatePlatformPostRequest updateReq = UpdatePlatformPostRequest.builder()
                    .body("Hacked body")
                    .build();

            assertThrows(PlatformPostAccessDeniedException.class, () -> postService.updatePost(postId, updateReq));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects update that leaves post empty")
        void updatePost_RejectsLeavingEmpty() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Initial body")
                    .visibility(PostVisibility.PUBLIC)
                    .status(PostStatus.ACTIVE)
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(author);

            UpdatePlatformPostRequest updateReq = UpdatePlatformPostRequest.builder()
                    .body("   ")
                    .media(Collections.emptyList())
                    .build();

            assertThrows(InvalidPlatformPostException.class, () -> postService.updatePost(postId, updateReq));
            verify(postRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deletePost Tests & Soft Deletion")
    class DeletePostTests {

        @Test
        @DisplayName("Author can soft-delete their own post")
        void deletePost_AuthorSuccess() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("To be deleted")
                    .visibility(PostVisibility.PUBLIC)
                    .status(PostStatus.ACTIVE)
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(author);

            postService.deletePost(postId);

            ArgumentCaptor<PlatformPost> captor = ArgumentCaptor.forClass(PlatformPost.class);
            verify(postRepository).save(captor.capture());
            PlatformPost saved = captor.getValue();
            assertEquals(PostStatus.DELETED, saved.getStatus());
            assertNotNull(saved.getDeletedAt());
            assertTrue(saved.isDeleted());
        }

        @Test
        @DisplayName("Non-author cannot delete another user's post (IDOR protection)")
        void deletePost_NonAuthor_Forbidden() {
            UUID postId = UUID.randomUUID();
            PlatformPost post = PlatformPost.builder()
                    .id(postId)
                    .author(author)
                    .body("Protected content")
                    .visibility(PostVisibility.PUBLIC)
                    .status(PostStatus.ACTIVE)
                    .build();

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser);

            assertThrows(PlatformPostAccessDeniedException.class, () -> postService.deletePost(postId));
            verify(postRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getMyPosts Keyset Pagination & Cursor Validation Tests")
    class GetMyPostsTests {

        @Test
        @DisplayName("Returns first page with next cursor when more items exist")
        void getMyPosts_FirstPage_WithNextCursor() {
            when(currentUserService.getCurrentUser()).thenReturn(author);

            Instant now = Instant.now();
            List<PlatformPost> mockPosts = new ArrayList<>();
            for (int i = 0; i < 4; i++) { // Limit 3 + 1 extra
                mockPosts.add(PlatformPost.builder()
                        .id(UUID.randomUUID())
                        .author(author)
                        .body("Post " + i)
                        .visibility(PostVisibility.PUBLIC)
                        .status(PostStatus.ACTIVE)
                        .createdAt(now.minusSeconds(i * 60))
                        .build());
            }

            when(postRepository.findInitialAuthorPosts(eq(author.getId()), eq(PostStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(mockPosts);

            PlatformPostCursorPageResponse response = postService.getMyPosts(null, 3);

            assertNotNull(response);
            assertEquals(3, response.getItems().size());
            assertTrue(response.isHasMore());
            assertNotNull(response.getNextCursor());

            byte[] decodedBytes = Base64.getUrlDecoder().decode(response.getNextCursor());
            String decodedStr = new String(decodedBytes, StandardCharsets.UTF_8);
            assertTrue(decodedStr.contains(":"));
        }

        @Test
        @DisplayName("Returns last page with null next cursor when no more items exist")
        void getMyPosts_LastPage_NoNextCursor() {
            when(currentUserService.getCurrentUser()).thenReturn(author);

            PlatformPost p1 = PlatformPost.builder()
                    .id(UUID.randomUUID())
                    .author(author)
                    .body("Only post")
                    .visibility(PostVisibility.PUBLIC)
                    .status(PostStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();

            when(postRepository.findInitialAuthorPosts(eq(author.getId()), eq(PostStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(List.of(p1));

            PlatformPostCursorPageResponse response = postService.getMyPosts(null, 5);

            assertNotNull(response);
            assertEquals(1, response.getItems().size());
            assertFalse(response.isHasMore());
            assertNull(response.getNextCursor());
        }

        @Test
        @DisplayName("Rejects malformed cursor: random string")
        void getMyPosts_CursorValidation_RandomString() {
            when(currentUserService.getCurrentUser()).thenReturn(author);
            assertThrows(InvalidPlatformPostException.class, () -> postService.getMyPosts("random_not_base64!", 10));
        }

        @Test
        @DisplayName("Rejects malformed cursor: missing separator")
        void getMyPosts_CursorValidation_MissingSeparator() {
            when(currentUserService.getCurrentUser()).thenReturn(author);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString("1774698000000".getBytes(StandardCharsets.UTF_8));
            assertThrows(InvalidPlatformPostException.class, () -> postService.getMyPosts(encoded, 10));
        }

        @Test
        @DisplayName("Rejects malformed cursor: invalid timestamp")
        void getMyPosts_CursorValidation_InvalidTimestamp() {
            when(currentUserService.getCurrentUser()).thenReturn(author);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString("not_a_number:3a1c8f9b-1234-5678-9abc-def012345678".getBytes(StandardCharsets.UTF_8));
            assertThrows(InvalidPlatformPostException.class, () -> postService.getMyPosts(encoded, 10));
        }

        @Test
        @DisplayName("Rejects malformed cursor: invalid UUID")
        void getMyPosts_CursorValidation_InvalidUUID() {
            when(currentUserService.getCurrentUser()).thenReturn(author);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString("1774698000000:not-a-valid-uuid".getBytes(StandardCharsets.UTF_8));
            assertThrows(InvalidPlatformPostException.class, () -> postService.getMyPosts(encoded, 10));
        }

        @Test
        @DisplayName("Rejects malformed cursor: timestamp out of valid bounds (overflow/negative)")
        void getMyPosts_CursorValidation_TimestampOutOfBounds() {
            when(currentUserService.getCurrentUser()).thenReturn(author);
            String encodedNegative = Base64.getUrlEncoder().withoutPadding().encodeToString("-500:3a1c8f9b-1234-5678-9abc-def012345678".getBytes(StandardCharsets.UTF_8));
            assertThrows(InvalidPlatformPostException.class, () -> postService.getMyPosts(encodedNegative, 10));
        }
    }
}
