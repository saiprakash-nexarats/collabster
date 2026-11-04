package com.collabsphere_first.community;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.entity.CommunityNotification;
import com.collabsphere_first.community.enums.CommunityCategory;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.enums.CommunityType;
import com.collabsphere_first.community.enums.LifecycleState;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.enums.PostType;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.event.CommunityNotificationSavedEvent;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.repository.PostRepository;
import com.collabsphere_first.community.service.CommunityContentAccessService;
import com.collabsphere_first.community.service.ContentService;
import com.collabsphere_first.explore.hydration.ExploreContentHydrator;
import com.collabsphere_first.explore.repository.ExploreTrendingRepository;
import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.collabsphere_first.feed.hydration.FeedContentHydrator;
import com.collabsphere_first.feed.hydration.HydratedFeedItem;
import com.collabsphere_first.interaction.dto.InteractionEventRequest;
import com.collabsphere_first.interaction.dto.InteractionSummary;
import com.collabsphere_first.interaction.dto.LikeResponse;
import com.collabsphere_first.interaction.enums.InteractionEventType;
import com.collabsphere_first.interaction.repository.ContentLikeRepository;
import com.collabsphere_first.interaction.service.InteractionService;
import com.collabsphere_first.network.entity.UserBlock;
import com.collabsphere_first.network.repository.UserBlockRepository;
import com.collabsphere_first.notification.entity.Notification;
import com.collabsphere_first.notification.enums.NavigationTargetType;
import com.collabsphere_first.notification.repository.NotificationRepository;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/collabster",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class CommunityPlatformIntegrationPostgresTest {

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private CommunityContentAccessService communityContentAccessService;

    @Autowired
    private FeedContentHydrator feedContentHydrator;

    @Autowired
    private ExploreContentHydrator exploreContentHydrator;

    @Autowired
    private ExploreTrendingRepository exploreTrendingRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private PostRepository communityPostRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformPostRepository platformPostRepository;

    @Autowired
    private ContentLikeRepository contentLikeRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private EntityManager entityManager;

    private User authorA;
    private User viewerB;
    private User viewerC;
    private Community publicCommunity;

    @BeforeEach
    void setUp() {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        authorA = userRepository.save(User.builder()
                .username("phase8_author_" + runId)
                .email("phase8_author_" + runId + "@test.com")
                .firstName("Author")
                .lastName("Alpha")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        viewerB = userRepository.save(User.builder()
                .username("phase8_viewer_b_" + runId)
                .email("phase8_viewer_b_" + runId + "@test.com")
                .firstName("Viewer")
                .lastName("Beta")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        viewerC = userRepository.save(User.builder()
                .username("phase8_viewer_c_" + runId)
                .email("phase8_viewer_c_" + runId + "@test.com")
                .firstName("Viewer")
                .lastName("Gamma")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        publicCommunity = communityRepository.save(Community.builder()
                .name("Phase 8 Tech " + runId)
                .slug("phase8-tech-" + runId)
                .description("Public testing community for Phase 8")
                .type(CommunityType.GENERAL)
                .lifecycle(LifecycleState.PERMANENT)
                .visibility(Visibility.PUBLIC)
                .ownerId(authorA.getId())
                .category(CommunityCategory.TECHNOLOGY)
                .communityStatus(CommunityStatus.ACTIVE)
                .memberCount(2)
                .build());

        membershipRepository.save(Membership.builder()
                .community(publicCommunity)
                .userId(authorA.getId())
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        membershipRepository.save(Membership.builder()
                .community(publicCommunity)
                .userId(viewerB.getId())
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build());

        entityManager.flush();
    }

    @Test
    @DisplayName("Mandatory Sections 36 & 37: Authoritative Community LIKE Same-Source across Community, Feed, Explore")
    void testAuthoritativeCommunityLikeSameSourceAcrossSurfaces() {
        Post post = communityPostRepository.save(Post.builder()
                .community(publicCommunity)
                .authorId(authorA.getId())
                .title("Phase 8 Distributed Architecture")
                .body("Single source of truth for all post interactions.")
                .moderationStatus(ModerationStatus.VISIBLE)
                .type(PostType.DISCUSSION)
                .commentCount(3)
                .build());
        entityManager.flush();

        ContentRef contentRef = ContentRef.communityPost(post.getId());

        // 1. Initial state check across surfaces: likeCount == 0, liked == false
        PostDto directDto = contentService.getPost(post.getId(), viewerB.getId());
        assertThat(directDto.isLiked()).isFalse();
        assertThat(directDto.getLikeCount()).isEqualTo(0L);

        Map<ContentRef, HydratedFeedItem> feedItems = feedContentHydrator.hydrate(List.of(contentRef), viewerB);
        assertThat(feedItems).containsKey(contentRef);
        assertThat(feedItems.get(contentRef).community()).isNotNull();
        assertThat(feedItems.get(contentRef).community().slug()).isEqualTo(publicCommunity.getSlug());

        List<FeedItemResponse> exploreItems = exploreContentHydrator.hydrateAndEnrich(List.of(contentRef), viewerB);
        assertThat(exploreItems).hasSize(1);
        assertThat(exploreItems.get(0).interactionState().liked()).isFalse();
        assertThat(exploreItems.get(0).engagement().likeCount()).isEqualTo(0L);

        // 2. User B likes the Community Post through the authoritative InteractionService
        LikeResponse likeResp = interactionService.likeContent(viewerB, contentRef);
        assertThat(likeResp.isLiked()).isTrue();
        assertThat(likeResp.getLikeCount()).isEqualTo(1L);

        // 3. Verify identical state in Community direct retrieval
        PostDto postAfterLike = contentService.getPost(post.getId(), viewerB.getId());
        assertThat(postAfterLike.isLiked()).isTrue();
        assertThat(postAfterLike.getLikeCount()).isEqualTo(1L);

        // 4. Verify identical state in Explore hydration
        List<FeedItemResponse> exploreAfterLike = exploreContentHydrator.hydrateAndEnrich(List.of(contentRef), viewerB);
        assertThat(exploreAfterLike).hasSize(1);
        assertThat(exploreAfterLike.get(0).interactionState().liked()).isTrue();
        assertThat(exploreAfterLike.get(0).engagement().likeCount()).isEqualTo(1L);

        // 5. Verify interaction summary batch lookup
        Map<ContentRef, InteractionSummary> summaries = interactionService.getInteractionSummaries(viewerB.getId(), List.of(contentRef));
        assertThat(summaries.get(contentRef).liked()).isTrue();
        assertThat(summaries.get(contentRef).likeCount()).isEqualTo(1L);

        // 6. Section 38: Verify storage in content_likes, no duplicate table exists
        assertThat(contentLikeRepository.existsByUserIdAndContentTypeAndContentId(
                viewerB.getId(), ContentType.COMMUNITY_POST, post.getId())).isTrue();

        // 7. Unlike the Community Post
        LikeResponse unlikeResp = interactionService.unlikeContent(viewerB, contentRef);
        assertThat(unlikeResp.isLiked()).isFalse();
        assertThat(unlikeResp.getLikeCount()).isEqualTo(0L);

        // 8. Re-verify all surfaces immediately reflect unlike
        PostDto postAfterUnlike = contentService.getPost(post.getId(), viewerB.getId());
        assertThat(postAfterUnlike.isLiked()).isFalse();
        assertThat(postAfterUnlike.getLikeCount()).isEqualTo(0L);

        List<FeedItemResponse> exploreAfterUnlike = exploreContentHydrator.hydrateAndEnrich(List.of(contentRef), viewerB);
        assertThat(exploreAfterUnlike.get(0).interactionState().liked()).isFalse();
        assertThat(exploreAfterUnlike.get(0).engagement().likeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Mandatory Section 35: Cross-Surface ContentRef Collision Test with Identical UUID")
    void testCrossSurfaceContentRefCollisionWithSharedUuid() {
        // Create PlatformPost
        PlatformPost platformPost = platformPostRepository.save(PlatformPost.builder()
                .author(authorA)
                .body("Platform Post Body with shared UUID")
                .status(PostStatus.ACTIVE)
                .visibility(PostVisibility.PUBLIC)
                .build());
        entityManager.flush();
        UUID sharedId = platformPost.getId();

        // Create CommunityPost with identical sharedId in database
        entityManager.createNativeQuery("""
            INSERT INTO posts (id, community_id, author_id, type, title, body, pinned, locked, comment_count, moderation_status, created_at, updated_at)
            VALUES (:id, :commId, :authorId, 'DISCUSSION', 'Community Post with shared UUID', 'Community Post Body', false, false, 0, 'VISIBLE', now(), now())
            """)
            .setParameter("id", sharedId)
            .setParameter("commId", publicCommunity.getId())
            .setParameter("authorId", authorA.getId())
            .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        ContentRef platformRef = ContentRef.platformPost(sharedId);
        ContentRef communityRef = ContentRef.communityPost(sharedId);

        // Action 1: Like both Platform and Community posts
        interactionService.likeContent(viewerB, platformRef);
        interactionService.likeContent(viewerB, communityRef);

        // Action 2: Save ONLY Community post
        interactionService.saveContent(viewerB, communityRef);

        // Action 3: Hide ONLY Platform post
        interactionService.recordEvent(viewerB, InteractionEventRequest.builder()
                .contentRef(platformRef)
                .eventType(InteractionEventType.HIDE)
                .build());

        // Verify independent states:
        assertThat(interactionService.isLiked(viewerB.getId(), platformRef)).isTrue();
        assertThat(interactionService.isLiked(viewerB.getId(), communityRef)).isTrue();

        assertThat(interactionService.isSaved(viewerB.getId(), platformRef)).isFalse();
        assertThat(interactionService.isSaved(viewerB.getId(), communityRef)).isTrue();

        Set<ContentRef> suppressed = interactionService.findSuppressedContentRefs(viewerB.getId());
        assertThat(suppressed).contains(platformRef);
        assertThat(suppressed).doesNotContain(communityRef);

        // Batch summaries must reflect independent states
        Map<ContentRef, InteractionSummary> summaries = interactionService.getInteractionSummaries(
                viewerB.getId(), List.of(platformRef, communityRef));

        assertThat(summaries.get(platformRef).liked()).isTrue();
        assertThat(summaries.get(platformRef).saved()).isFalse();

        assertThat(summaries.get(communityRef).liked()).isTrue();
        assertThat(summaries.get(communityRef).saved()).isTrue();
    }

    @Test
    @DisplayName("Mandatory Section 44 & 45: Block Author Suppresses Community Post from Feed/Explore without Changing Membership")
    void testBlockAuthorSuppressesCommunityPostFromFeedAndExploreWithoutAlteringMembership() {
        Post postByAuthorA = communityPostRepository.save(Post.builder()
                .community(publicCommunity)
                .authorId(authorA.getId())
                .title("Author A announcement")
                .body("Content from user who will be blocked")
                .moderationStatus(ModerationStatus.VISIBLE)
                .type(PostType.DISCUSSION)
                .build());
        entityManager.flush();

        ContentRef contentRef = ContentRef.communityPost(postByAuthorA.getId());

        // Viewer B can initially see the post in Feed & Explore
        Map<ContentRef, HydratedFeedItem> initialFeed = feedContentHydrator.hydrate(List.of(contentRef), viewerB);
        assertThat(initialFeed).containsKey(contentRef);

        List<FeedItemResponse> initialExplore = exploreContentHydrator.hydrateAndEnrich(List.of(contentRef), viewerB);
        assertThat(initialExplore).hasSize(1);

        // Viewer B blocks Author A
        userBlockRepository.save(UserBlock.builder()
                .blocker(viewerB)
                .blocked(authorA)
                .build());
        entityManager.flush();

        // 1. Passive Feed hydration must suppress the blocked author's post
        Map<ContentRef, HydratedFeedItem> blockedFeed = feedContentHydrator.hydrate(List.of(contentRef), viewerB);
        assertThat(blockedFeed).doesNotContainKey(contentRef);

        // 2. Passive Explore hydration must suppress the blocked author's post
        List<FeedItemResponse> blockedExplore = exploreContentHydrator.hydrateAndEnrich(List.of(contentRef), viewerB);
        assertThat(blockedExplore).isEmpty();

        // 3. Section 44: Community memberships of both A and B must remain ACTIVE (untouched)
        Optional<Membership> memA = membershipRepository.findByCommunityIdAndUserId(publicCommunity.getId(), authorA.getId());
        Optional<Membership> memB = membershipRepository.findByCommunityIdAndUserId(publicCommunity.getId(), viewerB.getId());
        assertThat(memA).isPresent();
        assertThat(memA.get().getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(memB).isPresent();
        assertThat(memB.get().getStatus()).isEqualTo(MembershipStatus.ACTIVE);

        // 4. Section 45: Direct Community navigation inside community allows reading post based on membership
        PostDto directRead = contentService.getPost(postByAuthorA.getId(), viewerB.getId());
        assertThat(directRead).isNotNull();
        assertThat(directRead.getTitle()).isEqualTo("Author A announcement");
    }

    @Test
    @DisplayName("Mandatory Section 40: Explore Trending Uses Single Authoritative Source of Truth")
    void testTrendingInteractionsUsesAuthoritativeSingleSourceOfTruth() {
        Post post = communityPostRepository.save(Post.builder()
                .community(publicCommunity)
                .authorId(authorA.getId())
                .title("Trending Community Topic")
                .body("High engagement discussion")
                .moderationStatus(ModerationStatus.VISIBLE)
                .type(PostType.DISCUSSION)
                .build());
        entityManager.flush();

        ContentRef contentRef = ContentRef.communityPost(post.getId());

        // Record likes from 2 users
        interactionService.likeContent(viewerB, contentRef);
        interactionService.likeContent(viewerC, contentRef);

        // Record save from 1 user
        interactionService.saveContent(viewerB, contentRef);

        // Record share event
        interactionService.recordShare(viewerC, contentRef);
        entityManager.flush();

        // Query Explore trending aggregation
        Instant windowStart = Instant.now().minusSeconds(3600);
        List<ExploreTrendingRepository.TrendingRawStats> statsList =
                exploreTrendingRepository.aggregateTrendingInteractions(windowStart, 50);

        Optional<ExploreTrendingRepository.TrendingRawStats> postStats = statsList.stream()
                .filter(s -> s.contentRef().equals(contentRef))
                .findFirst();

        assertThat(postStats).isPresent();
        assertThat(postStats.get().likes()).isEqualTo(2L);
        assertThat(postStats.get().saves()).isEqualTo(1L);
        assertThat(postStats.get().shares()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Mandatory Section 30: Notification Bridge Includes Typed Action and Authoritative Identifiers")
    void testNotificationBridgeIncludesTypedActionAndAuthoritativeIds() {
        UUID postId = UUID.randomUUID();
        UUID commId = publicCommunity.getId();

        CommunityNotification cn = CommunityNotification.builder()
                .id(UUID.randomUUID())
                .userId(viewerB.getId())
                .type("POST_ACTIVITY")
                .title("Post Activity")
                .body("Someone interacted with your post")
                .targetType("POST")
                .targetId(postId.toString())
                .communityId(commId)
                .build();

        // Publish event to trigger bridge
        eventPublisher.publishEvent(new CommunityNotificationSavedEvent(this, cn));
        entityManager.flush();

        // Find bridged notification in Global Notification Center
        Optional<Notification> bridged = notificationRepository.findByEventKey("COMMUNITY:" + cn.getId() + ":" + viewerB.getId());

        assertThat(bridged).isPresent();
        Notification n = bridged.get();
        assertThat(n.getTargetType()).isEqualTo(NavigationTargetType.OPEN_COMMUNITY_POST);
        assertThat(n.getTargetId()).isEqualTo(postId.toString());
        assertThat(n.getPayload()).containsEntry("communityId", commId.toString());
        assertThat(n.getPayload()).containsEntry("communityPostId", postId.toString());
    }

    @Test
    @DisplayName("Section 25 & 46: CommunityContentAccessService Batch Query Efficiency")
    void testCommunityContentAccessServiceBatchQuery() {
        Post p1 = communityPostRepository.save(Post.builder()
                .community(publicCommunity)
                .authorId(authorA.getId())
                .title("Post One")
                .body("Body One")
                .moderationStatus(ModerationStatus.VISIBLE)
                .build());

        Post p2 = communityPostRepository.save(Post.builder()
                .community(publicCommunity)
                .authorId(authorA.getId())
                .title("Post Two")
                .body("Body Two")
                .moderationStatus(ModerationStatus.VISIBLE)
                .build());
        entityManager.flush();

        interactionService.likeContent(viewerB, ContentRef.communityPost(p1.getId()));

        Map<UUID, PostDto> visible = communityContentAccessService.getVisiblePosts(
                List.of(p1.getId(), p2.getId()), viewerB.getId());

        assertThat(visible).containsKeys(p1.getId(), p2.getId());
        assertThat(visible.get(p1.getId()).isLiked()).isTrue();
        assertThat(visible.get(p1.getId()).getLikeCount()).isEqualTo(1L);
        assertThat(visible.get(p2.getId()).isLiked()).isFalse();
        assertThat(visible.get(p2.getId()).getLikeCount()).isEqualTo(0L);
    }
}
