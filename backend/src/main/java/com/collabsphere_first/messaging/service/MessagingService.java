package com.collabsphere_first.messaging.service;

import com.collabsphere_first.messaging.dto.*;

import java.util.UUID;

public interface MessagingService {

    ConversationResponse getOrCreateDirectConversation(Long currentUserId, Long targetUserId);

    ConversationResponse createGroupConversation(Long currentUserId, String title, java.util.List<Long> memberIds);

    ConversationPageResponse getConversations(Long currentUserId, String cursor, int limit);

    ConversationResponse getConversation(UUID conversationId, Long currentUserId);

    MessagePageResponse getMessages(UUID conversationId, Long currentUserId, String cursor, int limit);

    MessageResponse sendMessage(UUID conversationId, Long currentUserId, SendMessageRequest request);

    MessageResponse editMessage(UUID messageId, Long currentUserId, EditMessageRequest request);

    void deleteMessage(UUID messageId, Long currentUserId);

    ReactionResponse addOrUpdateReaction(UUID messageId, Long currentUserId, ReactionRequest request);

    void removeReaction(UUID messageId, Long currentUserId);

    void markDelivered(UUID messageId, Long currentUserId);

    void markConversationRead(UUID conversationId, Long currentUserId, ReadReceiptRequest request);

    void muteConversation(UUID conversationId, Long currentUserId, boolean mute);

    void archiveConversation(UUID conversationId, Long currentUserId, boolean archive);

    AttachmentSignedUrlResponse getAttachmentSignedUrl(UUID conversationId, UUID attachmentId, Long currentUserId);

    void starMessage(UUID conversationId, UUID messageId, Long currentUserId);

    void unstarMessage(UUID conversationId, UUID messageId, Long currentUserId);

    java.util.List<java.util.Map<String, Object>> getStarredMessages(Long currentUserId);

    java.util.List<java.util.Map<String, Object>> getConversationMembers(UUID conversationId, Long currentUserId);

    void addConversationMember(UUID conversationId, Long currentUserId, Long profileId);

    void removeConversationMember(UUID conversationId, Long currentUserId, Long profileId);
}
