package com.collabsphere_first.messaging.entity;

import com.collabsphere_first.messaging.enums.MessageStatus;
import com.collabsphere_first.messaging.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "direct_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "client_message_id")
    private UUID clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 32)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "reply_to_message_id")
    private UUID replyToMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private MessageStatus status = MessageStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == MessageStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return this.status == MessageStatus.DELETED;
    }

    public void markDeleted() {
        this.status = MessageStatus.DELETED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void editBody(String newBody) {
        this.body = newBody;
        this.editedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
