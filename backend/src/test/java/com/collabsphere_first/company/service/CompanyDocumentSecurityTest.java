package com.collabsphere_first.company.service;

import com.collabsphere_first.company.dto.*;
import com.collabsphere_first.company.entity.*;
import com.collabsphere_first.company.repository.*;
import com.collabsphere_first.company.service.impl.CompanyServiceImpl;
import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.enums.UploadState;
import com.collabsphere_first.storage.provider.ObjectStorageProvider;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Company Document Security and Access Hardening Tests")
class CompanyDocumentSecurityTest {

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
    private ObjectStorageProvider storageProvider;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private Long companyAdminId = 1L;
    private Long regularMemberId = 2L;
    private Long outsiderId = 3L;

    private Company companyA;
    private UUID companyAId;
    private UUID companyBId;

    @BeforeEach
    void setUp() {
        companyAId = UUID.randomUUID();
        companyA = Company.builder()
                .id(companyAId)
                .ownerId(companyAdminId)
                .name("Company Alpha")
                .state("active")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        companyBId = UUID.randomUUID();
    }

    @Test
    @DisplayName("P0.2: Cannot attach storage object scoped to another company (cross-company attachment)")
    void crossCompanyAttachment_rejected() {
        when(companyRepository.findById(companyAId)).thenReturn(Optional.of(companyA));

        UUID storageId = UUID.randomUUID();
        // Object belongs to company B
        StorageObject companyBObject = StorageObject.builder()
                .id(storageId)
                .ownerId(companyAdminId)
                .uploadState(UploadState.COMPLETED)
                .workspaceId(companyBId)
                .isDeleted(false)
                .build();
        when(storageObjectRepository.findByIdAndIsDeletedFalse(storageId)).thenReturn(Optional.of(companyBObject));

        CreateCompanyDocumentRequest request = CreateCompanyDocumentRequest.builder()
                .title("Tax Form")
                .objectId(storageId)
                .accessScope("company")
                .build();
        assertThatThrownBy(() -> companyService.createDocument(companyAId, request, companyAdminId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("belongs to another workspace");
    }

    @Test
    @DisplayName("P0.2: Cannot attach storage object owned by another user")
    void foreignOwnedStorageObject_rejected() {
        when(companyRepository.findById(companyAId)).thenReturn(Optional.of(companyA));

        UUID storageId = UUID.randomUUID();
        // Object owned by someone else
        StorageObject foreignObject = StorageObject.builder()
                .id(storageId)
                .ownerId(999L)
                .uploadState(UploadState.COMPLETED)
                .workspaceId(companyAId)
                .isDeleted(false)
                .build();
        when(storageObjectRepository.findByIdAndIsDeletedFalse(storageId)).thenReturn(Optional.of(foreignObject));

        CreateCompanyDocumentRequest request = CreateCompanyDocumentRequest.builder()
                .title("Pitch Deck")
                .objectId(storageId)
                .accessScope("company")
                .build();
        assertThatThrownBy(() -> companyService.createDocument(companyAId, request, companyAdminId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Caller does not own this storage object");
    }

    @Test
    @DisplayName("P0.3: Regular member cannot download admin_only document")
    void regularMember_cannotDownloadAdminOnlyDocument() {
        when(companyRepository.findById(companyAId)).thenReturn(Optional.of(companyA));
        when(companyMemberRepository.existsByCompanyIdAndUserIdAndMembershipState(companyAId, regularMemberId, "active"))
                .thenReturn(true);
        when(companyMemberRepository.findByCompanyIdAndUserId(companyAId, regularMemberId))
                .thenReturn(Optional.of(CompanyMember.builder().companyId(companyAId).userId(regularMemberId).role("member").membershipState("active").build()));

        UUID docId = UUID.randomUUID();
        CompanyDocument adminDoc = CompanyDocument.builder()
                .id(docId)
                .companyId(companyAId)
                .title("Cap Table 2026")
                .accessScope("admin_only")
                .isArchived(false)
                .storageObjectId(UUID.randomUUID())
                .build();
        when(companyDocumentRepository.findByCompanyIdAndIdAndIsArchivedFalse(companyAId, docId)).thenReturn(Optional.of(adminDoc));

        // Attempting direct file stream download must fail
        assertThatThrownBy(() -> companyService.downloadDocumentFile(companyAId, docId, regularMemberId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Admin access required");

        // Attempting ticket download must also fail
        assertThatThrownBy(() -> companyService.downloadDocument(companyAId, docId, regularMemberId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Admin access required");
    }

    @Test
    @DisplayName("P0.3: Outsider has no access to company documents")
    void outsider_cannotAccessDocuments() {
        when(companyRepository.findById(companyAId)).thenReturn(Optional.of(companyA));
        when(companyMemberRepository.existsByCompanyIdAndUserIdAndMembershipState(companyAId, outsiderId, "active"))
                .thenReturn(false);

        assertThatThrownBy(() -> companyService.listDocuments(companyAId, outsiderId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have access to this company workspace");
    }

    @Test
    @DisplayName("P0.3: Archived documents cannot be downloaded")
    void archivedDocument_downloadRejected() {
        when(companyRepository.findById(companyAId)).thenReturn(Optional.of(companyA));

        UUID docId = UUID.randomUUID();
        when(companyDocumentRepository.findByCompanyIdAndIdAndIsArchivedFalse(companyAId, docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.downloadDocumentFile(companyAId, docId, companyAdminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    @DisplayName("WorkspaceAccessChecker enforces admin_only check on raw storage download")
    void workspaceAccessChecker_storageDownload_adminOnly() {
        WorkspaceAccessChecker checker = companyService;
        when(companyRepository.existsById(companyAId)).thenReturn(true);
        when(companyRepository.findById(companyAId)).thenReturn(Optional.of(companyA));
        when(companyMemberRepository.findByCompanyIdAndUserId(companyAId, regularMemberId))
                .thenReturn(Optional.of(CompanyMember.builder().companyId(companyAId).userId(regularMemberId).role("member").membershipState("active").build()));

        UUID storageId = UUID.randomUUID();
        CompanyDocument adminDoc = CompanyDocument.builder()
                .id(UUID.randomUUID())
                .companyId(companyAId)
                .accessScope("admin_only")
                .isArchived(false)
                .storageObjectId(storageId)
                .build();
        when(companyDocumentRepository.findByCompanyIdAndIsArchivedFalseOrderByCreatedAtDesc(companyAId))
                .thenReturn(List.of(adminDoc));

        // Regular member cannot download admin_only file
        boolean memberAuthorized = checker.isAuthorizedForStorageDownload(companyAId, storageId, regularMemberId);
        assertThat(memberAuthorized).isFalse();

        // Company admin can download
        when(companyMemberRepository.findByCompanyIdAndUserId(companyAId, companyAdminId))
                .thenReturn(Optional.of(CompanyMember.builder().companyId(companyAId).userId(companyAdminId).role("admin").membershipState("active").build()));
        boolean adminAuthorized = checker.isAuthorizedForStorageDownload(companyAId, storageId, companyAdminId);
        assertThat(adminAuthorized).isTrue();
    }
}
