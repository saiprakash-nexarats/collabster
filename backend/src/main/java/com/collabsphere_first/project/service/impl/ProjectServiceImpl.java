package com.collabsphere_first.project.service.impl;

import com.collabsphere_first.project.dto.*;
import com.collabsphere_first.project.entity.*;
import com.collabsphere_first.project.repository.*;
import com.collabsphere_first.project.service.ProjectService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import com.collabsphere_first.workspace.service.WorkspaceAccessChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService, WorkspaceAccessChecker {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMembershipRowDto> listUserProjects(Long currentUserId) {
        if (currentUserId == null) {
            return Collections.emptyList();
        }

        List<ProjectMember> memberships = projectMemberRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        Map<UUID, ProjectMember> memberMap = new LinkedHashMap<>();
        for (ProjectMember m : memberships) {
            memberMap.put(m.getProjectId(), m);
        }

        // Also ensure owned projects are included
        List<Project> owned = projectRepository.findByOwnerIdOrderByCreatedAtDesc(currentUserId);
        for (Project p : owned) {
            if (!memberMap.containsKey(p.getId())) {
                memberMap.put(p.getId(), ProjectMember.builder()
                        .projectId(p.getId())
                        .userId(currentUserId)
                        .role("owner")
                        .membershipState("active")
                        .createdAt(p.getCreatedAt())
                        .build());
            }
        }

        List<ProjectMembershipRowDto> result = new ArrayList<>();
        for (Map.Entry<UUID, ProjectMember> entry : memberMap.entrySet()) {
            projectRepository.findById(entry.getKey()).ifPresent(project -> {
                result.add(ProjectMembershipRowDto.builder()
                        .project(toProjectDto(project))
                        .role(entry.getValue().getRole())
                        .membershipState(entry.getValue().getMembershipState())
                        .build());
            });
        }
        return result;
    }

    @Override
    @Transactional
    public ProjectDto createProject(CreateProjectRequest request, Long currentUserId) {
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required to create a project");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Project title is required");
        }

        String visibility = request.getVisibility() != null && !request.getVisibility().trim().isEmpty()
                ? request.getVisibility().trim().toLowerCase(Locale.ROOT)
                : "private";

        Project project = Project.builder()
                .ownerId(currentUserId)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .visibility(visibility)
                .state("active")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        project = projectRepository.save(project);

        ProjectMember ownerMember = ProjectMember.builder()
                .projectId(project.getId())
                .userId(currentUserId)
                .role("owner")
                .membershipState("active")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        projectMemberRepository.save(ownerMember);

        return toProjectDto(project);
    }

    @Override
    @Transactional
    public ProjectMemberDto inviteMember(UUID projectId, InviteProjectMemberRequest request, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        requireProjectManager(project, currentUserId);

        Long targetUserId = request.getProfileId() != null ? request.getProfileId() : request.getUserId();
        if (targetUserId == null) {
            throw new IllegalArgumentException("Target user/profile ID is required");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found: " + targetUserId));

        Optional<ProjectMember> existing = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId);
        if (existing.isPresent()) {
            ProjectMember member = existing.get();
            if ("active".equalsIgnoreCase(member.getMembershipState())) {
                throw new IllegalStateException("User is already an active member of this project");
            }
            member.setRole(request.getRole() != null ? request.getRole().toLowerCase(Locale.ROOT) : "collaborator");
            member.setMembershipState("pending");
            member.setInvitedBy(currentUserId);
            return toMemberDto(projectMemberRepository.save(member));
        }

        ProjectMember newMember = ProjectMember.builder()
                .projectId(projectId)
                .userId(targetUserId)
                .role(request.getRole() != null ? request.getRole().toLowerCase(Locale.ROOT) : "collaborator")
                .membershipState("pending")
                .invitedBy(currentUserId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toMemberDto(projectMemberRepository.save(newMember));
    }

    @Override
    @Transactional
    public ProjectMemberDto acceptInvitation(UUID projectId, Long currentUserId) {
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        getProjectOrThrow(projectId);

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("No pending invitation found for user on project: " + projectId));

        member.setMembershipState("active");
        member.setUpdatedAt(Instant.now());
        return toMemberDto(projectMemberRepository.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectFileDto> listFiles(UUID projectId, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        requireProjectAccess(project, currentUserId);

        return projectFileRepository.findByProjectIdOrderByPathAsc(projectId).stream()
                .map(this::toFileDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectFileDto saveFile(UUID projectId, SaveProjectFileRequest request, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        requireProjectAccess(project, currentUserId);

        if (request.getPath() == null || request.getPath().trim().isEmpty()) {
            throw new IllegalArgumentException("File path is required");
        }

        String path = request.getPath().trim();
        Optional<ProjectFile> existing = projectFileRepository.findByProjectIdAndPath(projectId, path);

        ProjectFile file;
        if (existing.isPresent()) {
            file = existing.get();
            file.setContent(request.getContent());
            if (request.getFileType() != null) {
                file.setFileType(request.getFileType());
            }
            if (request.getStorageObjectId() != null) {
                file.setStorageObjectId(request.getStorageObjectId());
            }
            file.setUpdatedAt(Instant.now());
        } else {
            file = ProjectFile.builder()
                    .projectId(projectId)
                    .path(path)
                    .content(request.getContent())
                    .fileType(request.getFileType() != null ? request.getFileType() : "text")
                    .storageObjectId(request.getStorageObjectId())
                    .createdBy(currentUserId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        return toFileDto(projectFileRepository.save(file));
    }

    @Override
    @Transactional
    public void deleteFile(UUID projectId, UUID fileId, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        requireProjectAccess(project, currentUserId);

        ProjectFile file = projectFileRepository.findByProjectIdAndId(projectId, fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        projectFileRepository.delete(file);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectTaskDto> listTasks(UUID projectId, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        requireProjectAccess(project, currentUserId);

        return projectTaskRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toTaskDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectTaskDto createTask(UUID projectId, CreateProjectTaskRequest request, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        requireProjectAccess(project, currentUserId);

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title is required");
        }

        ProjectTask task = ProjectTask.builder()
                .projectId(projectId)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status("todo")
                .priority(request.getPriority() != null ? request.getPriority().toLowerCase(Locale.ROOT) : "medium")
                .assigneeId(request.getAssigneeId())
                .createdBy(currentUserId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toTaskDto(projectTaskRepository.save(task));
    }

    @Override
    @Transactional
    public ProjectTaskDto updateTaskStatus(UUID projectId, UUID taskId, String status, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        requireProjectAccess(project, currentUserId);

        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Task status is required");
        }

        String normalizedStatus = status.trim().toLowerCase(Locale.ROOT);
        ProjectTask task = projectTaskRepository.findByProjectIdAndId(projectId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(normalizedStatus);
        task.setUpdatedAt(Instant.now());
        return toTaskDto(projectTaskRepository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMilestoneDto> listMilestones(UUID projectId, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        requireProjectAccess(project, currentUserId);

        return projectMilestoneRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(this::toMilestoneDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectMilestoneDto createMilestone(UUID projectId, CreateProjectMilestoneRequest request, Long currentUserId) {
        Project project = getProjectOrThrow(projectId);
        requireProjectAccess(project, currentUserId);

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Milestone title is required");
        }

        ProjectMilestone milestone = ProjectMilestone.builder()
                .projectId(projectId)
                .title(request.getTitle().trim())
                .targetDate(request.getTargetDate())
                .status("open")
                .createdBy(currentUserId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toMilestoneDto(projectMilestoneRepository.save(milestone));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserAuthorized(UUID projectId, Long currentUserId) {
        if (projectId == null || currentUserId == null) {
            return false;
        }
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return false;
        }
        Project project = projectOpt.get();
        if (currentUserId.equals(project.getOwnerId())) {
            return true;
        }
        if ("public".equalsIgnoreCase(project.getVisibility())) {
            return true;
        }
        return projectMemberRepository.existsByProjectIdAndUserIdAndMembershipState(projectId, currentUserId, "active");
    }

    @Override
    public boolean isAuthorized(UUID workspaceId, Long userId) {
        return isUserAuthorized(workspaceId, userId);
    }

    private Project getProjectOrThrow(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }

    private void requireProjectAccess(Project project, Long currentUserId) {
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (currentUserId.equals(project.getOwnerId())) {
            return;
        }
        if ("public".equalsIgnoreCase(project.getVisibility())) {
            return;
        }
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndMembershipState(project.getId(), currentUserId, "active");
        if (!isMember) {
            throw new AccessDeniedException("You do not have access to this project");
        }
    }

    private void requireProjectManager(Project project, Long currentUserId) {
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (currentUserId.equals(project.getOwnerId())) {
            return;
        }
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUserId);
        if (memberOpt.isPresent()) {
            ProjectMember m = memberOpt.get();
            if ("active".equalsIgnoreCase(m.getMembershipState()) && ("owner".equalsIgnoreCase(m.getRole()) || "maintainer".equalsIgnoreCase(m.getRole()))) {
                return;
            }
        }
        throw new AccessDeniedException("Only project owners and maintainers can perform this action");
    }

    private ProjectDto toProjectDto(Project p) {
        return ProjectDto.builder()
                .id(p.getId())
                .ownerId(p.getOwnerId())
                .title(p.getTitle())
                .description(p.getDescription())
                .visibility(p.getVisibility())
                .state(p.getState())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private ProjectMemberDto toMemberDto(ProjectMember m) {
        return ProjectMemberDto.builder()
                .id(m.getId())
                .projectId(m.getProjectId())
                .userId(m.getUserId())
                .role(m.getRole())
                .membershipState(m.getMembershipState())
                .invitedBy(m.getInvitedBy())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private ProjectFileDto toFileDto(ProjectFile f) {
        return ProjectFileDto.builder()
                .id(f.getId())
                .projectId(f.getProjectId())
                .path(f.getPath())
                .content(f.getContent())
                .fileType(f.getFileType())
                .storageObjectId(f.getStorageObjectId())
                .createdBy(f.getCreatedBy())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }

    private ProjectTaskDto toTaskDto(ProjectTask t) {
        return ProjectTaskDto.builder()
                .id(t.getId())
                .projectId(t.getProjectId())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus())
                .priority(t.getPriority())
                .assigneeId(t.getAssigneeId())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private ProjectMilestoneDto toMilestoneDto(ProjectMilestone m) {
        return ProjectMilestoneDto.builder()
                .id(m.getId())
                .projectId(m.getProjectId())
                .title(m.getTitle())
                .targetDate(m.getTargetDate())
                .status(m.getStatus())
                .createdBy(m.getCreatedBy())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
