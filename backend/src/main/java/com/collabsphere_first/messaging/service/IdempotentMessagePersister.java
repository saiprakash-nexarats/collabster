package com.collabsphere_first.messaging.service;

import com.collabsphere_first.messaging.dto.PersistMessageResult;
import com.collabsphere_first.messaging.dto.SendMessageRequest;
import com.collabsphere_first.messaging.entity.*;
import com.collabsphere_first.messaging.enums.MessageStatus;
import com.collabsphere_first.messaging.enums.MessageType;
import com.collabsphere_first.messaging.exception.ConcurrentIdempotentSendException;
import com.collabsphere_first.messaging.exception.ConversationNotFoundException;
import com.collabsphere_first.messaging.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentMessagePersister {

    private final DirectMessageRepository messageRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final ConversationRepository conversationRepository;
    private final MessageReceiptRepository receiptRepository;
    private final MessagingAttachmentService attachmentService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public PersistMessageResult persistMessage(
            UUID conversationId,
            Long currentUserId,
            Long recipientId,
            SendMessageRequest request,
            MessageType messageType,
            boolean hasBody
    ) {
        if (request.clientMessageId() != null) {
            Optional<DirectMessage> existing = messageRepository.findByConversationIdAndSenderIdAndClientMessageId(
                    conversationId, currentUserId, request.clientMessageId()
            );
            if (existing.isPresent()) {
                return new PersistMessageResult(existing.get(), false);
            }
        }

        Instant now = Instant.now();
        DirectMessage message = DirectMessage.builder()
                .conversationId(conversationId)
                .senderId(currentUserId)
                .clientMessageId(request.clientMessageId())
                .messageType(messageType)
                .body(hasBody ? request.body().trim() : null)
                .replyToMessageId(request.replyToMessageId())
                .status(MessageStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            message = messageRepository.saveAndFlush(message);

            List<MessageAttachment> attachments = attachmentService.createAttachments(message, request.attachments());
            if (!attachments.isEmpty()) {
                attachmentRepository.saveAll(attachments);
            }

            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ConversationNotFoundException(conversationId));
            conversation.setLastMessageId(message.getId());
            conversation.setLastMessageAt(message.getCreatedAt());
            conversation.setUpdatedAt(message.getCreatedAt());
            conversationRepository.save(conversation);

            if (recipientId != null) {
                MessageReceipt receipt = MessageReceipt.builder()
                        .id(new MessageReceiptId(message.getId(), recipientId))
                        .deliveredAt(null)
                        .readAt(null)
                        .build();
                receiptRepository.save(receipt);
            }

            eventPublisher.publishEvent(new com.collabsphere_first.messaging.event.DirectMessageCreatedEvent(
                    conversationId,
                    message.getId(),
                    currentUserId,
                    recipientId,
                    message.getCreatedAt()
            ));

            return new PersistMessageResult(message, true);
        } catch (DataIntegrityViolationException ex) {
            if (request.clientMessageId() != null && isIdempotencyViolation(ex)) {
                log.info("Concurrent insert caught idempotency unique constraint violation for clientMessageId {}", request.clientMessageId());
                throw new ConcurrentIdempotentSendException(ex);
            }
            throw ex;
        }
    }

    private boolean isIdempotencyViolation(DataIntegrityViolationException ex) {
        String message = ex.getMessage();
        Throwable cause = ex.getRootCause();
        if (cause != null && cause.getMessage() != null) {
            message = message + " " + cause.getMessage();
        }
        return message != null && (message.contains("uq_direct_messages_client_msg") ||
                (message.contains("client_message_id") && message.contains("unique")));
    }
}
