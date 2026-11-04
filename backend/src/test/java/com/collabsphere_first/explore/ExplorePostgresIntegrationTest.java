package com.collabsphere_first.explore;

import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.enums.CommunityType;
import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.PostRepository;
import com.collabsphere_first.explore.dto.ExplorePostSearchResponse;
import com.collabsphere_first.explore.repository.ExploreTrendingRepository;
import com.collabsphere_first.explore.service.ExploreService;
import com.collabsphere_first.explore.trending.ExploreTrendingService;
import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.collabsphere_first.interaction.entity.ContentLike;
import com.collabsphere_first.interaction.entity.ContentSave;
import com.collabsphere_first.interaction.repository.ContentLikeRepository;
import com.collabsphere_first.interaction.repository.ContentSaveRepository;
import com.collabsphere_first.network.entity.UserBlock;
import com.collabsphere_first.network.entity.UserFollow;
import com.collabsphere_first.network.repository.UserBlockRepository;
import com.collabsphere_first.network.repository.UserFollowRepository;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class ExplorePostgresIntegrationTest {

    @Autowired
    private ExploreService exploreService;

    @Autowired
    private ExploreTrendingService trendingService;

    @Autowired
    private ExploreTrendingRepository trendingRepository;

    @Autowired
    private PlatformPostRepository platformPostRepository;

    @Autowired
    private PostRepository communityPostRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFollowRepository userFollowRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private ContentLikeRepository contentLikeRepository;

    @Autowired
    private ContentSaveRepository contentSaveRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private User authorA;
    private User viewerB;
    private User viewerC;
    private Community publicCommunity;
    private Community privateCommunity;

    @BeforeEach
    void setUp() {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        authorA = userRepository.save(User.builder()
                .username("author_a_" + runId)
                .email("author_a_" + runId + "@test.com")
                .firstName("Author")
                .lastName("Alpha")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        viewerB = userRepository.save(User.builder()
                .username("viewer_b_" + runId)
                .email("viewer_b_" + runId + "@test.com")
                .firstName("Viewer")
                .lastName("Beta")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        viewerC = userRepository.save(User.builder()
                .username("viewer_c_" + runId)
                .email("viewer_c_" + runId + "@test.com")
                .firstName("Viewer")
                .lastName("Gamma")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        publicCommunity = communityRepository.save(Community.builder()
                .name("Public Tech " + runId)
                .slug("public-tech-" + runId)
                .description("Public community for testing")
                .type(CommunityType.GENERAL)
                .lifecycle(com.collabsphere_first.community.enums.LifecycleState.PERMANENT)
                .visibility(Visibility.PUBLIC)
                .ownerId(authorA.getId())
                .category(com.collabsphere_first.community.enums.CommunityCategory.TECHNOLOGY)
                .communityStatus(CommunityStatus.ACTIVE)
                .memberCount(1)
                .build());

        privateCommunity = communityRepository.save(Community.builder()
                .name("Secret Club " + runId)
                .slug("secret-club-" + runId)
                .description("Private community for members only")
                .type(CommunityType.GENERAL)
                .lifecycle(com.collabsphere_first.community.enums.LifecycleState.PERMANENT)
                .visibility(Visibility.PRIVATE)
                .ownerId(authorA.getId())
                .category(com.collabsphere_first.community.enums.CommunityCategory.TECHNOLOGY)
                .communityStatus(CommunityStatus.ACTIVE)
                .memberCount(1)
                .build());

        entityManager.flush();
    }

    @Test
    @DisplayName("Mandatory Scenario: Active PlatformPost found, then soft-deleted, then excluded")
    void testPlatformPost_PersistSearchAndSoftDelete() {
        String uniqueMarker = "ZiltoidOmniscient" + System.currentTimeMillis();

        PlatformPost post = PlatformPost.builder()
                .author(authorA)
                .body("Exploring cosmos with " + uniqueMarker + " hyperdrive engine")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .build();
        post = platformPostRepository.saveAndFlush(post);

        // 1. Search while ACTIVE
        ExplorePostSearchResponse searchBeforeDelete =
                exploreService.searchPosts(uniqueMarker, viewerB, null, 10);
        assertThat(searchBeforeDelete.items()).hasSize(1);
        assertThat(searchBeforeDelete.items().get(0).contentRef().id()).isEqualTo(post.getId());

        // 2. Soft-delete the post (status = DELETED)
        post.setStatus(PostStatus.DELETED);
        post.setDeletedAt(Instant.now());
        platformPostRepository.saveAndFlush(post);

        // 3. Search after DELETED -> MUST NOT be returned!
        ExplorePostSearchResponse searchAfterDelete =
                exploreService.searchPosts(uniqueMarker, viewerB, null, 10);
        assertThat(searchAfterDelete.items()).isEmpty();
    }

    @Test
    @DisplayName("Community Moderation Status Audit: Only VISIBLE community posts appear in Explore")
    void testCommunityModerationStates_Consistency() {
        String uniqueVisible = "UniqueVisiblePost" + System.currentTimeMillis();
        String uniquePending = "UniquePendingPost" + System.currentTimeMillis();
        String uniqueFlagged = "UniqueFlaggedPost" + System.currentTimeMillis();
        String uniqueRemoved = "UniqueRemovedPost" + System.currentTimeMillis();

        // 1. VISIBLE post
        Post postVisible = communityPostRepository.saveAndFlush(Post.builder()
                .community(publicCommunity)
                .authorId(authorA.getId())
                .title("Visible Post")
                .body("Body containing " + uniqueVisible)
                .moderationStatus(ModerationStatus.VISIBLE)
                .build());

        // 2. PENDING_REVIEW post
        communityPostRepository.saveAndFlush(Post.builder()
                .community(publicCommunity)
                .authorId(authorA.getId())
                .title("Pending Post")
                .body("Body containing " + uniquePending)
                .moderationStatus(ModerationStatus.PENDING_REVIEW)
                .build());

        // 3. FLAGGED post
        communityPostRepository.saveAndFlush(Post.builder()
                .community(publicCommunity)
                .authorId(authorA.getId())
                .title("Flagged Post")
                .body("Body containing " + uniqueFlagged)
                .moderationStatus(ModerationStatus.FLAGGED)
                .build());

        // 4. REMOVED post
        communityPostRepository.saveAndFlush(Post.builder()
                .community(publicCommunity)
                .authorId(authorA.getId())
                .title("Removed Post")
                .body("Body containing " + uniqueRemoved)
                .moderationStatus(ModerationStatus.REMOVED)
                .build());

        // Verify: Only VISIBLE is found
        ExplorePostSearchResponse resVisible = exploreService.searchPosts(uniqueVisible, viewerB, null, 10);
        assertThat(resVisible.items()).hasSize(1);
        assertThat(resVisible.items().get(0).contentRef().id()).isEqualTo(postVisible.getId());

        ExplorePostSearchResponse resPending = exploreService.searchPosts(uniquePending, viewerB, null, 10);
        assertThat(resPending.items()).isEmpty();

        ExplorePostSearchResponse resFlagged = exploreService.searchPosts(uniqueFlagged, viewerB, null, 10);
        assertThat(resFlagged.items()).isEmpty();

        ExplorePostSearchResponse resRemoved = exploreService.searchPosts(uniqueRemoved, viewerB, null, 10);
        assertThat(resRemoved.items()).isEmpty();
    }

    @Test
    @DisplayName("Search Visibility <= Direct Visibility: Followers-only, Private, and Community access")
    void testSearchVisibility_Invariants() {
        String marker = "MatrixSecurityRule" + System.currentTimeMillis();

        // 1. Private PlatformPost
        platformPostRepository.saveAndFlush(PlatformPost.builder()
                .author(authorA)
                .body("Private diary " + marker + " top-secret")
                .visibility(PostVisibility.PRIVATE)
                .status(PostStatus.ACTIVE)
                .build());

        // 2. Followers-only PlatformPost
        PlatformPost followersPost = platformPostRepository.saveAndFlush(PlatformPost.builder()
                .author(authorA)
                .body("Followers only memo " + marker)
                .visibility(PostVisibility.FOLLOWERS_ONLY)
                .status(PostStatus.ACTIVE)
                .build());

        // 3. Private Community post
        communityPostRepository.saveAndFlush(Post.builder()
                .community(privateCommunity)
                .authorId(authorA.getId())
                .title("Private Community Secret")
                .body("Private community message " + marker)
                .moderationStatus(ModerationStatus.VISIBLE)
                .build());

        // ViewerB is NOT a follower and NOT a member of private community
        ExplorePostSearchResponse resNonFollower = exploreService.searchPosts(marker, viewerB, null, 10);
        assertThat(resNonFollower.items()).isEmpty();

        // ViewerC FOLLOWS authorA -> can discover followersPost, but NOT private post or private community post
        userFollowRepository.saveAndFlush(UserFollow.builder()
                .follower(viewerC)
                .following(authorA)
                .build());

        ExplorePostSearchResponse resFollower = exploreService.searchPosts(marker, viewerC, null, 10);
        assertThat(resFollower.items()).hasSize(1);
        assertThat(resFollower.items().get(0).contentRef().id()).isEqualTo(followersPost.getId());

        // AuthorA searches: author can see followersPost
        ExplorePostSearchResponse resAuthor = exploreService.searchPosts(marker, authorA, null, 10);
        assertThat(resAuthor.items()).isNotEmpty();
    }

    @Test
    @DisplayName("Bidirectional Block Boundary: Blocked author content never appears in Explore")
    void testBlockExclusion_InSearchAndDiscovery() {
        String uniqueBlock = "BlockedAuthorStory" + System.currentTimeMillis();

        platformPostRepository.saveAndFlush(PlatformPost.builder()
                .author(authorA)
                .body("Author A post " + uniqueBlock)
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .build());

        // Before block: ViewerB can find it
        assertThat(exploreService.searchPosts(uniqueBlock, viewerB, null, 10).items()).hasSize(1);

        // Block A -> B (Author blocks viewer)
        userBlockRepository.saveAndFlush(UserBlock.builder()
                .blocker(authorA)
                .blocked(viewerB)
                .build());

        assertThat(exploreService.searchPosts(uniqueBlock, viewerB, null, 10).items()).isEmpty();
    }

    @Test
    @DisplayName("ContentRef Collision Safety: Same UUID across PLATFORM_POST and COMMUNITY_POST")
    void testTrending_ContentRefCollisionSameUuid() {
        UUID sharedId = UUID.randomUUID();

        // 1. Create PlatformPost with sharedId via JDBC to bypass JPA UUID auto-generation semantics
        jdbcTemplate.update("""
            INSERT INTO platform_posts (id, author_id, body, visibility, status, created_at, updated_at, version)
            VALUES (?, ?, ?, 'PUBLIC', 'ACTIVE', NOW(), NOW(), 0)
        """, sharedId, authorA.getId(), "Platform version of collision item");

        // 2. Create CommunityPost with identical sharedId via JDBC
        jdbcTemplate.update("""
            INSERT INTO posts (id, community_id, author_id, type, title, body, pinned, locked, comment_count, moderation_status, created_at, updated_at)
            VALUES (?, ?, ?, 'DISCUSSION', 'Community Collision Title', 'Community version of collision item', false, false, 0, 'VISIBLE', NOW(), NOW())
        """, sharedId, publicCommunity.getId(), authorA.getId());

        // 3. Interactions:
        // Platform gets 1 like from viewerB
        contentLikeRepository.saveAndFlush(ContentLike.builder()
                .user(viewerB)
                .contentType(ContentType.PLATFORM_POST)
                .contentId(sharedId)
                .build());

        // Community gets 1 save from viewerC
        contentSaveRepository.saveAndFlush(ContentSave.builder()
                .user(viewerC)
                .contentType(ContentType.COMMUNITY_POST)
                .contentId(sharedId)
                .build());

        // Verify aggregation yields TWO independent stats
        List<ExploreTrendingRepository.TrendingRawStats> stats =
                trendingRepository.aggregateTrendingInteractions(Instant.now().minusSeconds(3600), 50);

        Optional<ExploreTrendingRepository.TrendingRawStats> platformStat = stats.stream()
                .filter(s -> s.contentRef().type() == ContentType.PLATFORM_POST && s.contentRef().id().equals(sharedId))
                .findFirst();

        Optional<ExploreTrendingRepository.TrendingRawStats> communityStat = stats.stream()
                .filter(s -> s.contentRef().type() == ContentType.COMMUNITY_POST && s.contentRef().id().equals(sharedId))
                .findFirst();

        assertThat(platformStat).isPresent();
        assertThat(platformStat.get().likes()).isEqualTo(1);
        assertThat(platformStat.get().saves()).isEqualTo(0);

        assertThat(communityStat).isPresent();
        assertThat(communityStat.get().likes()).isEqualTo(0);
        assertThat(communityStat.get().saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("Historical Events Cannot Resurrect Inaccessible Content")
    void testTrending_InaccessibleContentRemovedDuringHydration() {
        // 1. Create a public platform post
        PlatformPost post = platformPostRepository.saveAndFlush(PlatformPost.builder()
                .author(authorA)
                .body("Trending candidate that will be deleted")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .build());
        UUID postId = post.getId();

        // 2. Accumulate likes
        contentLikeRepository.saveAndFlush(ContentLike.builder()
                .user(viewerB)
                .contentType(ContentType.PLATFORM_POST)
                .contentId(postId)
                .build());

        // Verify it appears in trending preview
        List<FeedItemResponse> previewBefore = trendingService.getTrendingPreview(viewerC, 50);
        boolean containsBefore = previewBefore.stream().anyMatch(i -> i.contentRef().id().equals(postId));
        assertThat(containsBefore).isTrue();

        // 3. Post becomes DELETED
        post.setStatus(PostStatus.DELETED);
        post.setDeletedAt(Instant.now());
        platformPostRepository.saveAndFlush(post);

        // 4. Verify trending hydration drops it despite historical like
        List<FeedItemResponse> previewAfter = trendingService.getTrendingPreview(viewerC, 50);
        boolean containsAfter = previewAfter.stream().anyMatch(i -> i.contentRef().id().equals(postId));
        assertThat(containsAfter).isFalse();
    }
}
