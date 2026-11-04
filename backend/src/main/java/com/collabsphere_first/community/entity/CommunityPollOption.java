package com.collabsphere_first.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "community_poll_options", uniqueConstraints = {
        @UniqueConstraint(name = "uq_poll_option_text", columnNames = {"poll_id", "option_text"}),
        @UniqueConstraint(name = "uq_poll_option_position", columnNames = {"poll_id", "position"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPollOption {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private CommunityPoll poll;

    @Column(name = "option_text", nullable = false, length = 200)
    private String text;

    @Column(nullable = false)
    private int position;
}
