package com.collabsphere_first.company.service;

import com.collabsphere_first.company.dto.*;

import java.util.List;
import java.util.UUID;

public interface CompanyService {
    List<CompanyMembershipRowDto> listUserCompanies(Long currentUserId);
    CompanyDto createCompany(CreateCompanyRequest request, Long currentUserId);
    CompanyMemberDto inviteMember(UUID companyId, InviteCompanyMemberRequest request, Long currentUserId);
    CompanyMemberDto acceptInvitation(UUID companyId, Long currentUserId);
    List<CompanyTaskDto> listTasks(UUID companyId, Long currentUserId);
    CompanyTaskDto createTask(UUID companyId, CreateCompanyTaskRequest request, Long currentUserId);
    CompanyTaskDto updateTask(UUID companyId, UUID taskId, UpdateCompanyTaskRequest request, Long currentUserId);
    List<CompanyDocumentDto> listDocuments(UUID companyId, Long currentUserId);
    CompanyDocumentDto createDocument(UUID companyId, CreateCompanyDocumentRequest request, Long currentUserId);
    CompanyDownloadTicketDto downloadDocument(UUID companyId, UUID documentId, Long currentUserId);
    void archiveDocument(UUID companyId, UUID documentId, Long currentUserId);
    boolean isUserAuthorized(UUID companyId, Long currentUserId);
}
