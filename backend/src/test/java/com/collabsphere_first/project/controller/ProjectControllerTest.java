package com.collabsphere_first.project.controller;

import com.collabsphere_first.project.dto.*;
import com.collabsphere_first.project.service.ProjectService;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectController Web Tier Tests (12 Operations)")
class ProjectControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProjectService projectService;

    @Mock
    private CurrentUserService currentUserService;

    private User mockUser;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(100L);
        mockUser.setEmail("tester@collobster.com");
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        projectId = UUID.randomUUID();
        ProjectController controller = new ProjectController(projectService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("1. GET /v1/projects returns user projects")
    void listProjects() throws Exception {
        ProjectDto project = ProjectDto.builder().id(projectId).title("Collobster Code Space").build();
        ProjectMembershipRowDto row = ProjectMembershipRowDto.builder()
                .project(project)
                .role("owner")
                .membershipState("active")
                .build();
        when(projectService.listUserProjects(100L)).thenReturn(List.of(row));

        mockMvc.perform(get("/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].project.title").value("Collobster Code Space"))
                .andExpect(jsonPath("$.data[0].role").value("owner"));
    }

    @Test
    @DisplayName("2. POST /v1/projects creates new project")
    void createProject() throws Exception {
        CreateProjectRequest req = CreateProjectRequest.builder()
                .title("New React App")
                .description("Vite + React")
                .visibility("private")
                .build();
        ProjectDto project = ProjectDto.builder().id(projectId).title("New React App").build();
        when(projectService.createProject(any(), eq(100L))).thenReturn(project);

        mockMvc.perform(post("/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("New React App"));
    }

    @Test
    @DisplayName("3. POST /v1/projects/{id}/members invites collaborator")
    void inviteMember() throws Exception {
        InviteProjectMemberRequest req = InviteProjectMemberRequest.builder().profileId(200L).role("collaborator").build();
        ProjectMemberDto member = ProjectMemberDto.builder().projectId(projectId).userId(200L).role("collaborator").build();
        when(projectService.inviteMember(eq(projectId), any(), eq(100L))).thenReturn(member);

        mockMvc.perform(post("/v1/projects/" + projectId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(200L));
    }

    @Test
    @DisplayName("4. POST /v1/projects/{id}/membership/accept accepts invitation")
    void acceptInvitation() throws Exception {
        ProjectMemberDto member = ProjectMemberDto.builder().projectId(projectId).userId(100L).membershipState("active").build();
        when(projectService.acceptInvitation(projectId, 100L)).thenReturn(member);

        mockMvc.perform(post("/v1/projects/" + projectId + "/membership/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.membershipState").value("active"));
    }

    @Test
    @DisplayName("5. GET /v1/projects/{id}/files lists project files")
    void listFiles() throws Exception {
        ProjectFileDto file = ProjectFileDto.builder().id(UUID.randomUUID()).projectId(projectId).path("src/App.jsx").build();
        when(projectService.listFiles(projectId, 100L)).thenReturn(List.of(file));

        mockMvc.perform(get("/v1/projects/" + projectId + "/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].path").value("src/App.jsx"));
    }

    @Test
    @DisplayName("6. PUT /v1/projects/{id}/files saves project file")
    void saveFile() throws Exception {
        SaveProjectFileRequest req = SaveProjectFileRequest.builder().path("src/index.js").content("console.log(1)").build();
        ProjectFileDto file = ProjectFileDto.builder().id(UUID.randomUUID()).projectId(projectId).path("src/index.js").build();
        when(projectService.saveFile(eq(projectId), any(), eq(100L))).thenReturn(file);

        mockMvc.perform(put("/v1/projects/" + projectId + "/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.path").value("src/index.js"));
    }

    @Test
    @DisplayName("7. DELETE /v1/projects/{id}/files/{fileId} deletes file")
    void deleteFile() throws Exception {
        UUID fileId = UUID.randomUUID();
        doNothing().when(projectService).deleteFile(projectId, fileId, 100L);

        mockMvc.perform(delete("/v1/projects/" + projectId + "/files/" + fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("8. GET /v1/projects/{id}/tasks lists tasks")
    void listTasks() throws Exception {
        ProjectTaskDto task = ProjectTaskDto.builder().id(UUID.randomUUID()).projectId(projectId).title("Task 1").build();
        when(projectService.listTasks(projectId, 100L)).thenReturn(List.of(task));

        mockMvc.perform(get("/v1/projects/" + projectId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Task 1"));
    }

    @Test
    @DisplayName("9. POST /v1/projects/{id}/tasks creates task")
    void createTask() throws Exception {
        CreateProjectTaskRequest req = CreateProjectTaskRequest.builder().title("Build API").build();
        ProjectTaskDto task = ProjectTaskDto.builder().id(UUID.randomUUID()).projectId(projectId).title("Build API").build();
        when(projectService.createTask(eq(projectId), any(), eq(100L))).thenReturn(task);

        mockMvc.perform(post("/v1/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Build API"));
    }

    @Test
    @DisplayName("10. PATCH /v1/projects/{id}/tasks/{taskId}/status updates task status")
    void updateTaskStatus() throws Exception {
        UUID taskId = UUID.randomUUID();
        ProjectTaskDto task = ProjectTaskDto.builder().id(taskId).projectId(projectId).status("done").build();
        when(projectService.updateTaskStatus(projectId, taskId, "done", 100L)).thenReturn(task);

        mockMvc.perform(patch("/v1/projects/" + projectId + "/tasks/" + taskId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "done"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("done"));
    }

    @Test
    @DisplayName("11. GET /v1/projects/{id}/milestones lists milestones")
    void listMilestones() throws Exception {
        ProjectMilestoneDto milestone = ProjectMilestoneDto.builder().id(UUID.randomUUID()).projectId(projectId).title("MVP").build();
        when(projectService.listMilestones(projectId, 100L)).thenReturn(List.of(milestone));

        mockMvc.perform(get("/v1/projects/" + projectId + "/milestones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("MVP"));
    }

    @Test
    @DisplayName("12. POST /v1/projects/{id}/milestones creates milestone")
    void createMilestone() throws Exception {
        CreateProjectMilestoneRequest req = CreateProjectMilestoneRequest.builder().title("Beta Release").build();
        ProjectMilestoneDto milestone = ProjectMilestoneDto.builder().id(UUID.randomUUID()).projectId(projectId).title("Beta Release").build();
        when(projectService.createMilestone(eq(projectId), any(), eq(100L))).thenReturn(milestone);

        mockMvc.perform(post("/v1/projects/" + projectId + "/milestones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Beta Release"));
    }
}
