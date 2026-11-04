package com.collabsphere_first.security;

import com.collabsphere_first.messaging.exception.MessagingAccessDeniedException;
import com.collabsphere_first.messaging.repository.ConversationParticipantRepository;
import com.collabsphere_first.messaging.repository.ConversationRepository;
import com.collabsphere_first.messaging.security.MessagingAccessService;
import com.collabsphere_first.messaging.service.MessagingServiceImpl;
import com.collabsphere_first.notification.entity.Notification;
import com.collabsphere_first.notification.exception.NotificationAccessDeniedException;
import com.collabsphere_first.notification.repository.NotificationRepository;
import com.collabsphere_first.notification.service.NotificationServiceImpl;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.exception.PlatformPostAccessDeniedException;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.post.service.impl.PlatformPostServiceImpl;
import com.collabsphere_first.profile.dto.request.CreateProfileRequest;
import com.collabsphere_first.profile.service.ProfileServiceImpl;
import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.role.repository.RoleRepository;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Platform Authorization Matrix & IDOR Protection Tests")
public class PlatformAuthorizationMatrixTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PlatformPostRepository platformPostRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private MessagingAccessService messagingAccessService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @InjectMocks
    private PlatformPostServiceImpl postService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @InjectMocks
    private MessagingServiceImpl messagingService;

    private User regularUserA;
    private User regularUserB;
    private User platformAdmin;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1L).name(RoleName.ROLE_STUDENT).build();
        Role adminRole = Role.builder().id(99L).name(RoleName.ROLE_ADMIN).build();

        regularUserA = User.builder()
                .id(101L)
                .username("user_a")
                .email("user_a@collobster.com")
                .accountEnabled(true)
                .deactivated(false)
                .roles(Set.of(studentRole))
                .build();

        regularUserB = User.builder()
                .id(102L)
                .username("user_b")
                .email("user_b@collobster.com")
                .accountEnabled(true)
                .deactivated(false)
                .roles(Set.of(studentRole))
                .build();

        platformAdmin = User.builder()
                .id(999L)
                .username("admin_user")
                .email("admin@collobster.com")
                .accountEnabled(true)
                .deactivated(false)
                .roles(Set.of(adminRole))
                .build();
    }

    @Test
    @DisplayName("Self-assignment of ROLE_ADMIN must be blocked in createProfile")
    void testSelfAssignmentOfAdminRoleBlockedInCreateProfile() {
        when(currentUserService.getCurrentUser()).thenReturn(regularUserA);

        CreateProfileRequest request = new CreateProfileRequest();
        request.setRole(RoleName.ROLE_ADMIN);

        assertThrows(IllegalArgumentException.class, () -> profileService.createProfile(request));
    }

    @Test
    @DisplayName("Self-assignment of ROLE_ADMIN must be blocked in updateProfile")
    void testSelfAssignmentOfAdminRoleBlockedInUpdateProfile() {
        when(currentUserService.getCurrentUser()).thenReturn(regularUserA);
        when(userRepository.findById(101L)).thenReturn(Optional.of(regularUserA));

        CreateProfileRequest request = new CreateProfileRequest();
        request.setRole(RoleName.ROLE_ADMIN);

        assertThrows(IllegalArgumentException.class, () -> profileService.updateProfile(request));
    }

    @Test
    @DisplayName("PlatformPost author can delete their own post")
    void testAuthorCanDeleteOwnPost() {
        when(currentUserService.getCurrentUser()).thenReturn(regularUserA);

        UUID postId = UUID.randomUUID();
        PlatformPost post = PlatformPost.builder()
                .id(postId)
                .author(regularUserA)
                .status(PostStatus.ACTIVE)
                .visibility(PostVisibility.PUBLIC)
                .build();

        when(platformPostRepository.findById(postId)).thenReturn(Optional.of(post));

        assertDoesNotThrow(() -> postService.deletePost(postId));
        verify(platformPostRepository).save(argThat(p -> p.getStatus() == PostStatus.DELETED));
    }

    @Test
    @DisplayName("Platform Administrator (ROLE_ADMIN) can delete any user's post")
    void testAdminCanDeleteAnyPost() {
        when(currentUserService.getCurrentUser()).thenReturn(platformAdmin);

        UUID postId = UUID.randomUUID();
        PlatformPost post = PlatformPost.builder()
                .id(postId)
                .author(regularUserA)
                .status(PostStatus.ACTIVE)
                .visibility(PostVisibility.PUBLIC)
                .build();

        when(platformPostRepository.findById(postId)).thenReturn(Optional.of(post));

        assertDoesNotThrow(() -> postService.deletePost(postId));
        verify(platformPostRepository).save(argThat(p -> p.getStatus() == PostStatus.DELETED));
    }

    @Test
    @DisplayName("Unauthorized user CANNOT delete another user's post (IDOR protection)")
    void testUnauthorizedUserCannotDeleteOtherUserPost() {
        when(currentUserService.getCurrentUser()).thenReturn(regularUserB);

        UUID postId = UUID.randomUUID();
        PlatformPost post = PlatformPost.builder()
                .id(postId)
                .author(regularUserA)
                .status(PostStatus.ACTIVE)
                .visibility(PostVisibility.PUBLIC)
                .build();

        when(platformPostRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThrows(PlatformPostAccessDeniedException.class, () -> postService.deletePost(postId));
        verify(platformPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("User cannot access notifications of another user (IDOR protection)")
    void testNotificationAccessDeniedForNonRecipient() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .recipientUserId(regularUserA.getId())
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // User B tries to fetch User A's notification
        assertThrows(NotificationAccessDeniedException.class, () ->
                notificationService.getNotification(notificationId, regularUserB.getId()));
    }

    @Test
    @DisplayName("User cannot access direct conversation if not a participant (IDOR protection)")
    void testMessagingAccessDeniedForNonParticipant() {
        UUID conversationId = UUID.randomUUID();

        doThrow(new MessagingAccessDeniedException("You are not a participant in this conversation"))
                .when(messagingAccessService).requireParticipant(conversationId, regularUserB.getId());

        assertThrows(MessagingAccessDeniedException.class, () ->
                messagingService.getMessages(conversationId, regularUserB.getId(), null, 20));
    }
}
