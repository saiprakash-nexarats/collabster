package com.collabsphere_first.company.controller;

import com.collabsphere_first.company.dto.*;
import com.collabsphere_first.company.service.CompanyService;
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
@DisplayName("CompanyController Web Tier Tests (11 Operations)")
class CompanyControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CompanyService companyService;

    @Mock
    private CurrentUserService currentUserService;

    private User mockUser;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(300L);
        mockUser.setEmail("companyadmin@collobster.com");
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        companyId = UUID.randomUUID();
        CompanyController controller = new CompanyController(companyService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("1. GET /v1/companies returns user companies")
    void listCompanies() throws Exception {
        CompanyDto company = CompanyDto.builder().id(companyId).name("Nexarats").state("active").build();
        CompanyMembershipRowDto row = CompanyMembershipRowDto.builder()
                .company(company)
                .role("owner")
                .membershipState("active")
                .build();
        when(companyService.listUserCompanies(300L)).thenReturn(List.of(row));

        mockMvc.perform(get("/v1/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].company.name").value("Nexarats"));
    }

    @Test
    @DisplayName("2. POST /v1/companies creates company workspace")
    void createCompany() throws Exception {
        CreateCompanyRequest req = CreateCompanyRequest.builder().name("Nexarats Labs").industry("AI").build();
        CompanyDto company = CompanyDto.builder().id(companyId).name("Nexarats Labs").build();
        when(companyService.createCompany(any(), eq(300L))).thenReturn(company);

        mockMvc.perform(post("/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Nexarats Labs"));
    }

    @Test
    @DisplayName("3. POST /v1/companies/{id}/members invites team member")
    void inviteMember() throws Exception {
        InviteCompanyMemberRequest req = InviteCompanyMemberRequest.builder().profileId(400L).role("member").build();
        CompanyMemberDto member = CompanyMemberDto.builder().companyId(companyId).userId(400L).role("member").build();
        when(companyService.inviteMember(eq(companyId), any(), eq(300L))).thenReturn(member);

        mockMvc.perform(post("/v1/companies/" + companyId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(400L));
    }

    @Test
    @DisplayName("4. POST /v1/companies/{id}/membership/accept accepts invitation")
    void acceptInvitation() throws Exception {
        CompanyMemberDto member = CompanyMemberDto.builder().companyId(companyId).userId(300L).membershipState("active").build();
        when(companyService.acceptInvitation(companyId, 300L)).thenReturn(member);

        mockMvc.perform(post("/v1/companies/" + companyId + "/membership/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.membershipState").value("active"));
    }

    @Test
    @DisplayName("5. GET /v1/companies/{id}/tasks lists company tasks")
    void listTasks() throws Exception {
        CompanyTaskDto task = CompanyTaskDto.builder().id(UUID.randomUUID()).companyId(companyId).title("Q4 audit").build();
        when(companyService.listTasks(companyId, 300L)).thenReturn(List.of(task));

        mockMvc.perform(get("/v1/companies/" + companyId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Q4 audit"));
    }

    @Test
    @DisplayName("6. POST /v1/companies/{id}/tasks creates task")
    void createTask() throws Exception {
        CreateCompanyTaskRequest req = CreateCompanyTaskRequest.builder().title("Review financials").build();
        CompanyTaskDto task = CompanyTaskDto.builder().id(UUID.randomUUID()).companyId(companyId).title("Review financials").build();
        when(companyService.createTask(eq(companyId), any(), eq(300L))).thenReturn(task);

        mockMvc.perform(post("/v1/companies/" + companyId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Review financials"));
    }

    @Test
    @DisplayName("7. PATCH /v1/companies/{id}/tasks/{taskId} updates task")
    void updateTask() throws Exception {
        UUID taskId = UUID.randomUUID();
        UpdateCompanyTaskRequest req = UpdateCompanyTaskRequest.builder().state("done").build();
        CompanyTaskDto task = CompanyTaskDto.builder().id(taskId).companyId(companyId).state("done").build();
        when(companyService.updateTask(eq(companyId), eq(taskId), any(), eq(300L))).thenReturn(task);

        mockMvc.perform(patch("/v1/companies/" + companyId + "/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.state").value("done"));
    }

    @Test
    @DisplayName("8. GET /v1/companies/{id}/documents lists documents")
    void listDocuments() throws Exception {
        CompanyDocumentDto doc = CompanyDocumentDto.builder().id(UUID.randomUUID()).companyId(companyId).title("Pitch Deck").build();
        when(companyService.listDocuments(companyId, 300L)).thenReturn(List.of(doc));

        mockMvc.perform(get("/v1/companies/" + companyId + "/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Pitch Deck"));
    }

    @Test
    @DisplayName("9. POST /v1/companies/{id}/documents links document to storage")
    void createDocument() throws Exception {
        UUID objectId = UUID.randomUUID();
        CreateCompanyDocumentRequest req = CreateCompanyDocumentRequest.builder()
                .objectId(objectId)
                .title("Financial Statement")
                .collection("Finance")
                .build();
        CompanyDocumentDto doc = CompanyDocumentDto.builder().id(UUID.randomUUID()).companyId(companyId).objectId(objectId).title("Financial Statement").build();
        when(companyService.createDocument(eq(companyId), any(), eq(300L))).thenReturn(doc);

        mockMvc.perform(post("/v1/companies/" + companyId + "/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.objectId").value(objectId.toString()));
    }

    @Test
    @DisplayName("10. GET /v1/companies/{id}/documents/{documentId}/download gets download ticket")
    void downloadDocument() throws Exception {
        UUID documentId = UUID.randomUUID();
        CompanyDownloadTicketDto ticket = CompanyDownloadTicketDto.builder()
                .downloadUrl("/api/v1/storage/files/123/download")
                .expiresIn(900L)
                .build();
        when(companyService.downloadDocument(companyId, documentId, 300L)).thenReturn(ticket);

        mockMvc.perform(get("/v1/companies/" + companyId + "/documents/" + documentId + "/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.downloadUrl").value("/api/v1/storage/files/123/download"));
    }

    @Test
    @DisplayName("11. DELETE /v1/companies/{id}/documents/{documentId} archives document")
    void archiveDocument() throws Exception {
        UUID documentId = UUID.randomUUID();
        doNothing().when(companyService).archiveDocument(companyId, documentId, 300L);

        mockMvc.perform(delete("/v1/companies/" + companyId + "/documents/" + documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document archived successfully"));
    }
}
