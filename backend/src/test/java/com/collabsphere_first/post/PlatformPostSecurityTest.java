package com.collabsphere_first.post;

import com.collabsphere_first.post.dto.request.CreatePlatformPostRequest;
import com.collabsphere_first.post.dto.request.UpdatePlatformPostRequest;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.exception.PlatformPostAccessDeniedException;
import com.collabsphere_first.post.exception.PlatformPostNotFoundException;
import com.collabsphere_first.post.mapper.PlatformPostMapper;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.post.service.impl.PlatformPostServiceImpl;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Platform Post Security & Anti-Spoofing Tests")
class PlatformPostSecurityTest {

    @Mock
    private PlatformPostRepository postRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Spy
    private PlatformPostMapper postMapper = new PlatformPostMapper();

    @InjectMocks
    private PlatformPostServiceImpl postService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = User.builder().id(100L).username("userA").email("userA@collobster.com").build();
        userB = User.builder().id(200L).username("userB").email("userB@collobster.com").build();
    }

    @Test
    @DisplayName("Identity Anti-Spoofing: Request DTOs must not contain authorId, userId, or ownerId fields")
    void testRequestDtosDoNotExposeAuthorOrUserFields() {
        Class<?>[] classes = { CreatePlatformPostRequest.class, UpdatePlatformPostRequest.class };
        for (Class<?> clazz : classes) {
            for (Field field : clazz.getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                assertFalse(name.contains("authorid"), "Class " + clazz.getSimpleName() + " contains authorId");
                assertFalse(name.contains("userid"), "Class " + clazz.getSimpleName() + " contains userId");
                assertFalse(name.contains("ownerid"), "Class " + clazz.getSimpleName() + " contains ownerId");
            }
        }
    }

    @Test
    @DisplayName("Identity Anti-Spoofing: Malicious client injecting authorId into JSON cannot spoof post author")
    void testClientCannotInjectAuthorIdInJson() throws Exception {
        when(currentUserService.getCurrentUser()).thenReturn(userA);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Attacker attempts to forge authorId: 200 (userB) in request JSON
        String maliciousJson = """
                {
                    "body": "Spoofed post",
                    "visibility": "PUBLIC",
                    "authorId": 200,
                    "userId": 200,
                    "ownerId": 200
                }
                """;

        CreatePlatformPostRequest request = mapper.readValue(maliciousJson, CreatePlatformPostRequest.class);

        when(postRepository.save(any(PlatformPost.class))).thenAnswer(i -> {
            PlatformPost p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });

        postService.createPost(request);

        // Verify the saved entity's author is User A (from SecurityContextHolder), NEVER User B
        ArgumentCaptor<PlatformPost> captor = ArgumentCaptor.forClass(PlatformPost.class);
        verify(postRepository).save(captor.capture());
        assertEquals(userA.getId(), captor.getValue().getAuthor().getId());
        assertNotEquals(userB.getId(), captor.getValue().getAuthor().getId());
    }

    @Test
    @DisplayName("IDOR Prevention: User B cannot edit User A's post")
    void testIdor_UserBCannotEditUserAPost() {
        UUID postAId = UUID.randomUUID();
        PlatformPost postA = PlatformPost.builder()
                .id(postAId)
                .author(userA)
                .body("User A's post")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .build();

        when(postRepository.findById(postAId)).thenReturn(Optional.of(postA));
        when(currentUserService.getCurrentUser()).thenReturn(userB);

        UpdatePlatformPostRequest updateReq = UpdatePlatformPostRequest.builder()
                .body("Tampered by User B")
                .build();

        assertThrows(PlatformPostAccessDeniedException.class, () -> postService.updatePost(postAId, updateReq));
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("IDOR Prevention: User B cannot delete User A's post")
    void testIdor_UserBCannotDeleteUserAPost() {
        UUID postAId = UUID.randomUUID();
        PlatformPost postA = PlatformPost.builder()
                .id(postAId)
                .author(userA)
                .body("User A's post")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .build();

        when(postRepository.findById(postAId)).thenReturn(Optional.of(postA));
        when(currentUserService.getCurrentUser()).thenReturn(userB);

        assertThrows(PlatformPostAccessDeniedException.class, () -> postService.deletePost(postAId));
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("IDOR Prevention: User B cannot access User A's PRIVATE post")
    void testIdor_UserBCannotViewUserAPrivatePost() {
        UUID postAId = UUID.randomUUID();
        PlatformPost postA = PlatformPost.builder()
                .id(postAId)
                .author(userA)
                .body("User A's secret diary")
                .visibility(PostVisibility.PRIVATE)
                .status(PostStatus.ACTIVE)
                .build();

        when(postRepository.findById(postAId)).thenReturn(Optional.of(postA));
        when(currentUserService.getCurrentUser()).thenReturn(userB);

        assertThrows(PlatformPostAccessDeniedException.class, () -> postService.getPostById(postAId));
    }

    @Test
    @DisplayName("Soft Deletion: Deleted post returns 404 NotFound on subsequent retrieval")
    void testSoftDeletion_SubsequentGetReturns404() {
        UUID postId = UUID.randomUUID();
        PlatformPost post = PlatformPost.builder()
                .id(postId)
                .author(userA)
                .body("To be deleted")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(currentUserService.getCurrentUser()).thenReturn(userA);

        postService.deletePost(postId);
        assertTrue(post.isDeleted());
        assertEquals(PostStatus.DELETED, post.getStatus());

        // Subsequent get throws PlatformPostNotFoundException
        assertThrows(PlatformPostNotFoundException.class, () -> postService.getPostById(postId));
    }
}
