package com.collabsphere_first.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "community_channel_messages", indexes = @Index(name = "idx_channel_message_channel_time", columnList = "channel_id, created_at"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityChannelMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private CommunityChannel channel;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private CommunityChannelMessage parentMessage;

    @OneToMany(
            mappedBy = "message",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<CommunityChannelMessageAttachment> attachments = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean removed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public void addAttachment(String attachmentUrl) {
        CommunityChannelMessageAttachment attachment =
                CommunityChannelMessageAttachment.builder()
                        .message(this)
                        .attachmentUrl(attachmentUrl)
                        .build();

        attachments.add(attachment);
    }
}
