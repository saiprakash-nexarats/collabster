package com.collabsphere_first.project;

import com.collabsphere_first.company.entity.Company;
import com.collabsphere_first.company.entity.CompanyDocument;
import com.collabsphere_first.company.entity.CompanyMember;
import com.collabsphere_first.company.entity.CompanyTask;
import com.collabsphere_first.company.repository.CompanyDocumentRepository;
import com.collabsphere_first.company.repository.CompanyMemberRepository;
import com.collabsphere_first.company.repository.CompanyRepository;
import com.collabsphere_first.company.repository.CompanyTaskRepository;
import com.collabsphere_first.project.entity.Project;
import com.collabsphere_first.project.entity.ProjectFile;
import com.collabsphere_first.project.entity.ProjectMember;
import com.collabsphere_first.project.entity.ProjectMilestone;
import com.collabsphere_first.project.entity.ProjectTask;
import com.collabsphere_first.project.repository.ProjectFileRepository;
import com.collabsphere_first.project.repository.ProjectMemberRepository;
import com.collabsphere_first.project.repository.ProjectMilestoneRepository;
import com.collabsphere_first.project.repository.ProjectRepository;
import com.collabsphere_first.project.repository.ProjectTaskRepository;
import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.enums.StorageProvider;
import com.collabsphere_first.storage.enums.UploadState;
import com.collabsphere_first.storage.repository.StorageObjectRepository;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.net.Socket;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "jwt.secret=c2FpLXByYWthc2gtbmV4YXJhdHMtY29sbGFic3Rlci10ZXN0LWp3dC1zZWNyZXQta2V5LXNob3VsZC1iZS1hdC1sZWFzdC0yNTYtYml0cy02NC1jaGFyYWN0ZXJzLWxvbmchIQ=="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Real PostgreSQL 17 & Flyway V1-V36 Integration Tests")
class ProjectsAndCompaniesPostgreSqlIntegrationTest {

    static boolean isPostgresAvailable() {
        try (Socket socket = new Socket("localhost", 5432)) {
            return true;
        } catch (Exception e) {
            if ("true".equalsIgnoreCase(System.getenv("CI"))) {
                throw new IllegalStateException("PostgreSQL 17 is required in CI environment but unreachable on localhost:5432", e);
            }
            return false;
        }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectFileRepository projectFileRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private ProjectMilestoneRepository projectMilestoneRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMemberRepository companyMemberRepository;

    @Autowired
    private CompanyTaskRepository companyTaskRepository;

    @Autowired
    private CompanyDocumentRepository companyDocumentRepository;

    @Autowired
    private StorageObjectRepository storageObjectRepository;

    private User owner;
    private User collaborator;
    private User outsider;

    @BeforeEach
    void setUp() {
        if (!isPostgresAvailable()) return;

        owner = userRepository.save(User.builder()
                .firstName("Postgres")
                .lastName("Owner")
                .email("pg_owner_" + System.nanoTime() + "@collobster.com")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        collaborator = userRepository.save(User.builder()
                .firstName("Postgres")
                .lastName("Collaborator")
                .email("pg_collab_" + System.nanoTime() + "@collobster.com")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        outsider = userRepository.save(User.builder()
                .firstName("Postgres")
                .lastName("Outsider")
                .email("pg_outsider_" + System.nanoTime() + "@collobster.com")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());
    }

    @Test
    @EnabledIf("isPostgresAvailable")
    @Transactional
    @DisplayName("PG-01: Real PostgreSQL project creation and membership constraints")
    void testPostgresProjectCreationAndMembership() {
        Project project = projectRepository.save(Project.builder()
                .ownerId(owner.getId())
                .title("PostgreSQL 17 Scalable Architecture")
                .description("Production persistence verification")
                .visibility("team")
                .state("active")
                .build());

        assertThat(project.getId()).isNotNull();

        ProjectMember ownerMembership = projectMemberRepository.save(ProjectMember.builder()
                .projectId(project.getId())
                .userId(owner.getId())
                .role("owner")
                .membershipState("active")
                .build());
        assertThat(ownerMembership.getId()).isNotNull();

        ProjectMember invited = projectMemberRepository.save(ProjectMember.builder()
                .projectId(project.getId())
                .userId(collaborator.getId())
                .role("collaborator")
                .membershipState("pending")
                .invitedBy(owner.getId())
                .build());
        assertThat(invited.getMembershipState()).isEqualTo("pending");

        // Accept invitation
        invited.setMembershipState("active");
        projectMemberRepository.saveAndFlush(invited);
        ProjectMember activeMember = projectMemberRepository.findByProjectIdAndUserId(project.getId(), collaborator.getId()).orElseThrow();
        assertThat(activeMember.getMembershipState()).isEqualTo("active");

        // Unique constraint uq_project_member
        ProjectMember duplicate = ProjectMember.builder()
                .projectId(project.getId())
                .userId(collaborator.getId())
                .role("viewer")
                .membershipState("pending")
                .build();
        assertThatThrownBy(() -> projectMemberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @EnabledIf("isPostgresAvailable")
    @Transactional
    @DisplayName("PG-02: Real PostgreSQL project files, tasks, and unique path constraint")
    void testPostgresProjectFilesAndTasks() {
        Project project = projectRepository.save(Project.builder()
                .ownerId(owner.getId())
                .title("Code Repository")
                .visibility("private")
                .state("active")
                .build());

        ProjectFile file1 = projectFileRepository.save(ProjectFile.builder()
                .projectId(project.getId())
                .path("src/index.js")
                .content("console.log('PostgreSQL 17');")
                .fileType("javascript")
                .createdBy(owner.getId())
                .build());
        assertThat(file1.getId()).isNotNull();

        // Project Task
        ProjectTask task = projectTaskRepository.save(ProjectTask.builder()
                .projectId(project.getId())
                .title("Deploy to staging")
                .status("todo")
                .priority("high")
                .createdBy(owner.getId())
                .build());
        assertThat(task.getId()).isNotNull();

        task.setStatus("in_progress");
        projectTaskRepository.saveAndFlush(task);
        assertThat(projectTaskRepository.findByProjectIdAndId(project.getId(), task.getId()).orElseThrow().getStatus())
                .isEqualTo("in_progress");

        // Duplicate path constraint uq_project_files_path
        ProjectFile duplicateFile = ProjectFile.builder()
                .projectId(project.getId())
                .path("src/index.js")
                .content("console.log('duplicate');")
                .fileType("javascript")
                .createdBy(owner.getId())
                .build();
        assertThatThrownBy(() -> projectFileRepository.saveAndFlush(duplicateFile))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @EnabledIf("isPostgresAvailable")
    @Transactional
    @DisplayName("PG-03: Real PostgreSQL company documents with storage object FK and soft delete")
    void testPostgresCompanyDocumentsWithStorageObjectFk() {
        Company company = companyRepository.save(Company.builder()
                .ownerId(owner.getId())
                .name("PostgreSQL Enterprise Systems")
                .industry("Cloud Infrastructure")
                .stage("Growth")
                .state("active")
                .build());

        UUID storageId = UUID.randomUUID();
        StorageObject storageObject = storageObjectRepository.save(StorageObject.builder()
                .id(storageId)
                .ownerId(owner.getId())
                .storageProvider(StorageProvider.LOCAL)
                .storageKey("uploads/" + storageId + "_audit.pdf")
                .filename("audit.pdf")
                .contentType("application/pdf")
                .uploadState(UploadState.COMPLETED)
                .expectedBytes(2048L)
                .verifiedBytes(2048L)
                .expiresAt(Instant.now().plusSeconds(86400 * 30))
                .build());
        assertThat(storageObject.getId()).isEqualTo(storageId);

        CompanyDocument document = companyDocumentRepository.save(CompanyDocument.builder()
                .companyId(company.getId())
                .storageObjectId(storageId)
                .title("Annual Audit Report")
                .collection("Compliance")
                .accessScope("admins")
                .createdBy(owner.getId())
                .isArchived(false)
                .build());
        assertThat(document.getId()).isNotNull();

        List<CompanyDocument> activeDocs = companyDocumentRepository.findByCompanyIdAndIsArchivedFalseOrderByCreatedAtDesc(company.getId());
        assertThat(activeDocs).hasSize(1);
        assertThat(activeDocs.get(0).getTitle()).isEqualTo("Annual Audit Report");

        // Soft delete archiving
        document.setIsArchived(true);
        document.setArchivedAt(Instant.now());
        companyDocumentRepository.saveAndFlush(document);

        List<CompanyDocument> docsAfterArchive = companyDocumentRepository.findByCompanyIdAndIsArchivedFalseOrderByCreatedAtDesc(company.getId());
        assertThat(docsAfterArchive).isEmpty();
    }
}

