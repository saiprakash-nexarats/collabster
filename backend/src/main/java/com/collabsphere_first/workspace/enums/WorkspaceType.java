package com.collabsphere_first.workspace.enums;

public enum WorkspaceType {

    STUDENT(false),

    MENTOR(true),

    FOUNDER(true),

    COMPANY(true),

    INVESTOR(true),

    CREATOR(false),

    INFLUENCER(true),

    PROFESSIONAL(false),

    SERVICE_PROVIDER(true);

    private final boolean verificationRequired;

    WorkspaceType(boolean verificationRequired) {
        this.verificationRequired = verificationRequired;
    }

    public boolean isVerificationRequired() {
        return verificationRequired;
    }
}