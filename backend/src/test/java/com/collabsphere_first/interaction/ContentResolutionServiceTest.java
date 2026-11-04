package com.collabsphere_first.interaction;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.interaction.exception.ContentInaccessibleException;
import com.collabsphere_first.interaction.resolver.ContentResolutionService;
import com.collabsphere_first.interaction.resolver.ContentResolver;
import com.collabsphere_first.interaction.resolver.ResolvedContent;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentResolutionService Tests")
class ContentResolutionServiceTest {

    @Mock
    private ContentResolver platformResolver;

    @Mock
    private ContentResolver communityResolver;

    @Mock
    private NetworkAccessService networkAccessService;

    private ContentResolutionService resolutionService;

    private User viewer;
    private UUID contentId;
    private ContentRef platformRef;

    @BeforeEach
    void setUp() {
        lenient().when(platformResolver.supports(ContentType.PLATFORM_POST)).thenReturn(true);
        lenient().when(communityResolver.supports(ContentType.COMMUNITY_POST)).thenReturn(true);

        resolutionService = new ContentResolutionService(
                List.of(platformResolver, communityResolver),
                networkAccessService
        );

        viewer = User.builder().id(100L).firstName("Viewer").lastName("User").build();
        contentId = UUID.randomUUID();
        platformRef = ContentRef.platformPost(contentId);
    }

    @Test
    @DisplayName("Resolves accessible content when no block exists")
    void resolveVisible_Success() {
        ResolvedContent expected = ResolvedContent.of(platformRef, 200L, null, "Summary text");
        when(platformResolver.resolveVisible(platformRef, viewer)).thenReturn(expected);
        when(networkAccessService.isBlockedEitherDirection(100L, 200L)).thenReturn(false);

        ResolvedContent result = resolutionService.resolveVisible(platformRef, viewer);

        assertNotNull(result);
        assertTrue(result.accessible());
        assertEquals(200L, result.authorId());
    }

    @Test
    @DisplayName("Rejects content when author and viewer have an active block")
    void resolveVisible_BlockedRelationship_ThrowsException() {
        ResolvedContent content = ResolvedContent.of(platformRef, 200L, null, "Summary text");
        when(platformResolver.resolveVisible(platformRef, viewer)).thenReturn(content);
        when(networkAccessService.isBlockedEitherDirection(100L, 200L)).thenReturn(true);

        ContentInaccessibleException ex = assertThrows(ContentInaccessibleException.class,
                () -> resolutionService.resolveVisible(platformRef, viewer));
        assertEquals("Interaction not permitted due to an active block relationship.", ex.getMessage());
    }

    @Test
    @DisplayName("Rejects inaccessible or deleted content from resolver")
    void resolveVisible_InaccessibleFromResolver_ThrowsException() {
        when(platformResolver.resolveVisible(platformRef, viewer))
                .thenReturn(ResolvedContent.inaccessible(platformRef));

        assertThrows(ContentInaccessibleException.class,
                () -> resolutionService.resolveVisible(platformRef, viewer));
    }
}
