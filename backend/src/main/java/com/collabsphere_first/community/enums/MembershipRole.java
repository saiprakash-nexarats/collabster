package com.collabsphere_first.community.enums;

import lombok.Getter;

@Getter
public enum MembershipRole {
    FOLLOWER(0),
    MEMBER(1),
    MODERATOR(2),
    ADMIN(3),
    OWNER(4);

    private final int rank;

    MembershipRole(int rank) {
        this.rank = rank;
    }

    public boolean hasAtLeastRank(MembershipRole requiredRole) {
        return this.rank >= requiredRole.getRank();
    }
}
