package com.collabsphere_first.community.entity;

import com.collabsphere_first.community.enums.MembershipRole;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "community_settings")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommunitySettings {
    @Id @Column(name = "community_id")
    private UUID communityId;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "community_id")
    private Community community;
    @Column(name = "member_posts_enabled", nullable = false) @Builder.Default private boolean memberPostsEnabled = true;
    @Column(name = "comments_enabled", nullable = false) @Builder.Default private boolean commentsEnabled = true;
    @Column(name = "channels_enabled", nullable = false) @Builder.Default private boolean channelsEnabled = true;
    @Column(name = "messaging_enabled", nullable = false) @Builder.Default private boolean messagingEnabled = true;
    @Column(name = "polls_enabled", nullable = false) @Builder.Default private boolean pollsEnabled = true;
    @Column(name = "events_enabled", nullable = false) @Builder.Default private boolean eventsEnabled = true;
    @Column(name = "member_invites_enabled", nullable = false) @Builder.Default private boolean memberInvitesEnabled = false;
    @Column(name = "ai_moderation_enabled", nullable = false) @Builder.Default private boolean aiModerationEnabled = false;
    @Enumerated(EnumType.STRING) @Column(name = "channel_creation_role", nullable = false, length = 20) @Builder.Default private MembershipRole channelCreationRole = MembershipRole.MODERATOR;
    @Enumerated(EnumType.STRING) @Column(name = "message_send_role", nullable = false, length = 20) @Builder.Default private MembershipRole messageSendRole = MembershipRole.MEMBER;
    @Enumerated(EnumType.STRING) @Column(name = "poll_creation_role", nullable = false, length = 20) @Builder.Default private MembershipRole pollCreationRole = MembershipRole.MEMBER;
}
