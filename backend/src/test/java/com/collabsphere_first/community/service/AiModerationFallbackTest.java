package com.collabsphere_first.community.service;

import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.mapper.PostMapper;
import com.collabsphere_first.community.mapper.ReportMapper;
import com.collabsphere_first.community.repository.*;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.community.service.impl.ModerationServiceImpl;
import com.collabsphere_first.integration.ai.AiModerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiModerationFallbackTest {

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ReportMapper reportMapper;
    @Mock private PostMapper postMapper;
    @Mock private AiModerationService aiModerationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AnalyticsService analyticsService;
    @Mock private CommunityRoleService roleService;
    @Mock private CommunityAuditLogService auditLogService;
    @Mock private CommunityEventOutboxService outboxService;
    @Mock private CommunitiesRateLimitConfig rateLimitConfig;

    @InjectMocks
    private ModerationServiceImpl moderationService;

    private UUID postId;
    private Post post;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        post = Post.builder()
                .id(postId)
                .community(Community.builder().id(UUID.randomUUID()).build())
                .authorId(101L)
                .title("Test Post")
                .body("Content for testing")
                .moderationStatus(ModerationStatus.PENDING_REVIEW)
                .build();
    }

    @Test
    @DisplayName("AI moderation passes -> post marked VISIBLE")
    void aiModeration_passed() {
        when(aiModerationService.classifyContentAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture(AiModerationService.ModerationResult.PASSED));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));

        moderationService.triggerAsyncPostModeration(postId, "Clean content");

        assertThat(post.getModerationStatus()).isEqualTo(ModerationStatus.VISIBLE);
        verify(outboxService).record(any(UUID.class), eq("POST_MODERATION_RESULT"), eq(postId.toString()), anyMap());
    }

    @Test
    @DisplayName("AI moderation detects violation -> post marked REMOVED")
    void aiModeration_violation() {
        when(aiModerationService.classifyContentAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture(AiModerationService.ModerationResult.VIOLATION));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));

        moderationService.triggerAsyncPostModeration(postId, "Hate speech content");

        assertThat(post.getModerationStatus()).isEqualTo(ModerationStatus.REMOVED);
    }

    @Test
    @DisplayName("AI moderation times out -> fails closed to FLAGGED")
    void aiModeration_timeout_failsClosed() {
        CompletableFuture<AiModerationService.ModerationResult> slowFuture = new CompletableFuture<>();
        // Will time out waiting for 2 seconds
        when(aiModerationService.classifyContentAsync(anyString())).thenReturn(slowFuture);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));

        moderationService.triggerAsyncPostModeration(postId, "Some content");

        assertThat(post.getModerationStatus()).isEqualTo(ModerationStatus.FLAGGED);
    }

    @Test
    @DisplayName("AI service throws error / unavailable -> fails closed to FLAGGED")
    void aiModeration_error_failsClosed() {
        CompletableFuture<AiModerationService.ModerationResult> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("503 Service Unavailable"));
        when(aiModerationService.classifyContentAsync(anyString())).thenReturn(failedFuture);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));

        moderationService.triggerAsyncPostModeration(postId, "Some content");

        assertThat(post.getModerationStatus()).isEqualTo(ModerationStatus.FLAGGED);
    }
}
