package com.collabsphere_first.project;

import com.collabsphere_first.company.entity.*;
import com.collabsphere_first.company.repository.*;
import com.collabsphere_first.project.entity.*;
import com.collabsphere_first.project.repository.*;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("Projects and Companies JPA Database Integration Tests")
class ProjectsAndCompaniesDatabaseIntegrationTest {

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

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .firstName("Sai")
                .lastName("Prakash")
                .email("sai" + System.nanoTime() + "@collobster.com")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        user2 = userRepository.save(User.builder()
                .firstName("Collaborator")
                .lastName("Teammate")
                .email("teammate" + System.nanoTime() + "@collobster.com")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());
    }

    @Test
    @DisplayName("Project and members persist with unique constraints and state transitions")
    void testProjectAndMembersPersistence() {
        Project project = projectRepository.save(Project.builder()
                .ownerId(user1.getId())
                .title("Full Stack Web Platform")
                .description("React + Spring Boot")
                .visibility("team")
                .state("active")
                .build());

        assertThat(project.getId()).isNotNull();

        ProjectMember ownerMember = projectMemberRepository.save(ProjectMember.builder()
                .projectId(project.getId())
                .userId(user1.getId())
                .role("owner")
                .membershipState("active")
                .build());
        assertThat(ownerMember.getId()).isNotNull();

        ProjectMember invitedMember = projectMemberRepository.save(ProjectMember.builder()
                .projectId(project.getId())
                .userId(user2.getId())
                .role("collaborator")
                .membershipState("pending")
                .invitedBy(user1.getId())
                .build());
        assertThat(invitedMember.getId()).isNotNull();

        // Verify membership query
        boolean exists = projectMemberRepository.existsByProjectIdAndUserIdAndMembershipState(
                project.getId(), user2.getId(), "pending");
        assertThat(exists).isTrue();

        // Accept invitation
        invitedMember.setMembershipState("active");
        projectMemberRepository.saveAndFlush(invitedMember);

        ProjectMember updated = projectMemberRepository.findByProjectIdAndUserId(project.getId(), user2.getId()).orElseThrow();
        assertThat(updated.getMembershipState()).isEqualTo("active");

        // Verify duplicate member constraint throws
        ProjectMember duplicate = ProjectMember.builder()
                .projectId(project.getId())
                .userId(user2.getId())
                .role("viewer")
                .membershipState("pending")
                .build();
        assertThatThrownBy(() -> {
            projectMemberRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Project files, tasks, and milestones persist and query by project")
    void testProjectFilesTasksMilestonesPersistence() {
        Project project = projectRepository.save(Project.builder()
                .ownerId(user1.getId())
                .title("Code Repository Space")
                .visibility("private")
                .state("active")
                .build());

        // Save file
        ProjectFile file = projectFileRepository.save(ProjectFile.builder()
                .projectId(project.getId())
                .path("src/App.jsx")
                .content("import React from 'react';")
                .fileType("javascript")
                .createdBy(user1.getId())
                .build());
        assertThat(file.getId()).isNotNull();

        List<ProjectFile> files = projectFileRepository.findByProjectIdOrderByPathAsc(project.getId());
        assertThat(files).hasSize(1);
        assertThat(files.get(0).getPath()).isEqualTo("src/App.jsx");

        // Save Task
        ProjectTask task = projectTaskRepository.save(ProjectTask.builder()
                .projectId(project.getId())
                .title("Initial scaffolding")
                .status("todo")
                .priority("high")
                .createdBy(user1.getId())
                .build());
        assertThat(task.getId()).isNotNull();

        task.setStatus("done");
        projectTaskRepository.saveAndFlush(task);

        ProjectTask updatedTask = projectTaskRepository.findByProjectIdAndId(project.getId(), task.getId()).orElseThrow();
        assertThat(updatedTask.getStatus()).isEqualTo("done");

        // Save Milestone
        ProjectMilestone milestone = projectMilestoneRepository.save(ProjectMilestone.builder()
                .projectId(project.getId())
                .title("Sprint 1")
                .targetDate(Instant.now().plusSeconds(86400))
                .status("open")
                .createdBy(user1.getId())
                .build());
        assertThat(milestone.getId()).isNotNull();

        List<ProjectMilestone> milestones = projectMilestoneRepository.findByProjectIdOrderByCreatedAtAsc(project.getId());
        assertThat(milestones).hasSize(1);
    }

    @Test
    @DisplayName("Company and company documents lifecycle persist correctly")
    void testCompanyAndDocumentsPersistence() {
        Company company = companyRepository.save(Company.builder()
                .ownerId(user1.getId())
                .name("Collobster Corporation")
                .industry("Internet")
                .stage("Series A")
                .state("active")
                .build());
        assertThat(company.getId()).isNotNull();

        CompanyMember member = companyMemberRepository.save(CompanyMember.builder()
                .companyId(company.getId())
                .userId(user1.getId())
                .role("owner")
                .membershipState("active")
                .build());
        assertThat(member.getId()).isNotNull();

        UUID storageObjId = UUID.randomUUID();
        CompanyDocument doc = companyDocumentRepository.save(CompanyDocument.builder()
                .companyId(company.getId())
                .storageObjectId(storageObjId)
                .title("Business Plan 2026")
                .collection("Executive")
                .accessScope("company")
                .createdBy(user1.getId())
                .isArchived(false)
                .build());
        assertThat(doc.getId()).isNotNull();

        List<CompanyDocument> activeDocs = companyDocumentRepository.findByCompanyIdAndIsArchivedFalseOrderByCreatedAtDesc(company.getId());
        assertThat(activeDocs).hasSize(1);

        // Archive document (soft delete)
        doc.setIsArchived(true);
        doc.setArchivedAt(Instant.now());
        companyDocumentRepository.saveAndFlush(doc);

        List<CompanyDocument> activeAfterArchive = companyDocumentRepository.findByCompanyIdAndIsArchivedFalseOrderByCreatedAtDesc(company.getId());
        assertThat(activeAfterArchive).isEmpty();
    }
}
