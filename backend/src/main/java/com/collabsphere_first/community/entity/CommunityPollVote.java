package com.collabsphere_first.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_poll_votes", uniqueConstraints = @UniqueConstraint(name = "uq_poll_user_option", columnNames = {"poll_id", "user_id", "option_id"}), indexes = @Index(name = "idx_poll_vote_user", columnList = "poll_id, user_id"))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPollVote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private CommunityPoll poll;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private CommunityPollOption option;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
