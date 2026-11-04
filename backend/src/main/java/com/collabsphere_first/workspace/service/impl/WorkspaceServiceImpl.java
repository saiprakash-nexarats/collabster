package com.collabsphere_first.workspace.service.impl;

import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import com.collabsphere_first.workspace.dto.SelectWorkspaceRequest;
import com.collabsphere_first.workspace.dto.WorkspaceOnboardingStatusResponse;
import com.collabsphere_first.workspace.dto.WorkspaceResponse;
import com.collabsphere_first.workspace.entity.UserWorkspace;
import com.collabsphere_first.workspace.enums.WorkspacePriority;
import com.collabsphere_first.workspace.enums.WorkspaceStatus;
import com.collabsphere_first.workspace.repository.UserWorkspaceRepository;
import com.collabsphere_first.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceServiceImpl implements WorkspaceService {

    private final UserWorkspaceRepository userWorkspaceRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Override
    public WorkspaceResponse selectPrimaryWorkspace(SelectWorkspaceRequest request) {

        User user = currentUserService.getCurrentUser();

        if (!user.getProfileCompleted()) {
            throw new IllegalStateException("Please complete your profile first.");
        }

        if (userWorkspaceRepository.existsByUserAndPriority(user, WorkspacePriority.PRIMARY)) {
            throw new IllegalStateException("Primary workspace already selected.");
        }

        if (userWorkspaceRepository.existsByUserAndWorkspaceType(user, request.getWorkspaceType())) {
            throw new IllegalStateException("Workspace already exists.");
        }

        WorkspaceStatus status = request.getWorkspaceType().isVerificationRequired()
                ? WorkspaceStatus.PENDING
                : WorkspaceStatus.ACTIVE;

        UserWorkspace workspace = UserWorkspace.builder()
                .user(user)
                .workspaceType(request.getWorkspaceType())
                .priority(WorkspacePriority.PRIMARY)
                .status(status)
                .build();

        userWorkspaceRepository.save(workspace);

        return WorkspaceResponse.builder()
                .workspaceType(workspace.getWorkspaceType())
                .priority(workspace.getPriority())
                .status(workspace.getStatus())
                .message("Primary workspace selected successfully.")
                .build();
    }

    @Override
    public WorkspaceResponse selectSecondaryWorkspace(SelectWorkspaceRequest request) {

        User user = currentUserService.getCurrentUser();

        if (!user.getProfileCompleted()) {
            throw new IllegalStateException("Please complete your profile first.");
        }

        UserWorkspace primaryWorkspace = userWorkspaceRepository
                .findByUserAndPriority(user, WorkspacePriority.PRIMARY)
                .orElseThrow(() ->
                        new IllegalStateException("Please select a primary workspace first."));

        if (primaryWorkspace.getWorkspaceType() == request.getWorkspaceType()) {
            throw new IllegalStateException("Primary and secondary workspace cannot be the same.");
        }

        if (userWorkspaceRepository.existsByUserAndPriority(user, WorkspacePriority.SECONDARY)) {
            throw new IllegalStateException("Secondary workspace already selected.");
        }

        if (userWorkspaceRepository.existsByUserAndWorkspaceType(user, request.getWorkspaceType())) {
            throw new IllegalStateException("Workspace already exists.");
        }

        WorkspaceStatus status = request.getWorkspaceType().isVerificationRequired()
                ? WorkspaceStatus.PENDING
                : WorkspaceStatus.ACTIVE;

        UserWorkspace workspace = UserWorkspace.builder()
                .user(user)
                .workspaceType(request.getWorkspaceType())
                .priority(WorkspacePriority.SECONDARY)
                .status(status)
                .build();

        userWorkspaceRepository.save(workspace);

        return WorkspaceResponse.builder()
                .workspaceType(workspace.getWorkspaceType())
                .priority(workspace.getPriority())
                .status(workspace.getStatus())
                .message("Secondary workspace selected successfully.")
                .build();
    }

    @Override
    public void skipSecondaryWorkspace() {

        User user = currentUserService.getCurrentUser();

        // We'll enhance this later when we add a dedicated onboarding flag.
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getMyWorkspaces() {

        User user = currentUserService.getCurrentUser();

        return userWorkspaceRepository.findByUser(user)
                .stream()
                .map(workspace -> WorkspaceResponse.builder()
                        .workspaceType(workspace.getWorkspaceType())
                        .priority(workspace.getPriority())
                        .status(workspace.getStatus())
                        .message("Workspace found.")
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceOnboardingStatusResponse getOnboardingStatus() {

        User user = currentUserService.getCurrentUser();

        boolean primarySelected =
                userWorkspaceRepository.existsByUserAndPriority(
                        user,
                        WorkspacePriority.PRIMARY
                );

        boolean secondarySelected =
                userWorkspaceRepository.existsByUserAndPriority(
                        user,
                        WorkspacePriority.SECONDARY
                );

        String nextStep;

        if (!user.getProfileCompleted()) {
            nextStep = "COMPLETE_PROFILE";
        } else if (!primarySelected) {
            nextStep = "SELECT_PRIMARY_WORKSPACE";
        } else if (!secondarySelected) {
            nextStep = "SELECT_SECONDARY_WORKSPACE";
        } else {
            nextStep = "GO_TO_DASHBOARD";
        }

        return WorkspaceOnboardingStatusResponse.builder()
                .profileCompleted(user.getProfileCompleted())
                .primaryWorkspaceSelected(primarySelected)
                .secondaryWorkspaceCompleted(secondarySelected)
                .nextStep(nextStep)
                .build();
    }
}