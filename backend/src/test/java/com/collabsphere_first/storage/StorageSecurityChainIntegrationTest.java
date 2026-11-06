package com.collabsphere_first.storage;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.security.CommunityRoleService;
import com.collabsphere_first.community.service.CommunityMembershipCacheService;
import com.collabsphere_first.security.config.SecurityConfig;
import com.collabsphere_first.security.jwt.JwtAuthenticationFilter;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.oauth2.CustomOAuth2UserService;
import com.collabsphere_first.security.oauth2.OAuth2FailureHandler;
import com.collabsphere_first.security.oauth2.OAuth2SuccessHandler;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.security.userdetails.CustomUserDetailsService;
import com.collabsphere_first.storage.controller.StorageController;
import com.collabsphere_first.storage.controller.StorageExceptionHandler;
import com.collabsphere_first.storage.dto.StorageFileResource;
import com.collabsphere_first.storage.dto.StorageObjectResponse;
import com.collabsphere_first.storage.dto.UploadInitiationRequest;
import com.collabsphere_first.storage.dto.UploadTicketResponse;
import com.collabsphere_first.storage.exception.StorageAccessDeniedException;
import com.collabsphere_first.storage.exception.UploadExpiredException;
import com.collabsphere_first.storage.service.StorageManagementService;
import com.collabsphere_first.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StorageController.class)
@Import({SecurityConfig.class, StorageExceptionHandler.class, StorageSecurityChainIntegrationTest.TestFilterConfig.class})
@AutoConfigureMockMvc
@DisplayName("Stage 5A — Storage Security Filter Chain Integration Tests")
public class StorageSecurityChainIntegrationTest {

    @TestConfiguration
    static class TestFilterConfig {
        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StorageManagementService storageManagementService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private MembershipRepository membershipRepository;

    @MockBean
    private CommunityRoleService communityRoleService;

    @MockBean
    private CommunityMembershipCacheService membershipCacheService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User authUser;

    @BeforeEach
    void setUp() {
        authUser = new User();
        authUser.setId(100L);
        authUser.setEmail("user100@collobster.com");
    }

    @Test
    @DisplayName("1. Upload Initiation: Unauthenticated request to /v1/storage/uploads returns 401 Unauthorized")
    void uploadInitiation_unauthenticated_returns401() throws Exception {
        UploadInitiationRequest req = UploadInitiationRequest.builder()
                .filename("document.pdf")
                .contentType("application/pdf")
                .contentLength(1024L)
                .build();

        mockMvc.perform(post("/v1/storage/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "100")
    @DisplayName("2. Upload Initiation: Authenticated request returns 201 Created with signed upload ticket")
    void uploadInitiation_authenticated_returns201() throws Exception {
        when(currentUserService.getCurrentUser()).thenReturn(authUser);

        UUID ticketId = UUID.randomUUID();
        UploadTicketResponse ticket = UploadTicketResponse.builder()
                .id(ticketId)
                .objectId(ticketId)
                .uploadUrl("http://localhost:8080/v1/storage/raw/" + ticketId + "?expires=123456&signature=validsig")
                .storageKey("uploads/100/test.pdf")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        when(storageManagementService.requestUpload(any(UploadInitiationRequest.class), eq(100L)))
                .thenReturn(ticket);

        UploadInitiationRequest req = UploadInitiationRequest.builder()
                .filename("test.pdf")
                .contentType("application/pdf")
                .contentLength(1024L)
                .build();

        mockMvc.perform(post("/v1/storage/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.objectId").value(ticketId.toString()))
                .andExpect(jsonPath("$.data.uploadUrl").exists());
    }

    @Test
    @DisplayName("3. Direct Raw PUT: permitAll route allows raw binary PUT with valid signature and query parameters returning 200 OK")
    void rawPut_permitAll_validSignature_returns200() throws Exception {
        UUID objectId = UUID.randomUUID();
        long expires = Instant.now().plus(15, ChronoUnit.MINUTES).getEpochSecond();
        String signature = "valid_hmac_signature";

        doNothing().when(storageManagementService).handleDirectUpload(
                eq(objectId), any(), any(), any(), eq(expires), eq(signature)
        );

        mockMvc.perform(put("/v1/storage/raw/{id}", objectId)
                        .param("expires", String.valueOf(expires))
                        .param("signature", signature)
                        .contentType(MediaType.APPLICATION_PDF)
                        .content(new byte[1024]))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("4. Direct Raw PUT: forged or missing signature returns 403 Forbidden")
    void rawPut_forgedOrMissingSignature_returns403() throws Exception {
        UUID objectId = UUID.randomUUID();
        long expires = Instant.now().plus(15, ChronoUnit.MINUTES).getEpochSecond();
        String forgedSignature = "forged_signature_xyz";

        doThrow(new StorageAccessDeniedException("Invalid upload signature"))
                .when(storageManagementService).handleDirectUpload(
                        eq(objectId), any(), any(), any(), eq(expires), eq(forgedSignature)
                );

        mockMvc.perform(put("/v1/storage/raw/{id}", objectId)
                        .param("expires", String.valueOf(expires))
                        .param("signature", forgedSignature)
                        .contentType(MediaType.APPLICATION_PDF)
                        .content(new byte[1024]))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid upload signature"));
    }

    @Test
    @DisplayName("5. Direct Raw PUT: expired upload ticket returns 410 Gone")
    void rawPut_expiredTicket_returns410() throws Exception {
        UUID objectId = UUID.randomUUID();
        long expiredTimestamp = Instant.now().minus(5, ChronoUnit.MINUTES).getEpochSecond();
        String signature = "valid_sig_but_expired";

        doThrow(new UploadExpiredException("Upload ticket has expired"))
                .when(storageManagementService).handleDirectUpload(
                        eq(objectId), any(), any(), any(), eq(expiredTimestamp), eq(signature)
                );

        mockMvc.perform(put("/v1/storage/raw/{id}", objectId)
                        .param("expires", String.valueOf(expiredTimestamp))
                        .param("signature", signature)
                        .contentType(MediaType.APPLICATION_PDF)
                        .content(new byte[1024]))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Upload ticket has expired"));
    }

    @Test
    @DisplayName("6. Private Download: Unauthenticated download returns 401 Unauthorized")
    void privateDownload_unauthenticated_returns401() throws Exception {
        UUID objectId = UUID.randomUUID();

        mockMvc.perform(get("/v1/storage/files/{id}/download", objectId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "200")
    @DisplayName("7. Private Download: Authenticated outsider unauthorized user returns 403 Forbidden")
    void privateDownload_unauthorizedUser_returns403() throws Exception {
        User outsider = new User();
        outsider.setId(200L);
        outsider.setEmail("outsider@collobster.com");
        when(currentUserService.getCurrentUser()).thenReturn(outsider);

        UUID objectId = UUID.randomUUID();
        when(storageManagementService.downloadFile(eq(objectId), eq(200L)))
                .thenThrow(new StorageAccessDeniedException("Unauthorized to download: User is not authorized to download file: " + objectId));

        mockMvc.perform(get("/v1/storage/files/{id}/download", objectId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized to download: User is not authorized to download file: " + objectId));
    }
}
