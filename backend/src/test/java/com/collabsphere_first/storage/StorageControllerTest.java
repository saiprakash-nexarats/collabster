package com.collabsphere_first.storage;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.storage.controller.StorageController;
import com.collabsphere_first.storage.controller.StorageExceptionHandler;
import com.collabsphere_first.storage.dto.StorageFileResource;
import com.collabsphere_first.storage.dto.StorageObjectResponse;
import com.collabsphere_first.storage.dto.UploadInitiationRequest;
import com.collabsphere_first.storage.dto.UploadTicketResponse;
import com.collabsphere_first.storage.exception.InvalidUploadException;
import com.collabsphere_first.storage.exception.StorageAccessDeniedException;
import com.collabsphere_first.storage.exception.StorageObjectNotFoundException;
import com.collabsphere_first.storage.service.StorageManagementService;
import com.collabsphere_first.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Stage 5A — Storage Controller Web Tier Tests")
class StorageControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private StorageManagementService storageManagementService;

    @Mock
    private CurrentUserService currentUserService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(42L);
        mockUser.setEmail("developer@collobster.com");

        StorageController controller = new StorageController(storageManagementService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new StorageExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /v1/storage/uploads returns 201 Created with upload ticket")
    void requestUpload_returns201() throws Exception {
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        UUID objectId = UUID.randomUUID();
        UploadTicketResponse ticket = UploadTicketResponse.builder()
                .id(objectId)
                .objectId(objectId)
                .uploadUrl("/v1/storage/raw/" + objectId + "?expires=123&signature=abc")
                .storageKey("uploads/42/file.png")
                .filename("file.png")
                .contentType("image/png")
                .expectedBytes(2048L)
                .expiresAt(Instant.now().plusSeconds(900))
                .state("PENDING")
                .build();

        when(storageManagementService.requestUpload(any(UploadInitiationRequest.class), eq(42L)))
                .thenReturn(ticket);

        UploadInitiationRequest req = UploadInitiationRequest.builder()
                .filename("file.png")
                .contentType("image/png")
                .contentLength(2048L)
                .build();

        mockMvc.perform(post("/v1/storage/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(objectId.toString()))
                .andExpect(jsonPath("$.data.objectId").value(objectId.toString()))
                .andExpect(jsonPath("$.data.uploadUrl").exists())
                .andExpect(jsonPath("$.data.state").value("PENDING"));
    }

    @Test
    @DisplayName("PUT /v1/storage/raw/{id} streams binary payload and returns 200 OK")
    void directUpload_returns200() throws Exception {
        UUID objectId = UUID.randomUUID();

        doNothing().when(storageManagementService).handleDirectUpload(
                eq(objectId), any(), any(), any(), eq(1234567890L), eq("valid-sig"));

        mockMvc.perform(put("/v1/storage/raw/" + objectId)
                        .param("expires", "1234567890")
                        .param("signature", "valid-sig")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("RAW_BYTES_PAYLOAD"))
                .andExpect(status().isOk());

        verify(storageManagementService, times(1)).handleDirectUpload(
                eq(objectId), any(), any(), any(), eq(1234567890L), eq("valid-sig"));
    }

    @Test
    @DisplayName("POST /v1/storage/uploads/{id}/complete returns 200 OK with verified metadata")
    void completeUpload_returns200() throws Exception {
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        UUID objectId = UUID.randomUUID();
        StorageObjectResponse response = StorageObjectResponse.builder()
                .id(objectId)
                .objectId(objectId)
                .filename("doc.pdf")
                .contentType("application/pdf")
                .verifiedBytes(5000L)
                .expectedBytes(5000L)
                .state("COMPLETED")
                .storageKey("uploads/42/doc.pdf")
                .downloadUrl("/v1/storage/files/" + objectId + "/download")
                .checksum("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .build();

        when(storageManagementService.completeUpload(eq(objectId), eq(42L)))
                .thenReturn(response);

        mockMvc.perform(post("/v1/storage/uploads/" + objectId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.state").value("COMPLETED"))
                .andExpect(jsonPath("$.data.verifiedBytes").value(5000))
                .andExpect(jsonPath("$.data.downloadUrl").value("/v1/storage/files/" + objectId + "/download"));
    }

    @Test
    @DisplayName("GET /v1/storage/files/{id}/download returns file resource and headers")
    void downloadFile_returnsResource() throws Exception {
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        UUID objectId = UUID.randomUUID();
        byte[] content = "File binary content".getBytes();
        StorageFileResource res = StorageFileResource.builder()
                .resource(new ByteArrayResource(content))
                .filename("report.pdf")
                .contentType("application/pdf")
                .contentLength(content.length)
                .build();

        when(storageManagementService.downloadFile(eq(objectId), eq(42L)))
                .thenReturn(res);

        mockMvc.perform(get("/v1/storage/files/" + objectId + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"report.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(content));
    }

    @Test
    @DisplayName("GET /v1/storage/files/{id}/download unauthorized caller returns 403 Forbidden")
    void downloadFile_unauthorized_returns403() throws Exception {
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        UUID objectId = UUID.randomUUID();
        when(storageManagementService.downloadFile(eq(objectId), eq(42L)))
                .thenThrow(new StorageAccessDeniedException("Unauthorized to download"));

        mockMvc.perform(get("/v1/storage/files/" + objectId + "/download"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized to download"));
    }

    @Test
    @DisplayName("DELETE /v1/storage/files/{id} returns 204 No Content")
    void deleteFile_returns204() throws Exception {
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        UUID objectId = UUID.randomUUID();
        doNothing().when(storageManagementService).deleteFile(eq(objectId), eq(42L));

        mockMvc.perform(delete("/v1/storage/files/" + objectId))
                .andExpect(status().isNoContent());

        verify(storageManagementService, times(1)).deleteFile(eq(objectId), eq(42L));
    }
}

