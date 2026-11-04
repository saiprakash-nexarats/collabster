package com.collabsphere_first.workspace.repository;

import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.workspace.entity.UserWorkspace;
import com.collabsphere_first.workspace.enums.WorkspacePriority;
import com.collabsphere_first.workspace.enums.WorkspaceStatus;
import com.collabsphere_first.workspace.enums.WorkspaceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWorkspaceRepository extends JpaRepository<UserWorkspace, Long> {

    Optional<UserWorkspace> findByUserAndWorkspaceType(User user, WorkspaceType workspaceType);

    boolean existsByUserAndWorkspaceType(User user, WorkspaceType workspaceType);

    List<UserWorkspace> findByUser(User user);

    List<UserWorkspace> findByUserAndStatus(User user, WorkspaceStatus status);

    Optional<UserWorkspace> findByUserAndPriority(User user, WorkspacePriority priority);

    boolean existsByUserAndPriority(User user, WorkspacePriority priority);
}