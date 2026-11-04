package com.collabsphere_first.messaging.entity;

import com.collabsphere_first.messaging.enums.ConversationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    @Builder.Default
    private ConversationType type = ConversationType.DIRECT;

    @Column(name = "direct_user_low_id")
    private Long directUserLowId;

    @Column(name = "direct_user_high_id")
    private Long directUserHighId;

    @Column(name = "last_message_id")
    private UUID lastMessageId;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Helper to determine the other participant ID in a direct 1:1 conversation.
     */
    public Long getOtherParticipantId(Long currentUserId) {
        if (currentUserId == null) return null;
        if (currentUserId.equals(directUserLowId)) return directUserHighId;
        if (currentUserId.equals(directUserHighId)) return directUserLowId;
        return null;
    }

    /**
     * Checks if the given user is one of the direct conversation participants.
     */
    public boolean isDirectParticipant(Long userId) {
        if (userId == null) return false;
        return userId.equals(directUserLowId) || userId.equals(directUserHighId);
    }
}
