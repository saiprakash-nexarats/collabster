package com.collabsphere_first.workspace.dto;

import com.collabsphere_first.workspace.enums.WorkspacePriority;
import com.collabsphere_first.workspace.enums.WorkspaceStatus;
import com.collabsphere_first.workspace.enums.WorkspaceType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkspaceResponse {

    private WorkspaceType workspaceType;

    private WorkspacePriority priority;

    private WorkspaceStatus status;

    private String message;
}