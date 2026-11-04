package com.collabsphere_first.messaging.entity;

import com.collabsphere_first.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "starred_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StarredMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private DirectMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "starred_at", nullable = false)
    @Builder.Default
    private Instant starredAt = Instant.now();
}

