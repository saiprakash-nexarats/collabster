package com.collabsphere_first.company.service.impl;

import com.collabsphere_first.company.dto.*;
import com.collabsphere_first.company.entity.*;
import com.collabsphere_first.company.repository.*;
import com.collabsphere_first.company.service.CompanyService;
import com.collabsphere_first.storage.dto.StorageFileResource;
import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.enums.UploadState;
import com.collabsphere_first.storage.provider.ObjectStorageProvider;
import com.collabsphere_first.storage.repository.StorageObjectRepository;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import com.collabsphere_first.workspace.service.WorkspaceAccessChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService, WorkspaceAccessChecker {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final CompanyTaskRepository companyTaskRepository;
    private final CompanyDocumentRepository companyDocumentRepository;
    private final StorageObjectRepository storageObjectRepository;
    private final ObjectStorageProvider storageProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CompanyMembershipRowDto> listUserCompanies(Long currentUserId) {
        if (currentUserId == null) {
            return Collections.emptyList();
        }

        List<CompanyMember> memberships = companyMemberRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        Map<UUID, CompanyMember> memberMap = new LinkedHashMap<>();
        for (CompanyMember m : memberships) {
            memberMap.put(m.getCompanyId(), m);
        }

        // Also ensure owned companies are included
        List<Company> owned = companyRepository.findByOwnerIdOrderByCreatedAtDesc(currentUserId);
        for (Company c : owned) {
            if (!memberMap.containsKey(c.getId())) {
                memberMap.put(c.getId(), CompanyMember.builder()
                        .companyId(c.getId())
                        .userId(currentUserId)
                        .role("owner")
                        .membershipState("active")
                        .createdAt(c.getCreatedAt())
                        .build());
            }
        }

        List<CompanyMembershipRowDto> result = new ArrayList<>();
        for (Map.Entry<UUID, CompanyMember> entry : memberMap.entrySet()) {
            companyRepository.findById(entry.getKey()).ifPresent(company -> {
                result.add(CompanyMembershipRowDto.builder()
                        .company(toCompanyDto(company))
                        .role(entry.getValue().getRole())
                        .membershipState(entry.getValue().getMembershipState())
                        .build());
            });
        }
        return result;
    }

    @Override
    @Transactional
    public CompanyDto createCompany(CreateCompanyRequest request, Long currentUserId) {
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required to create a company");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Company name is required");
        }

        Company company = Company.builder()
                .ownerId(currentUserId)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .industry(request.getIndustry() != null ? request.getIndustry().trim() : null)
                .stage(request.getStage() != null ? request.getStage().trim() : null)
                .state("active")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        company = companyRepository.save(company);

        CompanyMember ownerMember = CompanyMember.builder()
                .companyId(company.getId())
                .userId(currentUserId)
                .role("owner")
                .membershipState("active")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        companyMemberRepository.save(ownerMember);

        return toCompanyDto(company);
    }

    @Override
    @Transactional
    public CompanyMemberDto inviteMember(UUID companyId, InviteCompanyMemberRequest request, Long currentUserId) {
        Company company = getCompanyOrThrow(companyId);
        requireCompanyAdmin(company, currentUserId);

        Long targetUserId = request.getProfileId() != null ? request.getProfileId() : request.getUserId();
        if (targetUserId == null) {
            throw new IllegalArgumentException("Target user/profile ID is required");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found: " + targetUserId));

        // Prevent unauthorized owner role assignment via invitations
        String role = request.getRole() != null ? request.getRole().trim().toLowerCase(Locale.ROOT) : "member";
        if ("owner".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Cannot assign owner role via invitation");
        }
        if (!Set.of("admin", "member").contains(role)) {
            throw new IllegalArgumentException("Invalid company member role: " + role);
        }

        Optional<CompanyMember> existing = companyMemberRepository.findByCompanyIdAndUserId(companyId, targetUserId);
        if (existing.isPresent()) {
            CompanyMember member = existing.get();
            if ("active".equalsIgnoreCase(member.getMembershipState())) {
                throw new IllegalStateException("User is already an active member of this company");
            }
            member.setRole(role);
            member.setMembershipState("pending");
            member.setInvitedBy(currentUserId);
            member.setUpdatedAt(Instant.now());
            return toMemberDto(companyMemberRepository.save(member));
        }

        CompanyMember newMember = CompanyMember.builder()
                .companyId(companyId)
                .userId(targetUserId)
                .role(role)
                .membershipState("pending")
                .invitedBy(currentUserId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toMemberDto(companyMemberRepository.save(newMember));
    }

    @Override
    @Transactional
    public CompanyMemberDto acceptInvitation(UUID companyId, Long currentUserId) {
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        getCompanyOrThrow(companyId);

        CompanyMember member = companyMemberRepository.findByCompanyIdAndUserId(companyId, currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("No pending invitation found for user on company: " + companyId));

        if (!"pending".equalsIgnoreCase(member.getMembershipState())) {
            throw new IllegalStateException("Only pending invitations can be accepted; current state: " + member.getMembershipState());
        }

        member.setMembershipState("active");
        member.setUpdatedAt(Instant.now());
        return toMemberDto(companyMemberRepository.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyTaskDto> listTasks(UUID companyId, Long currentUserId) {
        Company company = getCompanyOrThrow(companyId);
        requireCompanyAccess(company, currentUserId);

        return companyTaskRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::toTaskDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CompanyTaskDto createTask(UUID companyId, CreateCompanyTaskRequest request, Long currentUserId) {
        Company company = getCompanyOrThrow(companyId);
        requireCompanyAccess(company, currentUserId);

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title is required");
        }

        CompanyTask task = CompanyTask.builder()
                .companyId(companyId)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .assigneeId(request.getAssigneeId())
                .state("todo")
                .createdBy(currentUserId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toTaskDto(companyTaskRepository.save(task));
    }

    @Override
    @Transactional
    public CompanyTaskDto updateTask(UUID companyId, UUID taskId, UpdateCompanyTaskRequest request, Long currentUserId) {
        Company company = getCompanyOrThrow(companyId);
        requireCompanyAccess(company, currentUserId);

        CompanyTask task = companyTaskRepository.findByCompanyIdAndId(companyId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (request.getState() != null && !request.getState().trim().isEmpty()) {
            task.setState(request.getState().trim().toLowerCase(Locale.ROOT));
        }
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription().trim());
        }
        if (request.getAssigneeId() != null) {
            task.setAssigneeId(request.getAssigneeId());
        }
        task.setUpdatedAt(Instant.now());

        return toTaskDto(companyTaskRepository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDocumentDto> listDocuments(UUID companyId, Long currentUserId) {
        Company company = getCompanyOrThrow(companyId);
        requireCompanyAccess(company, currentUserId);

        List<CompanyDocument> docs = companyDocumentRepository.findByCompanyIdAndIsArchivedFalseOrderByCreatedAtDesc(companyId);
        List<CompanyDocumentDto> result = new ArrayList<>();

        for (CompanyDocument doc : docs) {
            // Check admin_only access scope
            if ("admin_only".equalsIgnoreCase(doc.getAccessScope()) && !isCompanyAdmin(company, currentUserId)) {
                continue;
            }
            StorageObject storageObject = storageObjectRepository.findById(doc.getStorageObjectId()).orElse(null);
            result.add(toDocumentDto(doc, storageObject));
        }
        return result;
    }

    @Override
    @Transactional
    public CompanyDocumentDto createDocument(UUID companyId, CreateCompanyDocumentRequest request, Long currentUserId) {
        Company company = getCompanyOrThrow(companyId);
        requireCompanyAccess(company, currentUserId);

        if (request.getObjectId() == null) {
            throw new IllegalArgumentException("Storage object ID is required");
        }

        StorageObject storageObject = storageObjectRepository.findByIdAndIsDeletedFalse(request.getObjectId())
                .orElseThrow(() -> new IllegalArgumentException("Storage object not found or deleted: " + request.getObjectId()));

        if (storageObject.getUploadState() == UploadState.QUARANTINED) {
            throw new AccessDeniedException("Storage object is quarantined");
        }
        if (storageObject.getUploadState() != UploadState.COMPLETED) {
            throw new IllegalStateException("Storage object is not in COMPLETED state: " + storageObject.getUploadState());
        }
        if (!currentUserId.equals(storageObject.getOwnerId())) {
            throw new AccessDeniedException("Caller does not own this storage object");
        }
        if (storageObject.getWorkspaceId() != null && !storageObject.getWorkspaceId().equals(companyId)) {
            throw new AccessDeniedException("Storage object belongs to another workspace");
        }

        if (storageObject.getWorkspaceId() == null) {
            storageObject.setWorkspaceId(companyId);
            storageObjectRepository.save(storageObject);
        }

        String title = request.getTitle() != null && !request.getTitle().trim().isEmpty()
                ? request.getTitle().trim()
                : storageObject.getFilename();

        String collection = request.getCollection() != null && !request.getCollection().trim().isEmpty()
                ? request.getCollection().trim()
                : "General";

        String accessScope = request.getAccessScope() != null && !request.getAccessScope().trim().isEmpty()
                ? request.getAccessScope().trim().toLowerCase(Locale.ROOT)
                : "company";

        CompanyDocument document = CompanyDocument.builder()
                .companyId(companyId)
                .storageObjectId(storageObject.getId())
                .title(title)
                .collection(collection)
                .accessScope(accessScope)
                .createdBy(currentUserId)
                .isArchived(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toDocumentDto(companyDocumentRepository.save(document), storageObject);
    }

    @Override
    @Transactional
    public CompanyDownloadTicketDto downloadDocument(UUID companyId, UUID documentId, Long currentUserId) {
        Company company = getCompanyOrThrow(companyId);
        requireCompanyAccess(company, currentUserId);

        CompanyDocument document = companyDocumentRepository.findByCompanyIdAndIdAndIsArchivedFalse(companyId, documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if ("admin_only".equalsIgnoreCase(document.getAccessScope()) && !isCompanyAdmin(company, currentUserId)) {
            throw new AccessDeniedException("Admin access required to download this document");
        }

        return CompanyDownloadTicketDto.builder()
                .downloadUrl("/v1/companies/" + companyId + "/documents/" + documentId + "/raw")
                .expiresIn(900L)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StorageFileResource downloadDocumentFile(UUID companyId, UUID documentId, Long currentUserId) {
        Company company = getCompanyOrThrow(companyId);
        requireCompanyAccess(company, currentUserId);

        CompanyDocument document = companyDocumentRepository.findByCompanyIdAndIdAndIsArchivedFalse(companyId, documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if ("admin_only".equalsIgnoreCase(document.getAccessScope()) && !isCompanyAdmin(company, currentUserId)) {
            throw new AccessDeniedException("Admin access required to download this document");
        }

        StorageObject storageObject = storageObjectRepository.findByIdAndIsDeletedFalse(document.getStorageObjectId())
                .orElseThrow(() -> new IllegalArgumentException("Storage object not found: " + document.getStorageObjectId()));

        if (storageObject.getUploadState() != UploadState.COMPLETED) {
            throw new IllegalStateException("Storage object is not completed: " + storageObject.getUploadState());
        }

        InputStream stream = storageProvider.retrieveObject(storageObject.getStorageKey());
        return StorageFileResource.builder()
                .resource(new InputStreamResource(stream))
                .filename(document.getTitle() != null && !document.getTitle().isBlank() ? document.getTitle() : storageObject.getFilename())
                .contentType(storageObject.getContentType() != null ? storageObject.getContentType() : "application/octet-stream")
                .contentLength(storageObject.getVerifiedBytes() != null ? storageObject.getVerifiedBytes() : storageObject.getExpectedBytes())
                .build();
    }

    @Override
    @Transactional
    public void archiveDocument(UUID companyId, UUID documentId, Long currentUserId) {
        Company company = getCompanyOrThrow(companyId);
        requireCompanyAccess(company, currentUserId);

        CompanyDocument document = companyDocumentRepository.findByCompanyIdAndIdAndIsArchivedFalse(companyId, documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        boolean isCreator = currentUserId.equals(document.getCreatedBy());
        boolean isAdmin = isCompanyAdmin(company, currentUserId);

        if (!isCreator && !isAdmin) {
            throw new AccessDeniedException("Only the document creator or company admins can archive documents");
        }

        document.setIsArchived(true);
        document.setArchivedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        companyDocumentRepository.save(document);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserAuthorized(UUID companyId, Long currentUserId) {
        if (companyId == null || currentUserId == null) {
            return false;
        }
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            return false;
        }
        Company company = companyOpt.get();
        if (currentUserId.equals(company.getOwnerId())) {
            return true;
        }
        return companyMemberRepository.existsByCompanyIdAndUserIdAndMembershipState(companyId, currentUserId, "active");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean supportsWorkspace(UUID workspaceId) {
        if (workspaceId == null) {
            return false;
        }
        return companyRepository.existsById(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAuthorized(UUID workspaceId, Long userId) {
        return isUserAuthorized(workspaceId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAuthorizedForWrite(UUID workspaceId, Long userId) {
        if (workspaceId == null || userId == null) {
            return false;
        }
        Optional<Company> companyOpt = companyRepository.findById(workspaceId);
        if (companyOpt.isEmpty()) {
            return false;
        }
        Company company = companyOpt.get();
        if (userId.equals(company.getOwnerId())) {
            return true;
        }
        Optional<CompanyMember> memberOpt = companyMemberRepository.findByCompanyIdAndUserId(workspaceId, userId);
        if (memberOpt.isPresent()) {
            CompanyMember m = memberOpt.get();
            return "active".equalsIgnoreCase(m.getMembershipState());
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAuthorizedForStorageDownload(UUID workspaceId, UUID storageObjectId, Long userId) {
        if (workspaceId == null || storageObjectId == null || userId == null) {
            return false;
        }
        if (!supportsWorkspace(workspaceId)) {
            return false;
        }
        Optional<Company> companyOpt = companyRepository.findById(workspaceId);
        if (companyOpt.isEmpty()) {
            return false;
        }
        Company company = companyOpt.get();

        if (!isUserAuthorized(workspaceId, userId)) {
            return false;
        }

        List<CompanyDocument> docs = companyDocumentRepository.findByCompanyIdAndIsArchivedFalseOrderByCreatedAtDesc(workspaceId);
        Optional<CompanyDocument> docOpt = docs.stream()
                .filter(d -> storageObjectId.equals(d.getStorageObjectId()))
                .findFirst();

        if (docOpt.isEmpty()) {
            return false;
        }

        CompanyDocument doc = docOpt.get();
        if ("admin_only".equalsIgnoreCase(doc.getAccessScope())) {
            return isCompanyAdmin(company, userId);
        }
        return true;
    }

    private Company getCompanyOrThrow(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
    }

    private void requireCompanyAccess(Company company, Long currentUserId) {
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (currentUserId.equals(company.getOwnerId())) {
            return;
        }
        boolean isMember = companyMemberRepository.existsByCompanyIdAndUserIdAndMembershipState(company.getId(), currentUserId, "active");
        if (!isMember) {
            throw new AccessDeniedException("You do not have access to this company workspace");
        }
    }

    private void requireCompanyAdmin(Company company, Long currentUserId) {
        if (!isCompanyAdmin(company, currentUserId)) {
            throw new AccessDeniedException("Only company owners and admins can perform this action");
        }
    }

    private boolean isCompanyAdmin(Company company, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        if (currentUserId.equals(company.getOwnerId())) {
            return true;
        }
        Optional<CompanyMember> memberOpt = companyMemberRepository.findByCompanyIdAndUserId(company.getId(), currentUserId);
        if (memberOpt.isPresent()) {
            CompanyMember m = memberOpt.get();
            return "active".equalsIgnoreCase(m.getMembershipState()) &&
                    ("owner".equalsIgnoreCase(m.getRole()) || "admin".equalsIgnoreCase(m.getRole()));
        }
        return false;
    }

    private CompanyDto toCompanyDto(Company c) {
        return CompanyDto.builder()
                .id(c.getId())
                .ownerId(c.getOwnerId())
                .name(c.getName())
                .description(c.getDescription())
                .industry(c.getIndustry())
                .stage(c.getStage())
                .state(c.getState())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private CompanyMemberDto toMemberDto(CompanyMember m) {
        return CompanyMemberDto.builder()
                .id(m.getId())
                .companyId(m.getCompanyId())
                .userId(m.getUserId())
                .role(m.getRole())
                .membershipState(m.getMembershipState())
                .invitedBy(m.getInvitedBy())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private CompanyTaskDto toTaskDto(CompanyTask t) {
        return CompanyTaskDto.builder()
                .id(t.getId())
                .companyId(t.getCompanyId())
                .title(t.getTitle())
                .description(t.getDescription())
                .assigneeId(t.getAssigneeId())
                .state(t.getState())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private CompanyDocumentDto toDocumentDto(CompanyDocument d, StorageObject s) {
        return CompanyDocumentDto.builder()
                .id(d.getId())
                .companyId(d.getCompanyId())
                .objectId(d.getStorageObjectId())
                .title(d.getTitle())
                .filename(s != null ? s.getFilename() : d.getTitle())
                .contentType(s != null ? s.getContentType() : "application/octet-stream")
                .bytes(s != null ? (s.getVerifiedBytes() != null ? s.getVerifiedBytes() : s.getExpectedBytes()) : 0L)
                .collection(d.getCollection())
                .accessScope(d.getAccessScope())
                .createdBy(d.getCreatedBy())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
