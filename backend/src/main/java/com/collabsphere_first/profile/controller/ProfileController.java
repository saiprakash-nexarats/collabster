package com.collabsphere_first.profile.controller;

import com.collabsphere_first.profile.dto.request.CreateProfileRequest;
import com.collabsphere_first.profile.dto.response.ProfileResponse;
import com.collabsphere_first.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(
            @Valid @RequestBody CreateProfileRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.createProfile(request));
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getCurrentUserProfile() {
        return ResponseEntity.ok(profileService.getCurrentUserProfile());
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            @Valid @RequestBody CreateProfileRequest request
    ) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }
}
