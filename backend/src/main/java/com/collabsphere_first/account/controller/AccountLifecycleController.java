package com.collabsphere_first.account.controller;

import com.collabsphere_first.account.service.AccountLifecycleService;
import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AccountLifecycleController {

    private final AccountLifecycleService accountLifecycleService;
    private final CurrentUserService currentUserService;

    /**
     * Deactivate the current authenticated user's account.
     */
    @PostMapping("/api/v1/account/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        accountLifecycleService.deactivateAccount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully", null));
    }

    /**
     * Permanently delete and anonymize the current authenticated user's account.
     */
    @DeleteMapping("/api/v1/account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        accountLifecycleService.deleteAccount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Account deleted and personal data anonymized successfully", null));
    }

    /**
     * Suspend a user account (Platform Admin only).
     */
    @PostMapping("/api/v1/admin/users/{userId}/suspend")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "Administrative action") String reason
    ) {
        Long adminId = currentUserService.getCurrentUser().getId();
        accountLifecycleService.suspendAccount(adminId, userId, reason);
        return ResponseEntity.ok(ApiResponse.success("User suspended successfully", null));
    }

    /**
     * Reactivate a suspended user account (Platform Admin only).
     */
    @PostMapping("/api/v1/admin/users/{userId}/reactivate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(@PathVariable Long userId) {
        Long adminId = currentUserService.getCurrentUser().getId();
        accountLifecycleService.reactivateAccount(adminId, userId);
        return ResponseEntity.ok(ApiResponse.success("User reactivated successfully", null));
    }
}
