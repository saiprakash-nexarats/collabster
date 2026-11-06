package com.collabsphere_first.project.service;

import com.collabsphere_first.project.dto.*;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    List<ProjectMembershipRowDto> listUserProjects(Long currentUserId);
    ProjectDto createProject(CreateProjectRequest request, Long currentUserId);
    ProjectMemberDto inviteMember(UUID projectId, InviteProjectMemberRequest request, Long currentUserId);
    ProjectMemberDto acceptInvitation(UUID projectId, Long currentUserId);
    List<ProjectFileDto> listFiles(UUID projectId, Long currentUserId);
    ProjectFileDto saveFile(UUID projectId, SaveProjectFileRequest request, Long currentUserId);
    void deleteFile(UUID projectId, UUID fileId, Long currentUserId);
    List<ProjectTaskDto> listTasks(UUID projectId, Long currentUserId);
    ProjectTaskDto createTask(UUID projectId, CreateProjectTaskRequest request, Long currentUserId);
    ProjectTaskDto updateTaskStatus(UUID projectId, UUID taskId, String status, Long currentUserId);
    List<ProjectMilestoneDto> listMilestones(UUID projectId, Long currentUserId);
    ProjectMilestoneDto createMilestone(UUID projectId, CreateProjectMilestoneRequest request, Long currentUserId);
    boolean isUserAuthorized(UUID projectId, Long currentUserId);
}
