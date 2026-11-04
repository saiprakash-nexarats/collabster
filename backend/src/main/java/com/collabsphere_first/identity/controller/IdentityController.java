package com.collabsphere_first.identity.controller;

import com.collabsphere_first.account.service.AccountLifecycleService;
import com.collabsphere_first.explore.repository.ExploreUserRepository;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.Gender;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * Authoritative Identity REST Controller satisfying web and mobile clients
 * for /v1/identity and /api/v1/identity contracts.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class IdentityController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final ExploreUserRepository exploreUserRepository;
    private final AccountLifecycleService accountLifecycleService;

    @GetMapping({"/api/v1/identity/me", "/v1/identity/me"})
    public ResponseEntity<Map<String, Object>> getMe() {
        User user = currentUserService.getCurrentUser();
        return ResponseEntity.ok(toProfileMap(user));
    }

    @PatchMapping({"/api/v1/identity/me", "/v1/identity/me"})
    public ResponseEntity<Map<String, Object>> updateMe(@RequestBody Map<String, Object> updates) {
        User user = currentUserService.getCurrentUser();

        if (updates.containsKey("displayName")) {
            String displayName = (String) updates.get("displayName");
            if (displayName != null && !displayName.isBlank()) {
                String[] parts = displayName.trim().split("\\s+", 2);
                user.setFirstName(parts[0]);
                user.setLastName(parts.length > 1 ? parts[1] : "");
            }
        }

        if (updates.containsKey("username") && updates.get("username") != null) {
            String newUsername = ((String) updates.get("username")).trim();
            if (!newUsername.equalsIgnoreCase(user.getUsername())) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new IllegalArgumentException("Username is already taken");
                }
                user.setUsername(newUsername);
            }
        }

        if (updates.containsKey("bio")) {
            user.setBio((String) updates.get("bio"));
        } else if (updates.containsKey("headline") && user.getBio() == null) {
            user.setBio((String) updates.get("headline"));
        }

        if (updates.containsKey("location")) {
            String loc = (String) updates.get("location");
            if (loc != null && !loc.isBlank()) {
                String[] parts = loc.split(",", 2);
                user.setCity(parts[0].trim());
                if (parts.length > 1) {
                    user.setCountry(parts[1].trim());
                }
            }
        }

        if (updates.containsKey("birthDate") && updates.get("birthDate") != null) {
            try {
                user.setDateOfBirth(LocalDate.parse(updates.get("birthDate").toString()));
            } catch (Exception ignored) {}
        }

        if (updates.containsKey("gender") && updates.get("gender") != null) {
            try {
                user.setGender(Gender.valueOf(updates.get("gender").toString().toUpperCase()));
            } catch (Exception ignored) {}
        }

        if (updates.containsKey("avatarUrl")) {
            user.setProfilePicture((String) updates.get("avatarUrl"));
        }

        user.setProfileCompleted(true);
        User saved = userRepository.save(user);
        return ResponseEntity.ok(toProfileMap(saved));
    }

    @DeleteMapping({"/api/v1/identity/me", "/v1/identity/me"})
    public ResponseEntity<Void> deleteMe() {
        User user = currentUserService.getCurrentUser();
        accountLifecycleService.deleteAccount(user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/api/v1/identity/profiles/{id}", "/v1/identity/profiles/{id}"})
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return ResponseEntity.ok(toProfileMap(user));
    }

    @GetMapping({"/api/v1/identity/profiles", "/v1/identity/profiles"})
    public ResponseEntity<List<Map<String, Object>>> searchProfiles(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit
    ) {
        User currentUser = null;
        try {
            currentUser = currentUserService.getCurrentUser();
        } catch (Exception ignored) {}

        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        List<User> users;
        if (query != null && !query.trim().isEmpty()) {
            users = exploreUserRepository.searchUsers(query.trim(), currentUserId, Collections.emptySet(), Math.min(limit, 50));
        } else {
            users = userRepository.findAll().stream().limit(Math.min(limit, 50)).toList();
        }

        List<Map<String, Object>> response = users.stream().map(u -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("displayName", (u.getFirstName() + " " + (u.getLastName() != null ? u.getLastName() : "")).trim());
            item.put("headline", u.getBio() != null ? u.getBio() : "Member");
            item.put("location", (u.getCity() != null ? u.getCity() + ", " : "") + (u.getCountry() != null ? u.getCountry() : "India"));
            item.put("avatarUrl", u.getProfilePicture());
            return item;
        }).toList();

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toProfileMap(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("displayName", (user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")).trim());
        profile.put("username", user.getUsername() != null ? user.getUsername() : "");
        profile.put("headline", user.getBio() != null ? user.getBio() : "");
        profile.put("bio", user.getBio() != null ? user.getBio() : "");
        profile.put("location", (user.getCity() != null ? user.getCity() + ", " : "") + (user.getCountry() != null ? user.getCountry() : ""));
        profile.put("birthDate", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "");
        profile.put("gender", user.getGender() != null ? user.getGender().name() : "");
        profile.put("avatarUrl", user.getProfilePicture());
        profile.put("coverUrl", null);
        profile.put("skills", Collections.emptyList());
        profile.put("interests", Collections.emptyList());

        Map<String, Object> privacy = new LinkedHashMap<>();
        privacy.put("profileVisibility", "public");
        profile.put("privacy", privacy);

        return profile;
    }
}
