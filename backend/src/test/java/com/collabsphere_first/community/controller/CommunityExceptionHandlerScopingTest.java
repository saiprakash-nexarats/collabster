package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.exception.GlobalCommunitiesExceptionHandler;
import com.collabsphere_first.community.exception.ResourceNotFoundException;
import com.collabsphere_first.post.controller.PlatformPostController;
import com.collabsphere_first.post.exception.PlatformPostExceptionHandler;
import com.collabsphere_first.post.exception.PlatformPostNotFoundException;
import com.collabsphere_first.post.service.PlatformPostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Exception Handler Scoping Regression Tests")
class CommunityExceptionHandlerScopingTest {

    @RestController
    static class DummyCommunityController {
        UUID resourceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        @GetMapping("/api/v1/dummy-community/test-error")
        public void throwCommunityError() {
            throw new ResourceNotFoundException(resourceId);
        }
    }

    @Test
    @DisplayName("Proves GlobalCommunitiesExceptionHandler handles community controller exceptions even when PlatformPostExceptionHandler is present")
    void testCommunityExceptionHandlerHandlesCommunityController() throws Exception {
        DummyCommunityController dummyCommunityController = new DummyCommunityController();
        GlobalCommunitiesExceptionHandler communityHandler = new GlobalCommunitiesExceptionHandler();
        PlatformPostExceptionHandler postHandler = new PlatformPostExceptionHandler();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(dummyCommunityController)
                .setControllerAdvice(communityHandler, postHandler)
                .build();

        mockMvc.perform(get("/api/v1/dummy-community/test-error")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found with id: 00000000-0000-0000-0000-000000000001"));
    }

    @Test
    @DisplayName("Proves PlatformPostExceptionHandler handles PlatformPostController exceptions without interference")
    void testPlatformPostExceptionHandlerHandlesPlatformPostController() throws Exception {
        PlatformPostService postService = mock(PlatformPostService.class);
        PlatformPostController postController = new PlatformPostController(postService);
        UUID randomId = UUID.randomUUID();

        when(postService.getPostById(randomId)).thenThrow(new PlatformPostNotFoundException(randomId));

        GlobalCommunitiesExceptionHandler communityHandler = new GlobalCommunitiesExceptionHandler();
        PlatformPostExceptionHandler postHandler = new PlatformPostExceptionHandler();

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setControllerAdvice(communityHandler, postHandler)
                .build();

        mockMvc.perform(get("/api/v1/posts/{postId}", randomId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Platform post not found with ID: " + randomId));
    }
}
