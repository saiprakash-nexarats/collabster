package com.collabsphere_first.workspace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkspaceOnboardingStatusResponse {

    private boolean profileCompleted;

    private boolean primaryWorkspaceSelected;

    private boolean secondaryWorkspaceCompleted;

    private String nextStep;
}