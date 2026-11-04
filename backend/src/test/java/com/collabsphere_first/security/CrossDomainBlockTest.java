package com.collabsphere_first.security;

import com.collabsphere_first.messaging.entity.Conversation;
import com.collabsphere_first.messaging.entity.ConversationParticipant;
import com.collabsphere_first.messaging.entity.ConversationParticipantId;
import com.collabsphere_first.messaging.enums.ConversationType;
import com.collabsphere_first.messaging.exception.BlockedUserException;
import com.collabsphere_first.messaging.repository.ConversationParticipantRepository;
import com.collabsphere_first.messaging.repository.ConversationRepository;
import com.collabsphere_first.messaging.security.MessagingAccessServiceImpl;
import com.collabsphere_first.network.exception.NetworkAccessDeniedException;
import com.collabsphere_first.network.repository.UserBlockRepository;
import com.collabsphere_first.network.repository.UserConnectionRepository;
import com.collabsphere_first.network.repository.UserFollowRepository;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.network.service.NetworkServiceImpl;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.exception.PlatformPostAccessDeniedException;
import com.collabsphere_first.post.mapper.PlatformPostMapper;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.post.service.impl.PlatformPostServiceImpl;
import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Cross-Domain Block & Mute Semantics Tests")
public class CrossDomainBlockTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private UserConnectionRepository userConnectionRepository;

    @Mock
    private NetworkAccessService networkAccessService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private PlatformPostRepository platformPostRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private PlatformPostMapper platformPostMapper = new PlatformPostMapper();

    @InjectMocks
    private NetworkServiceImpl networkService;

    @InjectMocks
    private MessagingAccessServiceImpl messagingAccessService;

    @InjectMocks
    private PlatformPostServiceImpl platformPostService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1L).name(RoleName.ROLE_STUDENT).build();

        userA = User.builder()
                .id(101L)
                .username("user_a")
                .accountEnabled(true)
                .roles(Set.of(studentRole))
                .build();

        userB = User.builder()
                .id(202L)
                .username("user_b")
                .accountEnabled(true)
                .roles(Set.of(studentRole))
                .build();

        when(userRepository.findById(101L)).thenReturn(Optional.of(userA));
        when(userRepository.findById(202L)).thenReturn(Optional.of(userB));
    }

    @Test
    @DisplayName("User B cannot follow User A when block relationship exists")
    void testFollowBlockedWhenBlockedEitherDirection() {
        when(userBlockRepository.isBlockedEitherDirection(202L, 101L)).thenReturn(true);

        assertThrows(NetworkAccessDeniedException.class, () ->
                networkService.followUser(202L, 101L));
    }

    @Test
    @DisplayName("User B cannot send connection request to User A when block relationship exists")
    void testConnectionRequestBlockedWhenBlockedEitherDirection() {
        when(userBlockRepository.isBlockedEitherDirection(202L, 101L)).thenReturn(true);

        assertThrows(NetworkAccessDeniedException.class, () ->
                networkService.sendConnectionRequest(202L, 101L));
    }

    @Test
    @DisplayName("User B cannot initiate direct conversation with User A when block relationship exists")
    void testDirectMessageConversationInitiationBlocked() {
        when(networkAccessService.isBlockedEitherDirection(202L, 101L)).thenReturn(true);

        assertThrows(BlockedUserException.class, () ->
                messagingAccessService.validateCanInitiateDirectConversation(202L, 101L));
    }

    @Test
    @DisplayName("User B cannot send message in conversation when block relationship exists")
    void testDirectMessageSendingBlockedWhenBlocked() {
        UUID conversationId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByIdConversationIdAndIdUserId(conversationId, 202L)).thenReturn(true);

        ConversationParticipant p1 = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, 202L))
                .build();
        ConversationParticipant p2 = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, 101L))
                .build();

        when(participantRepository.findByIdConversationId(conversationId)).thenReturn(List.of(p1, p2));
        when(networkAccessService.isBlockedEitherDirection(202L, 101L)).thenReturn(true);

        assertThrows(BlockedUserException.class, () ->
                messagingAccessService.validateCanSendMessage(conversationId, 202L));
    }

    @Test
    @DisplayName("User B cannot react to message when block relationship exists")
    void testDirectMessageReactingBlockedWhenBlocked() {
        UUID conversationId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByIdConversationIdAndIdUserId(conversationId, 202L)).thenReturn(true);

        ConversationParticipant p1 = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, 202L))
                .build();
        ConversationParticipant p2 = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, 101L))
                .build();

        when(participantRepository.findByIdConversationId(conversationId)).thenReturn(List.of(p1, p2));
        when(networkAccessService.isBlockedEitherDirection(202L, 101L)).thenReturn(true);

        assertThrows(BlockedUserException.class, () ->
                messagingAccessService.validateCanReact(conversationId, 202L));
    }

    @Test
    @DisplayName("User B cannot send typing indicator when block relationship exists")
    void testDirectMessageTypingBlockedWhenBlocked() {
        UUID conversationId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByIdConversationIdAndIdUserId(conversationId, 202L)).thenReturn(true);

        ConversationParticipant p1 = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, 202L))
                .build();
        ConversationParticipant p2 = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, 101L))
                .build();

        when(participantRepository.findByIdConversationId(conversationId)).thenReturn(List.of(p1, p2));
        when(networkAccessService.isBlockedEitherDirection(202L, 101L)).thenReturn(true);

        assertThrows(BlockedUserException.class, () ->
                messagingAccessService.validateCanSendTyping(conversationId, 202L));
    }

    @Test
    @DisplayName("User B cannot view User A's PUBLIC post when block relationship exists")
    void testPublicPostViewingBlockedWhenBlocked() {
        UUID postId = UUID.randomUUID();
        PlatformPost post = PlatformPost.builder()
                .id(postId)
                .author(userA)
                .status(PostStatus.ACTIVE)
                .visibility(PostVisibility.PUBLIC)
                .build();

        when(platformPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(currentUserService.getCurrentUser()).thenReturn(userB);
        when(networkAccessService.isBlockedEitherDirection(202L, 101L)).thenReturn(true);

        assertThrows(PlatformPostAccessDeniedException.class, () ->
                platformPostService.getPostById(postId));
    }

    @Test
    @DisplayName("Unblocking restores interaction eligibility")
    void testUnblockRestoresEligibility() {
        when(userBlockRepository.isBlockedEitherDirection(202L, 101L)).thenReturn(false);
        when(networkAccessService.isBlockedEitherDirection(202L, 101L)).thenReturn(false);

        // Can initiate conversation without BlockedUserException
        assertDoesNotThrow(() ->
                messagingAccessService.validateCanInitiateDirectConversation(202L, 101L));
    }
}
