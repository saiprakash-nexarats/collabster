package com.collabsphere_first.messaging.service;

import com.collabsphere_first.integration.storage.StorageService;
import com.collabsphere_first.messaging.config.MessagingProperties;
import com.collabsphere_first.messaging.dto.*;
import com.collabsphere_first.messaging.entity.*;
import com.collabsphere_first.messaging.enums.ConversationType;
import com.collabsphere_first.messaging.enums.MessageDeliveryState;
import com.collabsphere_first.messaging.enums.MessageStatus;
import com.collabsphere_first.messaging.enums.MessageType;
import com.collabsphere_first.messaging.event.DirectMessageReactionEvent;
import com.collabsphere_first.messaging.exception.*;
import com.collabsphere_first.messaging.mapper.MessagingMapper;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.messaging.repository.*;
import com.collabsphere_first.messaging.security.MessagingAccessService;
import com.collabsphere_first.messaging.cursor.MessagingCursorCodec;
import com.collabsphere_first.messaging.security.MessagingRateLimiter;
import com.collabsphere_first.messaging.websocket.MessagingRealtimePublisher;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingServiceImpl implements MessagingService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final DirectMessageRepository messageRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final MessageReactionRepository reactionRepository;
    private final MessageReceiptRepository receiptRepository;
    private final UserRepository userRepository;

    private final MessagingAccessService accessService;
    private final MessagingRealtimePublisher realtimePublisher;
    private final MessagingAttachmentService attachmentService;
    private final MessagingRateLimiter rateLimiter;
    private final MessagingPresenceService presenceService;
    private final MessagingCursorCodec cursorCodec;
    private final MessagingMapper mapper;
    private final MessagingProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    private final IdempotentMessagePersister persister;
    private final ObjectProvider<StorageService> storageServiceProvider;

    @Override
    @Transactional
    public ConversationResponse getOrCreateDirectConversation(Long currentUserId, Long targetUserId) {
        rateLimiter.checkConversationCreation(currentUserId);
        accessService.validateCanInitiateDirectConversation(currentUserId, targetUserId);

        long low = Math.min(currentUserId, targetUserId);
        long high = Math.max(currentUserId, targetUserId);

        Conversation conversation = conversationRepository.findByDirectUserLowIdAndDirectUserHighId(low, high)
                .orElse(null);

        if (conversation == null) {
            Conversation newConv = Conversation.builder()
                    .type(ConversationType.DIRECT)
                    .directUserLowId(low)
                    .directUserHighId(high)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            conversation = conversationRepository.saveAndFlush(newConv);

            User user1 = userRepository.getReferenceById(currentUserId);
            User user2 = userRepository.getReferenceById(targetUserId);

            ConversationParticipant part1 = ConversationParticipant.builder()
                    .id(new ConversationParticipantId(conversation.getId(), currentUserId))
                    .conversation(conversation)
                    .user(user1)
                    .joinedAt(Instant.now())
                    .build();

            ConversationParticipant part2 = ConversationParticipant.builder()
                    .id(new ConversationParticipantId(conversation.getId(), targetUserId))
                    .conversation(conversation)
                    .user(user2)
                    .joinedAt(Instant.now())
                    .build();

            participantRepository.saveAll(List.of(part1, part2));
            log.info("Created new DIRECT conversation {} between {} and {}", conversation.getId(), currentUserId, targetUserId);
        }

        return mapConversationResponse(conversation, currentUserId);
    }

    @Override
    @Transactional
    public ConversationResponse createGroupConversation(Long currentUserId, String title, List<Long> memberIds) {
        rateLimiter.checkConversationCreation(currentUserId);
        Conversation newConv = Conversation.builder()
                .type(ConversationType.GROUP)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Conversation conversation = conversationRepository.saveAndFlush(newConv);

        Set<Long> allUserIds = new HashSet<>();
        allUserIds.add(currentUserId);
        if (memberIds != null) {
            allUserIds.addAll(memberIds);
        }

        List<ConversationParticipant> participants = new ArrayList<>();
        for (Long userId : allUserIds) {
            User user = userRepository.getReferenceById(userId);
            participants.add(ConversationParticipant.builder()
                    .id(new ConversationParticipantId(conversation.getId(), userId))
                    .conversation(conversation)
                    .user(user)
                    .joinedAt(Instant.now())
                    .build());
        }
        participantRepository.saveAll(participants);
        log.info("Created new GROUP conversation {} with {} members", conversation.getId(), allUserIds.size());
        return mapConversationResponse(conversation, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationPageResponse getConversations(Long currentUserId, String cursor, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), properties.getMaxPageSize());
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<Conversation> conversations;
        if (cursor == null || cursor.isBlank()) {
            conversations = conversationRepository.findInitialConversations(currentUserId, false, pageable);
        } else {
            com.collabsphere_first.messaging.cursor.ConversationCursor cursorData = cursorCodec.decodeConversationCursor(cursor);
            conversations = conversationRepository.findConversationsAfterCursor(
                    currentUserId, false, cursorData.lastMessageAt(), cursorData.conversationId(), pageable
            );
        }

        boolean hasMore = conversations.size() > pageSize;
        List<Conversation> pageItems = hasMore ? conversations.subList(0, pageSize) : conversations;

        String nextCursor = null;
        if (hasMore && !pageItems.isEmpty()) {
            Conversation last = pageItems.get(pageItems.size() - 1);
            Instant cursorTime = last.getLastMessageAt() != null ? last.getLastMessageAt() : last.getCreatedAt();
            nextCursor = cursorCodec.encodeConversationCursor(new com.collabsphere_first.messaging.cursor.ConversationCursor(cursorTime, last.getId()));
        }

        List<ConversationResponse> responses = pageItems.stream()
                .map(c -> mapConversationResponse(c, currentUserId))
                .toList();

        return new ConversationPageResponse(responses, nextCursor, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, Long currentUserId) {
        accessService.requireParticipant(conversationId, currentUserId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        return mapConversationResponse(conversation, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public MessagePageResponse getMessages(UUID conversationId, Long currentUserId, String cursor, int limit) {
        accessService.requireParticipant(conversationId, currentUserId);

        int pageSize = Math.min(Math.max(limit, 1), properties.getMaxPageSize());
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<DirectMessage> messages;
        if (cursor == null || cursor.isBlank()) {
            messages = messageRepository.findInitialMessages(conversationId, pageable);
        } else {
            com.collabsphere_first.messaging.cursor.MessageCursor cursorData = cursorCodec.decodeMessageCursor(cursor);
            messages = messageRepository.findMessagesAfterCursor(
                    conversationId, cursorData.createdAt(), cursorData.messageId(), pageable
            );
        }

        boolean hasMore = messages.size() > pageSize;
        List<DirectMessage> pageItems = hasMore ? messages.subList(0, pageSize) : messages;

        String nextCursor = null;
        if (hasMore && !pageItems.isEmpty()) {
            DirectMessage last = pageItems.get(pageItems.size() - 1);
            nextCursor = cursorCodec.encodeMessageCursor(new com.collabsphere_first.messaging.cursor.MessageCursor(last.getCreatedAt(), last.getId()));
        }

        List<MessageResponse> responses = hydrateMessages(pageItems, currentUserId);
        return new MessagePageResponse(responses, nextCursor, hasMore);
    }

//    @Override
//    public MessageResponse sendMessage(UUID conversationId, Long currentUserId, SendMessageRequest request) {
//        rateLimiter.checkMessageSend(currentUserId);
//        accessService.validateCanSendMessage(conversationId, currentUserId);
//
//        // Pre-check idempotency if clientMessageId is present
//        if (request.clientMessageId() != null) {
//            Optional<DirectMessage> existing = messageRepository.findByConversationIdAndSenderIdAndClientMessageId(
//                    conversationId, currentUserId, request.clientMessageId()
//            );
//            if (existing.isPresent()) {
//                log.info("Idempotent hit: message already exists for clientMessageId {}", request.clientMessageId());
//                validatePayloadEquality(existing.get(), request);
//                return hydrateSingleMessage(existing.get(), currentUserId);
//            }
//        }
//
//        boolean hasBody = request.body() != null && !request.body().trim().isBlank();
//        boolean hasAttachments = request.attachments() != null && !request.attachments().isEmpty();
//
//        if (!hasBody && !hasAttachments) {
//            throw new IllegalArgumentException("Message must contain either a non-blank body or at least one attachment");
//        }
//
//        if (hasBody && request.body().length() > properties.getMaxMessageBodyLength()) {
//            throw new IllegalArgumentException("Message body exceeds maximum allowed length of " +
//                    properties.getMaxMessageBodyLength());
//        }
//
//        // Validate attachment ownership and syntax
//        attachmentService.validateAttachments(request.attachments(), currentUserId);
//
//        if (request.replyToMessageId() != null) {
//            DirectMessage replyTarget = messageRepository.findById(request.replyToMessageId())
//                    .orElseThrow(() -> new MessageNotFoundException("Replied message not found: " + request.replyToMessageId()));
//            if (!replyTarget.getConversationId().equals(conversationId)) {
//                throw new IllegalArgumentException("Cannot reply to a message from a different conversation");
//            }
//        }
//
//        MessageType messageType;
//        if (hasBody && hasAttachments) {
//            messageType = MessageType.MIXED;
//        } else if (hasAttachments) {
//            messageType = MessageType.ATTACHMENT;
//        } else {
//            messageType = MessageType.TEXT;
//        }
//
//        Long recipientId = accessService.getOtherParticipantId(conversationId, currentUserId);
//
//        PersistMessageResult result;
//        try {
//            result = persister.persistMessage(conversationId, currentUserId, recipientId, request, messageType, hasBody);
//        } catch (ConcurrentIdempotentSendException ex) {
//            DirectMessage existing = messageRepository.findByConversationIdAndSenderIdAndClientMessageId(
//                    conversationId, currentUserId, request.clientMessageId()
//            ).orElseThrow(() -> new IllegalStateException("Expected existing message on idempotency conflict", ex));
//            result = new PersistMessageResult(existing, false);
//        }
//
//        if (!result.isNew()) {
//            log.info("Idempotent hit: message already exists for clientMessageId {}", request.clientMessageId());
//            validatePayloadEquality(result.message(), request);
//            return hydrateSingleMessage(result.message(), currentUserId);
//        }
//
//        DirectMessage message = result.message();
//        MessageResponse response = hydrateSingleMessage(message, currentUserId);
//
//        // Note: DirectMessageCreatedEvent is published inside persister.persistMessage transaction for atomic durability
//
//        // Realtime WebSocket delivery for initial creation ONLY (safe from rollback)
//        String senderEmail = userRepository.findById(currentUserId).map(User::getEmail).orElse(null);
//        String recipientEmail = recipientId != null ? userRepository.findById(recipientId).map(User::getEmail).orElse(null) : null;
//        try {
//            realtimePublisher.publishMessageCreated(response, recipientEmail, senderEmail);
//        } catch (Exception ex) {
//            log.error("Failed to publish realtime message created event: {}", ex.getMessage(), ex);
//        }
//
//        return response;
//    }

    @Override
    public MessageResponse sendMessage(UUID conversationId, Long currentUserId, SendMessageRequest request){
        log.info("1. Starting sendMessage: conversationId={}, userId={}, clientMessageId={}",
                conversationId, currentUserId, request.clientMessageId());

        rateLimiter.checkMessageSend(currentUserId);
        log.info("2. Rate limiter passed");

        accessService.validateCanSendMessage(conversationId, currentUserId);
        log.info("3. Access validation passed");

        if (request.clientMessageId() != null) {
            Optional<DirectMessage> existing =
                    messageRepository.findByConversationIdAndSenderIdAndClientMessageId(
                            conversationId,
                            currentUserId,
                            request.clientMessageId()
                    );

            log.info("4. Idempotency check completed, existing={}", existing.isPresent());

            if (existing.isPresent()) {
                log.info("Existing message found");
                validatePayloadEquality(existing.get(), request);
                return hydrateSingleMessage(existing.get(), currentUserId);
            }
        }

        boolean hasBody = request.body() != null &&
                !request.body().trim().isBlank();

        boolean hasAttachments = request.attachments() != null &&
                !request.attachments().isEmpty();

        log.info("5. Payload validation: hasBody={}, hasAttachments={}",
                hasBody, hasAttachments);

        if (!hasBody && !hasAttachments) {
            throw new IllegalArgumentException(
                    "Message must contain either a non-blank body or at least one attachment"
            );
        }

        if (hasBody &&
                request.body().length() > properties.getMaxMessageBodyLength()) {
            throw new IllegalArgumentException(
                    "Message body exceeds maximum allowed length of " +
                            properties.getMaxMessageBodyLength()
            );
        }

        if (hasAttachments) {
            log.info("6. Validating attachments");
            attachmentService.validateAttachments(
                    request.attachments(),
                    currentUserId
            );
            log.info("7. Attachment validation passed");
        }

        if (request.replyToMessageId() != null) {
            log.info("8. Validating reply message");
            // existing reply validation...
        }

        log.info("9. Getting recipient");

        Long recipientId =
                accessService.getOtherParticipantId(
                        conversationId,
                        currentUserId
                );

        log.info("10. Recipient found: {}", recipientId);

        MessageType messageType;

        if (hasBody && hasAttachments) {
            messageType = MessageType.MIXED;
        } else if (hasAttachments) {
            messageType = MessageType.ATTACHMENT;
        } else {
            messageType = MessageType.TEXT;
        }

        log.info("11. Message type: {}", messageType);

        log.info("12. Persisting message");

        PersistMessageResult result =
                persister.persistMessage(
                        conversationId,
                        currentUserId,
                        recipientId,
                        request,
                        messageType,
                        hasBody
                );

        log.info("13. Message persisted: new={}", result.isNew());

        if (!result.isNew()) {
            log.info("Idempotent hit: message already exists for clientMessageId {}", request.clientMessageId());
            validatePayloadEquality(result.message(), request);
            return hydrateSingleMessage(result.message(), currentUserId);
        }

        DirectMessage message = result.message();
        MessageResponse response = hydrateSingleMessage(message, currentUserId);

        // Note: DirectMessageCreatedEvent is published inside persister.persistMessage transaction for atomic durability

        // Realtime WebSocket delivery for initial creation ONLY (safe from rollback)
        String senderEmail = userRepository.findById(currentUserId).map(User::getEmail).orElse(null);
        String recipientEmail = recipientId != null ? userRepository.findById(recipientId).map(User::getEmail).orElse(null) : null;
        try {
            realtimePublisher.publishMessageCreated(response, recipientEmail, senderEmail);
        } catch (Exception ex) {
            log.error("Failed to publish realtime message created event: {}", ex.getMessage(), ex);
        }

        return response;
    }

    private void validatePayloadEquality(DirectMessage existing, SendMessageRequest request) {
        String existingBody = existing.getBody() != null ? existing.getBody().trim() : null;
        String requestBody = request.body() != null && !request.body().trim().isBlank() ? request.body().trim() : null;
        if (!Objects.equals(existingBody, requestBody)) {
            throw new IdempotencyConflictException("IDEMPOTENCY_KEY_REUSED: clientMessageId is already in use with different body");
        }

        if (!Objects.equals(existing.getReplyToMessageId(), request.replyToMessageId())) {
            throw new IdempotencyConflictException("IDEMPOTENCY_KEY_REUSED: clientMessageId is already in use with different replyToMessageId");
        }

        List<MessageAttachment> existingAttachments = attachmentRepository.findByMessageIdOrderByDisplayOrderAsc(existing.getId());
        List<AttachmentRequest> requestAttachments = request.attachments() != null ? request.attachments() : List.of();

        if (existingAttachments.size() != requestAttachments.size()) {
            throw new IdempotencyConflictException("IDEMPOTENCY_KEY_REUSED: clientMessageId is already in use with different attachment count");
        }

        for (int i = 0; i < existingAttachments.size(); i++) {
            MessageAttachment existingAtt = existingAttachments.get(i);
            AttachmentRequest reqAtt = requestAttachments.get(i);

            if (existingAtt.getMediaType() != reqAtt.mediaType() ||
                    !existingAtt.getStorageKey().equals(reqAtt.storageKey().replace('\\', '/').trim()) ||
                    !existingAtt.getMimeType().equalsIgnoreCase(reqAtt.mimeType().trim())) {
                throw new IdempotencyConflictException("IDEMPOTENCY_KEY_REUSED: clientMessageId is already in use with different attachment payload");
            }
        }
    }

    @Override
    @Transactional
    public MessageResponse editMessage(UUID messageId, Long currentUserId, EditMessageRequest request) {
        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        if (message.isDeleted()) {
            throw new IllegalArgumentException("Cannot edit a deleted message");
        }

        if (!message.getSenderId().equals(currentUserId)) {
            throw new MessagingAccessDeniedException("Only the original sender can edit this message");
        }

        Instant editDeadline = message.getCreatedAt().plus(properties.getMessageEditWindow());
        if (Instant.now().isAfter(editDeadline)) {
            throw new IllegalArgumentException("Message can no longer be edited (edit window has passed)");
        }

        if (request.body() != null && request.body().length() > properties.getMaxMessageBodyLength()) {
            throw new IllegalArgumentException("Message body exceeds maximum allowed length of " +
                    properties.getMaxMessageBodyLength());
        }

        Instant now = Instant.now();
        message.setBody(request.body().trim());
        message.setEditedAt(now);
        message.setUpdatedAt(now);
        message = messageRepository.save(message);

        MessageResponse response = hydrateSingleMessage(message, currentUserId);

        Long recipientId = accessService.getOtherParticipantId(message.getConversationId(), currentUserId);
        String senderEmail = userRepository.findById(currentUserId).map(User::getEmail).orElse(null);
        String recipientEmail = recipientId != null ? userRepository.findById(recipientId).map(User::getEmail).orElse(null) : null;

        realtimePublisher.publishMessageUpdated(response, recipientEmail, senderEmail);

        return response;
    }

    @Override
    @Transactional
    public void deleteMessage(UUID messageId, Long currentUserId) {
        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        if (message.isDeleted()) {
            return;
        }

        if (!message.getSenderId().equals(currentUserId)) {
            throw new MessagingAccessDeniedException("Only the original sender can delete this message");
        }

        Instant now = Instant.now();
        message.setStatus(MessageStatus.DELETED);
        message.setDeletedAt(now);
        message.setUpdatedAt(now);
        messageRepository.save(message);

        Long recipientId = accessService.getOtherParticipantId(message.getConversationId(), currentUserId);
        String senderEmail = userRepository.findById(currentUserId).map(User::getEmail).orElse(null);
        String recipientEmail = recipientId != null ? userRepository.findById(recipientId).map(User::getEmail).orElse(null) : null;

        realtimePublisher.publishMessageDeleted(message.getConversationId(), messageId, recipientEmail, senderEmail);
    }

    @Override
    @Transactional
    public ReactionResponse addOrUpdateReaction(UUID messageId, Long currentUserId, ReactionRequest request) {
        rateLimiter.checkReaction(currentUserId);

        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        if (message.isDeleted()) {
            throw new IllegalArgumentException("Cannot react to a deleted message");
        }

        accessService.validateCanReact(message.getConversationId(), currentUserId);

        Instant now = Instant.now();
        Optional<MessageReaction> existing = reactionRepository.findByMessageIdAndUserId(messageId, currentUserId);

        MessageReaction reaction;
        if (existing.isPresent()) {
            reaction = existing.get();
            reaction.setReaction(request.reaction());
        } else {
            reaction = MessageReaction.builder()
                    .messageId(messageId)
                    .userId(currentUserId)
                    .reaction(request.reaction())
                    .createdAt(now)
                    .build();
        }
        reaction = reactionRepository.save(reaction);

        Long recipientId = accessService.getOtherParticipantId(message.getConversationId(), currentUserId);
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        ReactionResponse response = mapper.toReactionResponse(reaction, currentUser != null ? currentUser.getUsername() : String.valueOf(currentUserId));

        eventPublisher.publishEvent(new DirectMessageReactionEvent(
                message.getConversationId(),
                messageId,
                response,
                false,
                currentUserId,
                recipientId,
                now,
                UUID.randomUUID()
        ));

        String senderEmail = currentUser != null ? currentUser.getEmail() : null;
        String recipientEmail = recipientId != null ? userRepository.findById(recipientId).map(User::getEmail).orElse(null) : null;
        realtimePublisher.publishReactionUpdated(message.getConversationId(), messageId, response, false, recipientEmail, senderEmail);

        return response;
    }

    @Override
    @Transactional
    public void removeReaction(UUID messageId, Long currentUserId) {
        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        accessService.requireParticipant(message.getConversationId(), currentUserId);

        Optional<MessageReaction> existing = reactionRepository.findByMessageIdAndUserId(messageId, currentUserId);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());

            Long recipientId = accessService.getOtherParticipantId(message.getConversationId(), currentUserId);
            String senderEmail = userRepository.findById(currentUserId).map(User::getEmail).orElse(null);
            String recipientEmail = recipientId != null ? userRepository.findById(recipientId).map(User::getEmail).orElse(null) : null;

            eventPublisher.publishEvent(new DirectMessageReactionEvent(
                    message.getConversationId(),
                    messageId,
                    null,
                    true,
                    currentUserId,
                    recipientId,
                    Instant.now(),
                    UUID.randomUUID()
            ));

            realtimePublisher.publishReactionUpdated(message.getConversationId(), messageId, null, true, recipientEmail, senderEmail);
        }
    }

    @Override
    @Transactional
    public void markDelivered(UUID messageId, Long currentUserId) {
        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        accessService.requireParticipant(message.getConversationId(), currentUserId);

        if (message.getSenderId().equals(currentUserId)) {
            throw new MessagingAccessDeniedException("Sender cannot acknowledge delivery of own message");
        }

        MessageReceipt receipt = receiptRepository.findByIdMessageIdAndIdUserId(messageId, currentUserId)
                .orElseThrow(() -> new MessagingAccessDeniedException("Receipt record not found for recipient"));

        // Idempotent: preserve earliest known delivery time
        if (receipt.getDeliveredAt() == null) {
            Instant now = Instant.now();
            receipt.setDeliveredAt(now);
            receiptRepository.save(receipt);

            String senderEmail = userRepository.findById(message.getSenderId()).map(User::getEmail).orElse(null);
            realtimePublisher.publishDeliveryReceipt(message.getConversationId(), messageId, now, senderEmail);
        }
    }

    @Override
    @Transactional
    public void markConversationRead(UUID conversationId, Long currentUserId, ReadReceiptRequest request) {
        accessService.requireParticipant(conversationId, currentUserId);

        DirectMessage targetMessage = messageRepository.findById(request.lastReadMessageId())
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + request.lastReadMessageId()));

        if (!targetMessage.getConversationId().equals(conversationId)) {
            throw new IllegalArgumentException("Read cursor message does not belong to this conversation");
        }

        ConversationParticipant participant = participantRepository.findByIdConversationIdAndIdUserId(conversationId, currentUserId)
                .orElseThrow(() -> new IllegalStateException("Participant record missing"));

        // Canonical message-order comparison: advance only if target is strictly later
        if (participant.getLastReadMessageId() != null) {
            DirectMessage currentLastRead = messageRepository.findById(participant.getLastReadMessageId()).orElse(null);
            if (currentLastRead != null && compareMessageOrder(targetMessage, currentLastRead) <= 0) {
                // Target is equal or older than current cursor. Ignore idempotently.
                return;
            }
        }

        Instant now = Instant.now();
        participant.setLastReadMessageId(targetMessage.getId());
        participant.setLastReadAt(now); // Wall-clock timestamp when acknowledgment was processed
        participantRepository.save(participant);

        receiptRepository.markReadForUserUpTo(conversationId, currentUserId, targetMessage.getCreatedAt(), targetMessage.getId(), now);

        Long otherUserId = accessService.getOtherParticipantId(conversationId, currentUserId);
        String otherUserEmail = otherUserId != null ? userRepository.findById(otherUserId).map(User::getEmail).orElse(null) : null;
        realtimePublisher.publishReadReceipt(conversationId, targetMessage.getId(), now, otherUserEmail);
    }

    private int compareMessageOrder(DirectMessage m1, DirectMessage m2) {
        if (m1 == null && m2 == null) return 0;
        if (m1 == null) return -1;
        if (m2 == null) return 1;
        int timeCmp = m1.getCreatedAt().compareTo(m2.getCreatedAt());
        if (timeCmp != 0) {
            return timeCmp;
        }
        int cmp = Long.compareUnsigned(m1.getId().getMostSignificantBits(), m2.getId().getMostSignificantBits());
        if (cmp != 0) {
            return cmp;
        }
        return Long.compareUnsigned(m1.getId().getLeastSignificantBits(), m2.getId().getLeastSignificantBits());
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentSignedUrlResponse getAttachmentSignedUrl(UUID conversationId, UUID attachmentId, Long currentUserId) {
        accessService.requireParticipant(conversationId, currentUserId);

        MessageAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new MessageNotFoundException("Attachment not found: " + attachmentId));

        DirectMessage message = messageRepository.findById(attachment.getMessageId())
                .orElseThrow(() -> new MessageNotFoundException("Message not found for attachment"));

        if (!message.getConversationId().equals(conversationId)) {
            throw new IllegalArgumentException("Attachment does not belong to conversation");
        }

        StorageService storageService = storageServiceProvider.getIfAvailable();
        if (storageService == null) {
            throw new IllegalStateException("Storage service is not available");
        }

        String signedUrl = storageService.generateSignedUrl(attachment.getStorageKey(), 60);
        return new AttachmentSignedUrlResponse(attachment.getId(), attachment.getStorageKey(), signedUrl, 60);
    }

    @Override
    @Transactional
    public void muteConversation(UUID conversationId, Long currentUserId, boolean mute) {
        accessService.requireParticipant(conversationId, currentUserId);

        ConversationParticipant participant = participantRepository.findByIdConversationIdAndIdUserId(conversationId, currentUserId)
                .orElseThrow(() -> new IllegalStateException("Participant record missing"));

        participant.setNotificationsMuted(mute);
        participantRepository.save(participant);
    }

    @Override
    @Transactional
    public void archiveConversation(UUID conversationId, Long currentUserId, boolean archive) {
        accessService.requireParticipant(conversationId, currentUserId);

        ConversationParticipant participant = participantRepository.findByIdConversationIdAndIdUserId(conversationId, currentUserId)
                .orElseThrow(() -> new IllegalStateException("Participant record missing"));

        participant.setArchived(archive);
        participantRepository.save(participant);
    }

    private ConversationResponse mapConversationResponse(Conversation conversation, Long currentUserId) {
        Long otherUserId = conversation.getOtherParticipantId(currentUserId);
        User otherUser = otherUserId != null ? userRepository.findById(otherUserId).orElse(null) : null;
        boolean isOnline = otherUserId != null && presenceService.isUserOnline(otherUserId);
        ParticipantSummary otherParticipant = mapper.toParticipantSummary(otherUser, isOnline);

        ConversationParticipant participant = participantRepository.findByIdConversationIdAndIdUserId(conversation.getId(), currentUserId)
                .orElse(null);

        long unreadCount;
        if (participant == null) {
            unreadCount = 0;
        } else if (participant.getLastReadMessageId() == null) {
            unreadCount = messageRepository.countByConversationIdAndSenderIdNotAndStatus(
                    conversation.getId(), currentUserId, MessageStatus.ACTIVE
            );
        } else {
            DirectMessage lastRead = messageRepository.findById(participant.getLastReadMessageId()).orElse(null);
            if (lastRead != null) {
                unreadCount = messageRepository.countUnreadAfterCursor(
                        conversation.getId(), currentUserId, MessageStatus.ACTIVE, lastRead.getCreatedAt(), lastRead.getId()
                );
            } else {
                unreadCount = messageRepository.countByConversationIdAndSenderIdNotAndStatus(
                        conversation.getId(), currentUserId, MessageStatus.ACTIVE
                );
            }
        }

        MessageSummary lastMessage = null;
        if (conversation.getLastMessageId() != null) {
            lastMessage = messageRepository.findById(conversation.getLastMessageId())
                    .map(mapper::toMessageSummary)
                    .orElse(null);
        }

        return mapper.toConversationResponse(
                conversation,
                otherParticipant,
                lastMessage,
                unreadCount,
                participant != null && participant.isNotificationsMuted(),
                participant != null && participant.isArchived()
        );
    }

    private List<MessageResponse> hydrateMessages(List<DirectMessage> messages, Long currentUserId) {
        if (messages.isEmpty()) {
            return List.of();
        }

        List<UUID> messageIds = messages.stream().map(DirectMessage::getId).toList();

        Map<UUID, List<MessageAttachment>> attachmentMap = attachmentRepository.findByMessageIdInOrderByDisplayOrderAsc(messageIds).stream()
                .collect(Collectors.groupingBy(MessageAttachment::getMessageId));

        Map<UUID, List<MessageReaction>> reactionMap = reactionRepository.findByMessageIdIn(messageIds).stream()
                .collect(Collectors.groupingBy(MessageReaction::getMessageId));

        Map<UUID, List<MessageReceipt>> receiptMap = receiptRepository.findByIdMessageIdIn(messageIds).stream()
                .collect(Collectors.groupingBy(r -> r.getId().getMessageId()));

        Set<UUID> replyTargetIds = messages.stream()
                .map(DirectMessage::getReplyToMessageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, DirectMessage> replyMap = replyTargetIds.isEmpty() ? Map.of()
                : messageRepository.findAllById(replyTargetIds).stream()
                .collect(Collectors.toMap(DirectMessage::getId, m -> m));

        Set<Long> userIds = new HashSet<>();
        messages.forEach(m -> userIds.add(m.getSenderId()));
        replyMap.values().forEach(m -> userIds.add(m.getSenderId()));
        reactionMap.values().forEach(list -> list.forEach(r -> userIds.add(r.getUserId())));

        Map<Long, String> usernameMap = userRepository.findAllById(userIds).stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(
                        User::getId,
                        user -> user.getUsername() != null
                                ? user.getUsername()
                                : String.valueOf(user.getId())
                ));

        return messages.stream().map(message -> {
            String senderUsername = usernameMap.getOrDefault(message.getSenderId(), String.valueOf(message.getSenderId()));
            List<MessageAttachment> attachments = attachmentMap.getOrDefault(message.getId(), List.of());

            ReplyMessagePreview replyPreview = null;
            if (message.getReplyToMessageId() != null) {
                DirectMessage replyTarget = replyMap.get(message.getReplyToMessageId());
                if (replyTarget != null) {
                    String replySenderUsername = usernameMap.getOrDefault(replyTarget.getSenderId(), String.valueOf(replyTarget.getSenderId()));
                    replyPreview = mapper.toReplyPreview(replyTarget, replySenderUsername);
                }
            }

            List<ReactionResponse> reactions = reactionMap.getOrDefault(message.getId(), List.of()).stream()
                    .map(r -> mapper.toReactionResponse(r, usernameMap.getOrDefault(r.getUserId(), String.valueOf(r.getUserId()))))
                    .toList();

            MessageDeliveryState deliveryState = computeDeliveryState(message, receiptMap.getOrDefault(message.getId(), List.of()));

            return mapper.toMessageResponse(message, senderUsername, attachments, replyPreview, reactions, deliveryState);
        }).toList();
    }

    private MessageResponse hydrateSingleMessage(DirectMessage message, Long currentUserId) {
        return hydrateMessages(List.of(message), currentUserId).get(0);
    }

    private MessageDeliveryState computeDeliveryState(DirectMessage message, List<MessageReceipt> receipts) {
        if (receipts == null || receipts.isEmpty()) {
            return MessageDeliveryState.SENT;
        }
        boolean allRead = receipts.stream().allMatch(r -> r.getReadAt() != null);
        if (allRead) {
            return MessageDeliveryState.READ;
        }
        boolean allDelivered = receipts.stream().allMatch(r -> r.getDeliveredAt() != null || r.getReadAt() != null);
        if (allDelivered) {
            return MessageDeliveryState.DELIVERED;
        }
        return MessageDeliveryState.SENT;
    }
}
