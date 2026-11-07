package com.collabsphere_first.project.service;

import com.collabsphere_first.project.dto.*;
import com.collabsphere_first.project.entity.*;
import com.collabsphere_first.project.repository.*;
import com.collabsphere_first.project.service.impl.ProjectServiceImpl;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectFileRepository projectFileRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private ProjectMilestoneRepository projectMilestoneRepository;

    @Mock
    private com.collabsphere_first.storage.repository.StorageObjectRepository storageObjectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Long ownerId = 1L;
    private Long memberId = 2L;
    private Long strangerId = 99L;
    private UUID projectId;
    private Project testProject;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        testProject = Project.builder()
                .id(projectId)
                .ownerId(ownerId)
                .title("Test Collobster Code Space")
                .description("A collaborative project")
                .visibility("private")
                .state("active")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createProject saves project and creates owner member record")
    void createProject_success() {
        CreateProjectRequest req = CreateProjectRequest.builder()
                .title("New Code Space")
                .description("Ready for code")
                .visibility("private")
                .build();

        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(projectId);
            return p;
        });

        ProjectDto dto = projectService.createProject(req, ownerId);

        assertThat(dto).isNotNull();
        assertThat(dto.getTitle()).isEqualTo("New Code Space");
        assertThat(dto.getOwnerId()).isEqualTo(ownerId);
        verify(projectMemberRepository).save(argThat(m ->
                m.getProjectId().equals(projectId) &&
                m.getUserId().equals(ownerId) &&
                "owner".equals(m.getRole()) &&
                "active".equals(m.getMembershipState())));
    }

    @Test
    @DisplayName("createProject rejects blank title")
    void createProject_blankTitle_throws() {
        CreateProjectRequest req = CreateProjectRequest.builder().title("  ").build();
        assertThatThrownBy(() -> projectService.createProject(req, ownerId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("inviteMember succeeds when inviter is owner and target user exists")
    void inviteMember_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(memberId)).thenReturn(Optional.of(User.builder().id(memberId).build()));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, memberId)).thenReturn(Optional.empty());
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(inv -> inv.getArgument(0));

        InviteProjectMemberRequest req = InviteProjectMemberRequest.builder()
                .profileId(memberId)
                .role("collaborator")
                .build();

        ProjectMemberDto memberDto = projectService.inviteMember(projectId, req, ownerId);

        assertThat(memberDto).isNotNull();
        assertThat(memberDto.getUserId()).isEqualTo(memberId);
        assertThat(memberDto.getMembershipState()).isEqualTo("pending");
        assertThat(memberDto.getInvitedBy()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("inviteMember rejects unauthorized user")
    void inviteMember_unauthorized_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, strangerId)).thenReturn(Optional.empty());

        InviteProjectMemberRequest req = InviteProjectMemberRequest.builder().profileId(memberId).build();

        assertThatThrownBy(() -> projectService.inviteMember(projectId, req, strangerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("acceptInvitation transitions state to active")
    void acceptInvitation_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        ProjectMember pendingMember = ProjectMember.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .userId(memberId)
                .role("collaborator")
                .membershipState("pending")
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, memberId)).thenReturn(Optional.of(pendingMember));
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectMemberDto accepted = projectService.acceptInvitation(projectId, memberId);

        assertThat(accepted.getMembershipState()).isEqualTo("active");
    }

    @Test
    @DisplayName("saveFile creates or updates project file for authorized member")
    void saveFile_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(projectFileRepository.findByProjectIdAndPath(projectId, "src/App.jsx")).thenReturn(Optional.empty());
        when(projectFileRepository.save(any(ProjectFile.class))).thenAnswer(inv -> {
            ProjectFile f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        SaveProjectFileRequest req = SaveProjectFileRequest.builder()
                .path("src/App.jsx")
                .content("export default function App() {}")
                .fileType("javascript")
                .build();

        ProjectFileDto fileDto = projectService.saveFile(projectId, req, ownerId);

        assertThat(fileDto).isNotNull();
        assertThat(fileDto.getPath()).isEqualTo("src/App.jsx");
        assertThat(fileDto.getContent()).isEqualTo("export default function App() {}");
    }

    @Test
    @DisplayName("createTask saves task and updateTaskStatus updates it")
    void taskLifecycle_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        UUID taskId = UUID.randomUUID();
        when(projectTaskRepository.save(any(ProjectTask.class))).thenAnswer(inv -> {
            ProjectTask t = inv.getArgument(0);
            t.setId(taskId);
            return t;
        });

        CreateProjectTaskRequest createReq = CreateProjectTaskRequest.builder()
                .title("Implement React components")
                .description("Wire up api.projects")
                .priority("high")
                .build();

        ProjectTaskDto created = projectService.createTask(projectId, createReq, ownerId);
        assertThat(created.getTitle()).isEqualTo("Implement React components");
        assertThat(created.getStatus()).isEqualTo("todo");

        ProjectTask existing = ProjectTask.builder()
                .id(taskId)
                .projectId(projectId)
                .title("Implement React components")
                .status("todo")
                .build();
        when(projectTaskRepository.findByProjectIdAndId(projectId, taskId)).thenReturn(Optional.of(existing));

        ProjectTaskDto updated = projectService.updateTaskStatus(projectId, taskId, "done", ownerId);
        assertThat(updated.getStatus()).isEqualTo("done");
    }

    @Test
    @DisplayName("milestone creation works for authorized member")
    void milestone_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(projectMilestoneRepository.save(any(ProjectMilestone.class))).thenAnswer(inv -> {
            ProjectMilestone m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        CreateProjectMilestoneRequest req = CreateProjectMilestoneRequest.builder()
                .title("Phase 1 Release")
                .targetDate(Instant.now())
                .build();

        ProjectMilestoneDto milestone = projectService.createMilestone(projectId, req, ownerId);
        assertThat(milestone.getTitle()).isEqualTo("Phase 1 Release");
        assertThat(milestone.getStatus()).isEqualTo("open");
    }

    @Test
    @DisplayName("unauthorized stranger blocked from viewing files, tasks, milestones")
    void stranger_accessDenied() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.existsByProjectIdAndUserIdAndMembershipState(projectId, strangerId, "active"))
                .thenReturn(false);

        assertThatThrownBy(() -> projectService.listFiles(projectId, strangerId))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> projectService.listTasks(projectId, strangerId))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> projectService.listMilestones(projectId, strangerId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
