package com.collabsphere_first.project.service;

import com.collabsphere_first.project.dto.*;
import com.collabsphere_first.project.entity.Project;
import com.collabsphere_first.project.entity.ProjectFile;
import com.collabsphere_first.project.entity.ProjectMember;
import com.collabsphere_first.project.repository.*;
import com.collabsphere_first.project.service.impl.ProjectServiceImpl;
import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.enums.UploadState;
import com.collabsphere_first.storage.repository.StorageObjectRepository;
import com.collabsphere_first.workspace.service.WorkspaceAccessChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Project Security and Authorization Hardening Tests")
class ProjectSecurityAuthorizationTest {

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
    private StorageObjectRepository storageObjectRepository;

    @Mock
    private com.collabsphere_first.user.repository.UserRepository userRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Long ownerId = 1L;
    private Long callerUserId = 100L;
    private Project publicProject;
    private UUID publicProjectId;

    @BeforeEach
    void setUp() {
        publicProjectId = UUID.randomUUID();
        publicProject = Project.builder()
                .id(publicProjectId)
                .ownerId(ownerId)
                .title("Public Showcase")
                .visibility("public")
                .state("active")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("P0.1: Public project read access is allowed, but write access is strictly denied for non-member")
    void publicProject_readAllowed_writeDenied() {
        when(projectRepository.findById(publicProjectId)).thenReturn(Optional.of(publicProject));
        when(projectMemberRepository.findByProjectIdAndUserId(publicProjectId, callerUserId))
                .thenReturn(Optional.empty());
        when(projectTaskRepository.findByProjectIdOrderByCreatedAtDesc(publicProjectId))
                .thenReturn(Collections.emptyList());

        // Read is allowed for public project
        var tasks = projectService.listTasks(publicProjectId, callerUserId);
        assertThat(tasks).isNotNull();

        // Write operations must be rejected with AccessDeniedException
        CreateProjectTaskRequest taskRequest = CreateProjectTaskRequest.builder()
                .title("Unauthorized Task")
                .description("Desc")
                .priority("medium")
                .build();
        assertThatThrownBy(() -> projectService.createTask(publicProjectId, taskRequest, callerUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Active project membership with write permission required");

        assertThatThrownBy(() -> projectService.updateTaskStatus(publicProjectId, UUID.randomUUID(), "done", callerUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Active project membership with write permission required");

        SaveProjectFileRequest fileRequest = SaveProjectFileRequest.builder()
                .path("src/main.rs")
                .content("fn main() {}")
                .build();
        assertThatThrownBy(() -> projectService.saveFile(publicProjectId, fileRequest, callerUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Active project membership with write permission required");

        assertThatThrownBy(() -> projectService.deleteFile(publicProjectId, UUID.randomUUID(), callerUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Active project membership with write permission required");
    }

    @Test
    @DisplayName("P0.1: Viewer role has read access but is strictly forbidden from write operations")
    void viewerRole_readAllowed_writeDenied() {
        when(projectRepository.findById(publicProjectId)).thenReturn(Optional.of(publicProject));
        ProjectMember viewerMember = ProjectMember.builder()
                .id(UUID.randomUUID())
                .projectId(publicProjectId)
                .userId(callerUserId)
                .role("viewer")
                .membershipState("active")
                .build();
        when(projectMemberRepository.findByProjectIdAndUserId(publicProjectId, callerUserId))
                .thenReturn(Optional.of(viewerMember));

        CreateProjectTaskRequest taskRequest = CreateProjectTaskRequest.builder()
                .title("Task by Viewer")
                .description("Desc")
                .priority("low")
                .build();
        assertThatThrownBy(() -> projectService.createTask(publicProjectId, taskRequest, callerUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Viewers have read-only access to this project");
    }

    @Test
    @DisplayName("P0.4: Path traversal attacks in saveFile are detected and rejected")
    void pathTraversalInFileSave_rejected() {
        when(projectRepository.findById(publicProjectId)).thenReturn(Optional.of(publicProject));
        ProjectMember maintainer = ProjectMember.builder()
                .id(UUID.randomUUID())
                .projectId(publicProjectId)
                .userId(callerUserId)
                .role("maintainer")
                .membershipState("active")
                .build();
        when(projectMemberRepository.findByProjectIdAndUserId(publicProjectId, callerUserId))
                .thenReturn(Optional.of(maintainer));

        // Test ../ traversal
        SaveProjectFileRequest traversalRequest = SaveProjectFileRequest.builder()
                .path("../../etc/passwd")
                .content("root")
                .build();
        assertThatThrownBy(() -> projectService.saveFile(publicProjectId, traversalRequest, callerUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal");

        // Test absolute leading slash
        SaveProjectFileRequest absolutePathRequest = SaveProjectFileRequest.builder()
                .path("/root/config.yml")
                .content("secret")
                .build();
        assertThatThrownBy(() -> projectService.saveFile(publicProjectId, absolutePathRequest, callerUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path must be relative");

        // Test null byte injection
        SaveProjectFileRequest nullByteRequest = SaveProjectFileRequest.builder()
                .path("doc\0file.txt")
                .content("null")
                .build();
        assertThatThrownBy(() -> projectService.saveFile(publicProjectId, nullByteRequest, callerUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null bytes detected");
    }

    @Test
    @DisplayName("P0.4: Storage object attachment requires COMPLETED state, not quarantined, and caller ownership")
    void storageAttachmentValidation_rejectsUncompletedOrForeign() {
        when(projectRepository.findById(publicProjectId)).thenReturn(Optional.of(publicProject));
        ProjectMember maintainer = ProjectMember.builder()
                .id(UUID.randomUUID())
                .projectId(publicProjectId)
                .userId(callerUserId)
                .role("maintainer")
                .membershipState("active")
                .build();
        when(projectMemberRepository.findByProjectIdAndUserId(publicProjectId, callerUserId))
                .thenReturn(Optional.of(maintainer));

        UUID storageId = UUID.randomUUID();
        // Object in PENDING state
        StorageObject pendingObject = StorageObject.builder()
                .id(storageId)
                .ownerId(callerUserId)
                .uploadState(UploadState.PENDING)
                .workspaceId(publicProjectId)
                .isDeleted(false)
                .build();
        when(storageObjectRepository.findByIdAndIsDeletedFalse(storageId)).thenReturn(Optional.of(pendingObject));

        SaveProjectFileRequest fileRequest = SaveProjectFileRequest.builder()
                .path("valid/path/file.txt")
                .storageObjectId(storageId)
                .build();
        assertThatThrownBy(() -> projectService.saveFile(publicProjectId, fileRequest, callerUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not completed: PENDING");

        // Object owned by someone else
        StorageObject foreignObject = StorageObject.builder()
                .id(storageId)
                .ownerId(8888L)
                .uploadState(UploadState.COMPLETED)
                .workspaceId(publicProjectId)
                .isDeleted(false)
                .build();
        when(storageObjectRepository.findByIdAndIsDeletedFalse(storageId)).thenReturn(Optional.of(foreignObject));

        assertThatThrownBy(() -> projectService.saveFile(publicProjectId, fileRequest, callerUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Caller does not own this storage object");

        // Object quarantined
        StorageObject quarantinedObject = StorageObject.builder()
                .id(storageId)
                .ownerId(callerUserId)
                .uploadState(UploadState.QUARANTINED)
                .workspaceId(publicProjectId)
                .isDeleted(false)
                .build();
        when(storageObjectRepository.findByIdAndIsDeletedFalse(storageId)).thenReturn(Optional.of(quarantinedObject));

        assertThatThrownBy(() -> projectService.saveFile(publicProjectId, fileRequest, callerUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("quarantined");
    }

    @Test
    @DisplayName("P0.1: Inviting member with owner role is strictly forbidden")
    void inviteWithOwnerRole_forbidden() {
        when(projectRepository.findById(publicProjectId)).thenReturn(Optional.of(publicProject));
        when(projectMemberRepository.findByProjectIdAndUserId(publicProjectId, callerUserId))
                .thenReturn(Optional.of(ProjectMember.builder()
                        .role("maintainer")
                        .membershipState("active")
                        .build()));
        when(userRepository.findById(200L)).thenReturn(Optional.of(com.collabsphere_first.user.entity.User.builder()
                .id(200L)
                .email("invitee@collobster.com")
                .build()));

        InviteProjectMemberRequest inviteOwnerRequest = InviteProjectMemberRequest.builder()
                .profileId(200L)
                .role("owner")
                .build();
        assertThatThrownBy(() -> projectService.inviteMember(publicProjectId, inviteOwnerRequest, callerUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot assign owner role via invitation");
    }

    @Test
    @DisplayName("WorkspaceAccessChecker properly discriminates read, write, and storage download permissions")
    void workspaceAccessChecker_permissions() {
        WorkspaceAccessChecker checker = projectService;

        when(projectRepository.existsById(publicProjectId)).thenReturn(true);
        when(projectRepository.findById(publicProjectId)).thenReturn(Optional.of(publicProject));
        when(projectMemberRepository.findByProjectIdAndUserId(publicProjectId, callerUserId))
                .thenReturn(Optional.empty());

        // Supports workspace
        assertThat(checker.supportsWorkspace(publicProjectId)).isTrue();
        assertThat(checker.supportsWorkspace(UUID.randomUUID())).isFalse();

        // Read authorized because project is public
        assertThat(checker.isAuthorized(publicProjectId, callerUserId)).isTrue();

        // Write authorized is FALSE for non-members even if public
        assertThat(checker.isAuthorizedForWrite(publicProjectId, callerUserId)).isFalse();

        // Storage download is FALSE for non-members if unlinked file
        UUID randomStorageId = UUID.randomUUID();
        when(projectFileRepository.findByProjectIdOrderByPathAsc(publicProjectId))
                .thenReturn(Collections.emptyList());
        assertThat(checker.isAuthorizedForStorageDownload(publicProjectId, randomStorageId, callerUserId)).isFalse();
    }
}
