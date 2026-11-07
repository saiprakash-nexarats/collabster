package com.collabsphere_first.workspace.service;

import java.util.UUID;

public interface WorkspaceAccessChecker {
    /**
     * Determines whether this checker supports and owns the specified workspace ID.
     */
    boolean supportsWorkspace(UUID workspaceId);

    /**
     * Determines whether user has read/access permissions to this workspace.
     */
    boolean isAuthorized(UUID workspaceId, Long userId);

    /**
     * Determines whether user has write permissions to this workspace.
     * Public visibility must never grant write permissions.
     */
    boolean isAuthorizedForWrite(UUID workspaceId, Long userId);

    /**
     * Determines whether user is authorized to download a specific storage object
     * associated with this workspace, honoring document access scopes (e.g. admin_only, non-archived).
     */
    boolean isAuthorizedForStorageDownload(UUID workspaceId, UUID storageObjectId, Long userId);
}

