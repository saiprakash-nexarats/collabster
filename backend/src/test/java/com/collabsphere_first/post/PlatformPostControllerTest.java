package com.collabsphere_first.post;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.post.controller.PlatformPostController;
import com.collabsphere_first.post.dto.request.CreatePlatformPostRequest;
import com.collabsphere_first.post.dto.request.UpdatePlatformPostRequest;
import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;
import com.collabsphere_first.post.dto.response.PlatformPostCursorPageResponse;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.exception.PlatformPostAccessDeniedException;
import com.collabsphere_first.post.exception.PlatformPostExceptionHandler;
import com.collabsphere_first.post.exception.PlatformPostNotFoundException;
import com.collabsphere_first.post.service.PlatformPostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformPostController MockMvc REST Tests")
class PlatformPostControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private PlatformPostService postService;

    @InjectMocks
    private PlatformPostController postController;

    private AuthorSummaryResponse authorSummary;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setControllerAdvice(new PlatformPostExceptionHandler())
                .build();

        authorSummary = AuthorSummaryResponse.builder()
                .id(42L)
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .profilePicture("https://cdn.collobster.com/avatars/42.png")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/posts - Successfully creates post and returns 201 Created")
    void testCreatePost_Success() throws Exception {
        UUID postId = UUID.randomUUID();
        CreatePlatformPostRequest request = CreatePlatformPostRequest.builder()
                .body("Hello Collobster!")
                .visibility(PostVisibility.PUBLIC)
                .build();

        PlatformPostResponse responseDto = PlatformPostResponse.builder()
                .contentRef(ContentRef.platformPost(postId))
                .id(postId)
                .author(authorSummary)
                .body("Hello Collobster!")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .media(Collections.emptyList())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .edited(false)
                .version(0L)
                .build();

        when(postService.createPost(any(CreatePlatformPostRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post created successfully"))
                .andExpect(jsonPath("$.data.id").value(postId.toString()))
                .andExpect(jsonPath("$.data.body").value("Hello Collobster!"))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.contentRef.type").value("PLATFORM_POST"))
                .andExpect(jsonPath("$.data.contentRef.id").value(postId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{id} - Successfully retrieves post and returns 200 OK")
    void testGetPostById_Success() throws Exception {
        UUID postId = UUID.randomUUID();
        PlatformPostResponse responseDto = PlatformPostResponse.builder()
                .contentRef(ContentRef.platformPost(postId))
                .id(postId)
                .author(authorSummary)
                .body("Public post content")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .media(Collections.emptyList())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .edited(false)
                .version(0L)
                .build();

        when(postService.getPostById(postId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/posts/{postId}", postId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(postId.toString()))
                .andExpect(jsonPath("$.data.body").value("Public post content"));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{id} - Returns 404 Not Found when post does not exist")
    void testGetPostById_NotFound() throws Exception {
        UUID postId = UUID.randomUUID();
        when(postService.getPostById(postId)).thenThrow(new PlatformPostNotFoundException(postId));

        mockMvc.perform(get("/api/v1/posts/{postId}", postId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Platform post not found with ID: " + postId));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{id} - Returns 403 Forbidden for unauthorized private post")
    void testGetPostById_Forbidden() throws Exception {
        UUID postId = UUID.randomUUID();
        when(postService.getPostById(postId))
                .thenThrow(new PlatformPostAccessDeniedException("You do not have permission to view this private post."));

        mockMvc.perform(get("/api/v1/posts/{postId}", postId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You do not have permission to view this private post."));
    }

    @Test
    @DisplayName("PATCH /api/v1/posts/{id} - Successfully updates post and returns 200 OK")
    void testUpdatePost_Success() throws Exception {
        UUID postId = UUID.randomUUID();
        UpdatePlatformPostRequest request = UpdatePlatformPostRequest.builder()
                .body("Updated description text")
                .visibility(PostVisibility.PUBLIC)
                .build();

        PlatformPostResponse responseDto = PlatformPostResponse.builder()
                .contentRef(ContentRef.platformPost(postId))
                .id(postId)
                .author(authorSummary)
                .body("Updated description text")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .media(Collections.emptyList())
                .createdAt(Instant.now().minusSeconds(120))
                .updatedAt(Instant.now())
                .edited(true)
                .version(1L)
                .build();

        when(postService.updatePost(eq(postId), any(UpdatePlatformPostRequest.class))).thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post updated successfully"))
                .andExpect(jsonPath("$.data.body").value("Updated description text"))
                .andExpect(jsonPath("$.data.edited").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/posts/{id} - Returns 403 Forbidden when non-author attempts update")
    void testUpdatePost_Forbidden() throws Exception {
        UUID postId = UUID.randomUUID();
        UpdatePlatformPostRequest request = UpdatePlatformPostRequest.builder()
                .body("Malicious edit")
                .build();

        when(postService.updatePost(eq(postId), any(UpdatePlatformPostRequest.class)))
                .thenThrow(new PlatformPostAccessDeniedException("You are not authorized to update this post."));

        mockMvc.perform(patch("/api/v1/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You are not authorized to update this post."));
    }

    @Test
    @DisplayName("DELETE /api/v1/posts/{id} - Successfully soft deletes post and returns 200 OK")
    void testDeletePost_Success() throws Exception {
        UUID postId = UUID.randomUUID();
        doNothing().when(postService).deletePost(postId);

        mockMvc.perform(delete("/api/v1/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post deleted successfully"));

        verify(postService, times(1)).deletePost(postId);
    }

    @Test
    @DisplayName("DELETE /api/v1/posts/{id} - Returns 403 Forbidden when non-author attempts delete")
    void testDeletePost_Forbidden() throws Exception {
        UUID postId = UUID.randomUUID();
        doThrow(new PlatformPostAccessDeniedException("You are not authorized to delete this post."))
                .when(postService).deletePost(postId);

        mockMvc.perform(delete("/api/v1/posts/{postId}", postId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You are not authorized to delete this post."));
    }

    @Test
    @DisplayName("GET /api/v1/posts/me - Successfully retrieves current user posts with pagination")
    void testGetMyPosts_Success() throws Exception {
        UUID postId = UUID.randomUUID();
        PlatformPostResponse postResponse = PlatformPostResponse.builder()
                .contentRef(ContentRef.platformPost(postId))
                .id(postId)
                .author(authorSummary)
                .body("My authored post")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .media(Collections.emptyList())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .edited(false)
                .version(0L)
                .build();

        PlatformPostCursorPageResponse pageResponse = PlatformPostCursorPageResponse.builder()
                .items(List.of(postResponse))
                .nextCursor("next-token-abc")
                .hasMore(true)
                .limit(20)
                .build();

        when(postService.getMyPosts(eq("cursor-token-123"), eq(20))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/posts/me")
                        .param("cursor", "cursor-token-123")
                        .param("limit", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].id").value(postId.toString()))
                .andExpect(jsonPath("$.data.nextCursor").value("next-token-abc"))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.limit").value(20));
    }
}
