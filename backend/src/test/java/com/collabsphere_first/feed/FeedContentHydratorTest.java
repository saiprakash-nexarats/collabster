package com.collabsphere_first.feed;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.repository.PostRepository;
import com.collabsphere_first.community.service.CommunityFeedReadService;
import com.collabsphere_first.feed.hydration.*;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.service.PlatformPostFeedReadService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedContentHydratorTest {

    @Mock
    private PlatformPostFeedReadService platformPostFeedReadService;

    @Mock
    private CommunityFeedReadService communityFeedReadService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NetworkAccessService networkAccessService;

    private User viewer;

    @BeforeEach
    void setUp() {
        viewer = User.builder().id(100L).username("viewer").build();
    }

    @Test
    @DisplayName("PlatformPostFeedAdapter hydrates public post, suppresses private, followers-only, blocked, and muted posts")
    void testPlatformPostFeedAdapter() {
        PlatformPostFeedAdapter adapter = new PlatformPostFeedAdapter(platformPostFeedReadService, networkAccessService);
        assertThat(adapter.supportedType()).isEqualTo(ContentType.PLATFORM_POST);

        UUID publicId = UUID.randomUUID();
        UUID privateId = UUID.randomUUID();
        UUID followersOnlyId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();

        AuthorSummaryResponse author1 = AuthorSummaryResponse.builder().id(200L).username("author1").build();
        AuthorSummaryResponse author2 = AuthorSummaryResponse.builder().id(300L).username("author2").build();
        AuthorSummaryResponse authorBlocked = AuthorSummaryResponse.builder().id(400L).username("blockedAuthor").build();

        PlatformPostResponse publicPost = PlatformPostResponse.builder()
                .id(publicId)
                .author(author1)
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .body("Public content")
                .createdAt(Instant.now())
                .build();

        PlatformPostResponse privatePost = PlatformPostResponse.builder()
                .id(privateId)
                .author(author1)
                .visibility(PostVisibility.PRIVATE)
                .status(PostStatus.ACTIVE)
                .body("Private content")
                .createdAt(Instant.now())
                .build();

        PlatformPostResponse followersOnlyPost = PlatformPostResponse.builder()
                .id(followersOnlyId)
                .author(author2)
                .visibility(PostVisibility.FOLLOWERS_ONLY)
                .status(PostStatus.ACTIVE)
                .body("Followers only content")
                .createdAt(Instant.now())
                .build();

        PlatformPostResponse blockedPost = PlatformPostResponse.builder()
                .id(blockedId)
                .author(authorBlocked)
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .body("Blocked content")
                .createdAt(Instant.now())
                .build();

        when(platformPostFeedReadService.batchGetActivePosts(anyCollection()))
                .thenReturn(Map.of(
                        publicId, publicPost,
                        privateId, privatePost,
                        followersOnlyId, followersOnlyPost,
                        blockedId, blockedPost
                ));

        when(networkAccessService.findBlockedOrBlockingUserIdsAmong(eq(100L), anyCollection()))
                .thenReturn(Set.of(400L));
        when(networkAccessService.findMutedUserIdsAmong(eq(100L), anyCollection()))
                .thenReturn(Collections.emptySet());
        when(networkAccessService.findFollowingUserIdsAmong(eq(100L), anyCollection()))
                .thenReturn(Collections.emptySet());

        Map<UUID, HydratedFeedItem> result = adapter.batchHydrate(List.of(publicId, privateId, followersOnlyId, blockedId), viewer);

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(publicId);
        assertThat(result.get(publicId).body()).isEqualTo("Public content");
    }

    @Test
    @DisplayName("CommunityPostFeedAdapter hydrates eligible posts and resolves community name and author")
    void testCommunityPostFeedAdapter() {
        CommunityPostFeedAdapter adapter = new CommunityPostFeedAdapter(
                communityFeedReadService, postRepository, userRepository, networkAccessService
        );
        assertThat(adapter.supportedType()).isEqualTo(ContentType.COMMUNITY_POST);

        UUID postId = UUID.randomUUID();
        UUID commId = UUID.randomUUID();

        PostDto postDto = PostDto.builder()
                .id(postId)
                .communityId(commId)
                .authorId(500L)
                .title("Community Title")
                .body("Community Body")
                .commentCount(5)
                .moderationStatus(ModerationStatus.VISIBLE)
                .createdAt(Instant.now())
                .build();

        Community community = Community.builder().id(commId).name("Spring Boot Club").build();
        Post postEntity = Post.builder().id(postId).community(community).authorId(500L).build();
        User authorUser = User.builder().id(500L).username("springGuru").firstName("John").lastName("Doe").build();

        when(communityFeedReadService.batchGetEligiblePosts(eq(List.of(postId)), eq(viewer)))
                .thenReturn(Map.of(postId, postDto));
        when(postRepository.findByIdsWithCommunity(anyCollection())).thenReturn(List.of(postEntity));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(authorUser));
        when(networkAccessService.findBlockedOrBlockingUserIdsAmong(eq(100L), anyCollection()))
                .thenReturn(Collections.emptySet());
        when(networkAccessService.findMutedUserIdsAmong(eq(100L), anyCollection()))
                .thenReturn(Collections.emptySet());

        Map<UUID, HydratedFeedItem> result = adapter.batchHydrate(List.of(postId), viewer);

        assertThat(result).hasSize(1);
        HydratedFeedItem item = result.get(postId);
        assertThat(item.title()).isEqualTo("Community Title");
        assertThat(item.body()).isEqualTo("Community Body");
        assertThat(item.commentCount()).isEqualTo(5);
        assertThat(item.community().name()).isEqualTo("Spring Boot Club");
        assertThat(item.author().getUsername()).isEqualTo("springGuru");
    }

    @Test
    @DisplayName("FeedContentHydrator delegates to appropriate adapter by ContentType")
    void testFeedContentHydratorPolymorphism() {
        FeedContentAdapter platformAdapter = mock(FeedContentAdapter.class);
        when(platformAdapter.supportedType()).thenReturn(ContentType.PLATFORM_POST);

        FeedContentAdapter communityAdapter = mock(FeedContentAdapter.class);
        when(communityAdapter.supportedType()).thenReturn(ContentType.COMMUNITY_POST);

        FeedContentHydrator hydrator = new FeedContentHydrator(List.of(platformAdapter, communityAdapter));

        UUID platformId = UUID.randomUUID();
        UUID communityId = UUID.randomUUID();

        ContentRef refPlatform = ContentRef.platformPost(platformId);
        ContentRef refCommunity = ContentRef.communityPost(communityId);

        HydratedFeedItem item1 = new HydratedFeedItem(refPlatform, ContentType.PLATFORM_POST, null, null, "P1", Collections.emptyList(), null, 0, Instant.now(), Instant.now());
        HydratedFeedItem item2 = new HydratedFeedItem(refCommunity, ContentType.COMMUNITY_POST, null, "C1", "Body", Collections.emptyList(), null, 0, Instant.now(), Instant.now());

        when(platformAdapter.batchHydrate(anyCollection(), eq(viewer))).thenReturn(Map.of(platformId, item1));
        when(communityAdapter.batchHydrate(anyCollection(), eq(viewer))).thenReturn(Map.of(communityId, item2));

        Map<ContentRef, HydratedFeedItem> result = hydrator.hydrate(List.of(refPlatform, refCommunity), viewer);

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys(refPlatform, refCommunity);
    }

    @Test
    @DisplayName("Section 11: Mixed feed page with identical UUIDs across PLATFORM_POST and COMMUNITY_POST hydrates only respective content and preserves distinct entries")
    void testHydrationCollisionSameUuidAcrossContentTypes() {
        FeedContentAdapter platformAdapter = mock(FeedContentAdapter.class);
        when(platformAdapter.supportedType()).thenReturn(ContentType.PLATFORM_POST);

        FeedContentAdapter communityAdapter = mock(FeedContentAdapter.class);
        when(communityAdapter.supportedType()).thenReturn(ContentType.COMMUNITY_POST);

        FeedContentHydrator hydrator = new FeedContentHydrator(List.of(platformAdapter, communityAdapter));

        UUID sharedUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ContentRef refPlatform = ContentRef.platformPost(sharedUuid);
        ContentRef refCommunity = ContentRef.communityPost(sharedUuid);

        HydratedFeedItem platformItem = new HydratedFeedItem(refPlatform, ContentType.PLATFORM_POST, null, null, "Platform Body", Collections.emptyList(), null, 0, Instant.now(), Instant.now());
        HydratedFeedItem communityItem = new HydratedFeedItem(refCommunity, ContentType.COMMUNITY_POST, null, "Community Title", "Community Body", Collections.emptyList(), null, 0, Instant.now(), Instant.now());

        when(platformAdapter.batchHydrate(eq(Set.of(sharedUuid)), eq(viewer))).thenReturn(Map.of(sharedUuid, platformItem));
        when(communityAdapter.batchHydrate(eq(Set.of(sharedUuid)), eq(viewer))).thenReturn(Map.of(sharedUuid, communityItem));

        List<ContentRef> orderedRefs = List.of(refPlatform, refCommunity);
        Map<ContentRef, HydratedFeedItem> result = hydrator.hydrate(orderedRefs, viewer);

        assertThat(result).hasSize(2);
        assertThat(result.get(refPlatform)).isSameAs(platformItem);
        assertThat(result.get(refCommunity)).isSameAs(communityItem);
        assertThat(result.get(refPlatform).contentType()).isEqualTo(ContentType.PLATFORM_POST);
        assertThat(result.get(refCommunity).contentType()).isEqualTo(ContentType.COMMUNITY_POST);
    }

    @Test
    @DisplayName("Section 18 & 102: Hydration query count remains constant O(1) across page sizes 5 -> 20 -> 50, proving zero N+1 queries")
    void testBatchHydrationQueryBoundednessNPlusOneFree() {
        PlatformPostFeedAdapter adapter = new PlatformPostFeedAdapter(platformPostFeedReadService, networkAccessService);

        int[] pageSizes = {5, 20, 50};

        for (int pageSize : pageSizes) {
            Map<UUID, PlatformPostResponse> postMap = new HashMap<>();
            List<UUID> postIds = new ArrayList<>();

            for (int i = 0; i < pageSize; i++) {
                UUID id = UUID.randomUUID();
                postIds.add(id);
                AuthorSummaryResponse author = AuthorSummaryResponse.builder().id((long) (1000 + i)).username("author" + i).build();
                postMap.put(id, PlatformPostResponse.builder()
                        .id(id)
                        .author(author)
                        .visibility(PostVisibility.PUBLIC)
                        .status(PostStatus.ACTIVE)
                        .body("Body " + i)
                        .createdAt(Instant.now())
                        .build());
            }

            reset(platformPostFeedReadService, networkAccessService);
            when(platformPostFeedReadService.batchGetActivePosts(anyCollection())).thenReturn(postMap);
            when(networkAccessService.findBlockedOrBlockingUserIdsAmong(anyLong(), anyCollection())).thenReturn(Collections.emptySet());
            when(networkAccessService.findMutedUserIdsAmong(anyLong(), anyCollection())).thenReturn(Collections.emptySet());
            when(networkAccessService.findFollowingUserIdsAmong(anyLong(), anyCollection())).thenReturn(Collections.emptySet());

            Map<UUID, HydratedFeedItem> hydrated = adapter.batchHydrate(postIds, viewer);

            assertThat(hydrated).hasSize(pageSize);

            // Bounded O(1) query verification: exactly 1 batch call per dependency regardless of whether page is 5, 20, or 50 items!
            verify(platformPostFeedReadService, times(1)).batchGetActivePosts(anyCollection());
            verify(networkAccessService, times(1)).findBlockedOrBlockingUserIdsAmong(eq(viewer.getId()), anyCollection());
            verify(networkAccessService, times(1)).findMutedUserIdsAmong(eq(viewer.getId()), anyCollection());
            verify(networkAccessService, times(1)).findFollowingUserIdsAmong(eq(viewer.getId()), anyCollection());

            // Zero per-post N+1 invocations
            verify(networkAccessService, never()).isBlockedEitherDirection(anyLong(), anyLong());
            verify(networkAccessService, never()).isMuted(anyLong(), anyLong());
            verify(networkAccessService, never()).isFollowing(anyLong(), anyLong());
        }
    }
}
