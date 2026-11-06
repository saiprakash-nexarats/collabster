package com.collabsphere_first.workspace.service;

import java.util.UUID;

public interface WorkspaceAccessChecker {
    boolean isAuthorized(UUID workspaceId, Long userId);
}
