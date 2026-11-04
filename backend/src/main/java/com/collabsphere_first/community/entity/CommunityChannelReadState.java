package com.collabsphere_first.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_channel_read_states", uniqueConstraints =
        @UniqueConstraint(name = "uq_channel_read_state", columnNames = {"channel_id", "user_id"}),
        indexes = @Index(name = "idx_channel_read_state_user_community", columnList = "user_id, community_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityChannelReadState {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private CommunityChannel channel;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private CommunityChannelMessage lastReadMessage;

    @Column(name = "last_read_at", nullable = false)
    @Builder.Default
    private Instant lastReadAt = Instant.now();
}
