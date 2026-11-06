package com.collabsphere_first.company.service;

import com.collabsphere_first.company.dto.*;
import com.collabsphere_first.company.entity.*;
import com.collabsphere_first.company.repository.*;
import com.collabsphere_first.company.service.impl.CompanyServiceImpl;
import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.repository.StorageObjectRepository;
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
@DisplayName("CompanyService Unit Tests")
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMemberRepository companyMemberRepository;

    @Mock
    private CompanyTaskRepository companyTaskRepository;

    @Mock
    private CompanyDocumentRepository companyDocumentRepository;

    @Mock
    private StorageObjectRepository storageObjectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private Long ownerId = 10L;
    private Long memberId = 20L;
    private Long outsiderId = 88L;
    private UUID companyId;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        testCompany = Company.builder()
                .id(companyId)
                .ownerId(ownerId)
                .name("Nexarats Innovations")
                .description("Cloud workspace platform")
                .industry("Software")
                .stage("Growth")
                .state("active")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createCompany saves company and auto-enrols owner as active")
    void createCompany_success() {
        CreateCompanyRequest req = CreateCompanyRequest.builder()
                .name("Collobster Tech")
                .description("Next-gen platform")
                .industry("Fintech")
                .stage("Early")
                .build();

        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company c = inv.getArgument(0);
            c.setId(companyId);
            return c;
        });

        CompanyDto dto = companyService.createCompany(req, ownerId);

        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("Collobster Tech");
        assertThat(dto.getOwnerId()).isEqualTo(ownerId);
        verify(companyMemberRepository).save(argThat(m ->
                m.getCompanyId().equals(companyId) &&
                m.getUserId().equals(ownerId) &&
                "owner".equals(m.getRole()) &&
                "active".equals(m.getMembershipState())));
    }

    @Test
    @DisplayName("inviteMember succeeds for company owner")
    void inviteMember_success() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRepository.findById(memberId)).thenReturn(Optional.of(User.builder().id(memberId).build()));
        when(companyMemberRepository.findByCompanyIdAndUserId(companyId, memberId)).thenReturn(Optional.empty());
        when(companyMemberRepository.save(any(CompanyMember.class))).thenAnswer(inv -> inv.getArgument(0));

        InviteCompanyMemberRequest req = InviteCompanyMemberRequest.builder()
                .profileId(memberId)
                .role("member")
                .build();

        CompanyMemberDto memberDto = companyService.inviteMember(companyId, req, ownerId);

        assertThat(memberDto).isNotNull();
        assertThat(memberDto.getUserId()).isEqualTo(memberId);
        assertThat(memberDto.getMembershipState()).isEqualTo("pending");
    }

    @Test
    @DisplayName("inviteMember denies non-admin member")
    void inviteMember_nonAdminDenied() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        CompanyMember regularMember = CompanyMember.builder()
                .companyId(companyId)
                .userId(memberId)
                .role("member")
                .membershipState("active")
                .build();
        when(companyMemberRepository.findByCompanyIdAndUserId(companyId, memberId)).thenReturn(Optional.of(regularMember));

        InviteCompanyMemberRequest req = InviteCompanyMemberRequest.builder().profileId(outsiderId).build();

        assertThatThrownBy(() -> companyService.inviteMember(companyId, req, memberId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("acceptInvitation transitions company membership to active")
    void acceptInvitation_success() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        CompanyMember pending = CompanyMember.builder()
                .companyId(companyId)
                .userId(memberId)
                .role("member")
                .membershipState("pending")
                .build();
        when(companyMemberRepository.findByCompanyIdAndUserId(companyId, memberId)).thenReturn(Optional.of(pending));
        when(companyMemberRepository.save(any(CompanyMember.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanyMemberDto accepted = companyService.acceptInvitation(companyId, memberId);
        assertThat(accepted.getMembershipState()).isEqualTo("active");
    }

    @Test
    @DisplayName("createTask and updateTask work for company members")
    void companyTasks_success() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        UUID taskId = UUID.randomUUID();
        when(companyTaskRepository.save(any(CompanyTask.class))).thenAnswer(inv -> {
            CompanyTask t = inv.getArgument(0);
            t.setId(taskId);
            return t;
        });

        CreateCompanyTaskRequest createReq = CreateCompanyTaskRequest.builder()
                .title("Prepare Q4 roadmap")
                .description("Team planning")
                .build();

        CompanyTaskDto taskDto = companyService.createTask(companyId, createReq, ownerId);
        assertThat(taskDto.getTitle()).isEqualTo("Prepare Q4 roadmap");
        assertThat(taskDto.getState()).isEqualTo("todo");

        CompanyTask existing = CompanyTask.builder()
                .id(taskId)
                .companyId(companyId)
                .title("Prepare Q4 roadmap")
                .state("todo")
                .build();
        when(companyTaskRepository.findByCompanyIdAndId(companyId, taskId)).thenReturn(Optional.of(existing));

        UpdateCompanyTaskRequest updateReq = UpdateCompanyTaskRequest.builder().state("done").build();
        CompanyTaskDto updated = companyService.updateTask(companyId, taskId, updateReq, ownerId);
        assertThat(updated.getState()).isEqualTo("done");
    }

    @Test
    @DisplayName("createDocument links to real storage_object and downloadDocument returns ticket")
    void companyDocument_lifecycle() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        UUID storageObjectId = UUID.randomUUID();
        StorageObject storageObject = StorageObject.builder()
                .id(storageObjectId)
                .filename("QuarterlyReport.pdf")
                .contentType("application/pdf")
                .verifiedBytes(10240L)
                .expectedBytes(10240L)
                .isDeleted(false)
                .build();

        when(storageObjectRepository.findByIdAndIsDeletedFalse(storageObjectId)).thenReturn(Optional.of(storageObject));
        UUID docId = UUID.randomUUID();
        when(companyDocumentRepository.save(any(CompanyDocument.class))).thenAnswer(inv -> {
            CompanyDocument d = inv.getArgument(0);
            d.setId(docId);
            return d;
        });

        CreateCompanyDocumentRequest createReq = CreateCompanyDocumentRequest.builder()
                .objectId(storageObjectId)
                .title("Quarterly Report 2026")
                .collection("Finance")
                .accessScope("company")
                .build();

        CompanyDocumentDto docDto = companyService.createDocument(companyId, createReq, ownerId);
        assertThat(docDto.getTitle()).isEqualTo("Quarterly Report 2026");
        assertThat(docDto.getFilename()).isEqualTo("QuarterlyReport.pdf");

        CompanyDocument savedDoc = CompanyDocument.builder()
                .id(docId)
                .companyId(companyId)
                .storageObjectId(storageObjectId)
                .title("Quarterly Report 2026")
                .accessScope("company")
                .isArchived(false)
                .build();
        when(companyDocumentRepository.findByCompanyIdAndIdAndIsArchivedFalse(companyId, docId)).thenReturn(Optional.of(savedDoc));

        CompanyDownloadTicketDto ticket = companyService.downloadDocument(companyId, docId, ownerId);
        assertThat(ticket.getDownloadUrl()).contains(storageObjectId.toString());
        assertThat(ticket.getExpiresIn()).isPositive();
    }

    @Test
    @DisplayName("archiveDocument sets isArchived to true")
    void archiveDocument_success() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        UUID docId = UUID.randomUUID();
        CompanyDocument savedDoc = CompanyDocument.builder()
                .id(docId)
                .companyId(companyId)
                .storageObjectId(UUID.randomUUID())
                .title("Obsolete Spec")
                .isArchived(false)
                .createdBy(ownerId)
                .build();
        when(companyDocumentRepository.findByCompanyIdAndIdAndIsArchivedFalse(companyId, docId)).thenReturn(Optional.of(savedDoc));

        companyService.archiveDocument(companyId, docId, ownerId);

        assertThat(savedDoc.getIsArchived()).isTrue();
        assertThat(savedDoc.getArchivedAt()).isNotNull();
    }

    @Test
    @DisplayName("outsider denied access to company workspace")
    void outsider_denied() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(companyMemberRepository.existsByCompanyIdAndUserIdAndMembershipState(companyId, outsiderId, "active"))
                .thenReturn(false);

        assertThatThrownBy(() -> companyService.listTasks(companyId, outsiderId))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> companyService.listDocuments(companyId, outsiderId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
