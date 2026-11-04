package com.collabsphere_first.workspace.dto;

import com.collabsphere_first.workspace.enums.WorkspaceType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectWorkspaceRequest {

    @NotNull(message = "Workspace is required")
    private WorkspaceType workspaceType;
}