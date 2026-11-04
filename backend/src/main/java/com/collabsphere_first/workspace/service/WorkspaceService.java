package com.collabsphere_first.workspace.service;

import com.collabsphere_first.workspace.dto.SelectWorkspaceRequest;
import com.collabsphere_first.workspace.dto.WorkspaceOnboardingStatusResponse;
import com.collabsphere_first.workspace.dto.WorkspaceResponse;

import java.util.List;

public interface WorkspaceService {

    WorkspaceResponse selectPrimaryWorkspace(SelectWorkspaceRequest request);

    WorkspaceResponse selectSecondaryWorkspace(SelectWorkspaceRequest request);

    void skipSecondaryWorkspace();

    List<WorkspaceResponse> getMyWorkspaces();

    WorkspaceOnboardingStatusResponse getOnboardingStatus();
}