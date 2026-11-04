package com.collabsphere_first.interaction;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.exception.PostNotFoundException;
import com.collabsphere_first.community.service.ContentService;
import com.collabsphere_first.interaction.resolver.CommunityPostContentResolver;
import com.collabsphere_first.interaction.resolver.PlatformPostContentResolver;
import com.collabsphere_first.interaction.resolver.ResolvedContent;
import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.exception.PlatformPostAccessDeniedException;
import com.collabsphere_first.post.exception.PlatformPostNotFoundException;
import com.collabsphere_first.post.service.PlatformPostService;
import com.collabsphere_first.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Content Resolvers Test Suite")
class ContentResolversTest {

    @Nested
    @DisplayName("PlatformPostContentResolver Tests")
    class PlatformPostResolverTests {

        @Mock
        private PlatformPostService platformPostService;

        @InjectMocks
        private PlatformPostContentResolver resolver;

        @Test
        @DisplayName("supports PLATFORM_POST only")
        void supports() {
            assertTrue(resolver.supports(ContentType.PLATFORM_POST));
            assertFalse(resolver.supports(ContentType.COMMUNITY_POST));
        }

        @Test
        @DisplayName("resolveVisible successfully resolves platform post")
        void resolveVisible_Success() {
            UUID id = UUID.randomUUID();
            ContentRef ref = ContentRef.platformPost(id);
            User viewer = User.builder().id(10L).build();

            PlatformPostResponse response = PlatformPostResponse.builder()
                    .id(id)
                    .author(AuthorSummaryResponse.builder().id(20L).build())
                    .body("Platform post body text")
                    .build();

            when(platformPostService.getPostById(id)).thenReturn(response);

            ResolvedContent resolved = resolver.resolveVisible(ref, viewer);

            assertNotNull(resolved);
            assertTrue(resolved.accessible());
            assertEquals(20L, resolved.authorId());
            assertEquals("Platform post body text", resolved.summary());
        }

        @Test
        @DisplayName("resolveVisible handles not found gracefully")
        void resolveVisible_NotFound() {
            UUID id = UUID.randomUUID();
            ContentRef ref = ContentRef.platformPost(id);
            User viewer = User.builder().id(10L).build();

            when(platformPostService.getPostById(id)).thenThrow(new PlatformPostNotFoundException(id));
            ResolvedContent resolved = resolver.resolveVisible(ref, viewer);
            assertFalse(resolved.accessible());
        }

        @Test
        @DisplayName("resolveVisible handles access denied gracefully")
        void resolveVisible_AccessDenied() {
            UUID id = UUID.randomUUID();
            ContentRef ref = ContentRef.platformPost(id);
            User viewer = User.builder().id(10L).build();

            when(platformPostService.getPostById(id)).thenThrow(new PlatformPostAccessDeniedException("Forbidden"));
            ResolvedContent resolved = resolver.resolveVisible(ref, viewer);
            assertFalse(resolved.accessible());
        }
    }

    @Nested
    @DisplayName("CommunityPostContentResolver Tests")
    class CommunityPostResolverTests {

        @Mock
        private ContentService contentService;

        @InjectMocks
        private CommunityPostContentResolver resolver;

        @Test
        @DisplayName("supports COMMUNITY_POST only")
        void supports() {
            assertTrue(resolver.supports(ContentType.COMMUNITY_POST));
            assertFalse(resolver.supports(ContentType.PLATFORM_POST));
        }

        @Test
        @DisplayName("resolveVisible successfully resolves community post")
        void resolveVisible_Success() {
            UUID id = UUID.randomUUID();
            ContentRef ref = ContentRef.communityPost(id);
            User viewer = User.builder().id(10L).build();

            PostDto postDto = new PostDto();
            postDto.setId(id);
            postDto.setAuthorId(30L);
            postDto.setTitle("Community Post Title");
            postDto.setBody("Community post body text");

            when(contentService.getPost(id, 10L)).thenReturn(postDto);

            ResolvedContent resolved = resolver.resolveVisible(ref, viewer);

            assertNotNull(resolved);
            assertTrue(resolved.accessible());
            assertEquals(30L, resolved.authorId());
            assertEquals("Community Post Title", resolved.title());
            assertEquals("Community post body text", resolved.summary());
        }

        @Test
        @DisplayName("resolveVisible handles not found or error gracefully")
        void resolveVisible_NotFound() {
            UUID id = UUID.randomUUID();
            ContentRef ref = ContentRef.communityPost(id);
            User viewer = User.builder().id(10L).build();

            when(contentService.getPost(id, 10L)).thenThrow(new PostNotFoundException(id));

            ResolvedContent resolved = resolver.resolveVisible(ref, viewer);

            assertFalse(resolved.accessible());
        }
    }
}
