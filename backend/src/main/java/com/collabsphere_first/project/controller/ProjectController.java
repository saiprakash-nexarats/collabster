package com.collabsphere_first.project.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.project.dto.*;
import com.collabsphere_first.project.service.ProjectService;
import com.collabsphere_first.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/v1/projects", "/api/v1/projects"})
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectMembershipRowDto>>> listProjects() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<ProjectMembershipRowDto> projects = projectService.listUserProjects(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDto>> createProject(
            @Valid @RequestBody CreateProjectRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ProjectDto project = projectService.createProject(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(project));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<ProjectMemberDto>> inviteMember(
            @PathVariable UUID id,
            @RequestBody InviteProjectMemberRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ProjectMemberDto member = projectService.inviteMember(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(member));
    }

    @PostMapping("/{id}/membership/accept")
    public ResponseEntity<ApiResponse<ProjectMemberDto>> acceptInvitation(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ProjectMemberDto member = projectService.acceptInvitation(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(member));
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<ApiResponse<List<ProjectFileDto>>> listFiles(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<ProjectFileDto> files = projectService.listFiles(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @PutMapping("/{id}/files")
    public ResponseEntity<ApiResponse<ProjectFileDto>> saveFile(
            @PathVariable UUID id,
            @Valid @RequestBody SaveProjectFileRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ProjectFileDto file = projectService.saveFile(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(file));
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable UUID id,
            @PathVariable UUID fileId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        projectService.deleteFile(id, fileId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<List<ProjectTaskDto>>> listTasks(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<ProjectTaskDto> tasks = projectService.listTasks(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<ProjectTaskDto>> createTask(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProjectTaskRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ProjectTaskDto task = projectService.createTask(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(task));
    }

    @PatchMapping("/{id}/tasks/{taskId}/status")
    public ResponseEntity<ApiResponse<ProjectTaskDto>> updateTaskStatus(
            @PathVariable UUID id,
            @PathVariable UUID taskId,
            @RequestBody Map<String, String> body
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        String status = body != null ? body.get("status") : null;
        ProjectTaskDto task = projectService.updateTaskStatus(id, taskId, status, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @GetMapping("/{id}/milestones")
    public ResponseEntity<ApiResponse<List<ProjectMilestoneDto>>> listMilestones(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<ProjectMilestoneDto> milestones = projectService.listMilestones(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @PostMapping("/{id}/milestones")
    public ResponseEntity<ApiResponse<ProjectMilestoneDto>> createMilestone(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProjectMilestoneRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ProjectMilestoneDto milestone = projectService.createMilestone(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(milestone));
    }
}
