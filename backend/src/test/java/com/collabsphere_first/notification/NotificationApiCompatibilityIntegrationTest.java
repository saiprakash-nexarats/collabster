package com.collabsphere_first.notification;

import com.collabsphere_first.community.entity.CommunityNotification;
import com.collabsphere_first.community.repository.CommunityNotificationRepository;
import com.collabsphere_first.notification.entity.Notification;
import com.collabsphere_first.notification.enums.NotificationCategory;
import com.collabsphere_first.notification.enums.NotificationType;
import com.collabsphere_first.notification.repository.NotificationRepository;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.userdetails.CustomUserDetailsService;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/collabster",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=validate",
        "collobster.notifications.push.enabled=true",
        "collobster.notifications.email.enabled=true",
        "collobster.notifications.realtime.enabled=true"
})
@AutoConfigureMockMvc
@Transactional
public class NotificationApiCompatibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityNotificationRepository communityNotificationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private User alice;
    private User bob;
    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() {
        String aliceEmail = "compat-alice-" + UUID.randomUUID() + "@example.com";
        String bobEmail = "compat-bob-" + UUID.randomUUID() + "@example.com";

        alice = userRepository.save(User.builder()
                .email(aliceEmail)
                .username("compat_alice_" + UUID.randomUUID().toString().substring(0, 8))
                .password("hash123")
                .firstName("Alice")
                .lastName("Compatibility")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        bob = userRepository.save(User.builder()
                .email(bobEmail)
                .username("compat_bob_" + UUID.randomUUID().toString().substring(0, 8))
                .password("hash123")
                .firstName("Bob")
                .lastName("Compatibility")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        aliceToken = "Bearer " + jwtService.generateAccessToken(userDetailsService.loadUserByUsername(aliceEmail));
        bobToken = "Bearer " + jwtService.generateAccessToken(userDetailsService.loadUserByUsername(bobEmail));
    }

    // =========================================================================
    // 1. NO AMBIGUOUS SPRING MAPPINGS COEXISTENCE
    // =========================================================================

    @Test
    @DisplayName("Verify Spring RequestMappingHandlerMapping contains non-conflicting routes")
    void testNoAmbiguousSpringMappings() {
        var handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        assertThat(handlerMethods).isNotEmpty();

        boolean hasLegacyCommunityList = false;
        boolean hasCanonicalCommunityList = false;
        boolean hasGlobalNotificationCenterList = false;
        boolean hasGlobalNotificationCenterDevices = false;
        boolean hasGlobalNotificationCenterPreferences = false;

        for (var entry : handlerMethods.entrySet()) {
            var patterns = entry.getKey().getPatternValues();
            if (patterns.contains("/api/v1/notifications")) {
                hasLegacyCommunityList = true;
            }
            if (patterns.contains("/api/v1/communities/notifications")) {
                hasCanonicalCommunityList = true;
            }
            if (patterns.contains("/api/v1/notification-center")) {
                hasGlobalNotificationCenterList = true;
            }
            if (patterns.contains("/api/v1/notification-center/devices")) {
                hasGlobalNotificationCenterDevices = true;
            }
            if (patterns.contains("/api/v1/notification-center/preferences")) {
                hasGlobalNotificationCenterPreferences = true;
            }
        }

        assertThat(hasLegacyCommunityList).isTrue();
        assertThat(hasCanonicalCommunityList).isTrue();
        assertThat(hasGlobalNotificationCenterList).isTrue();
        assertThat(hasGlobalNotificationCenterDevices).isTrue();
        assertThat(hasGlobalNotificationCenterPreferences).isTrue();
    }

    // =========================================================================
    // 2. COMMUNITY LEGACY REST CONTRACT TESTS (/api/v1/notifications)
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/notifications returns historical Page<CommunityNotificationDto> format")
    void testLegacyCommunityGetContract() throws Exception {
        communityNotificationRepository.save(CommunityNotification.builder()
                .userId(alice.getId())
                .communityId(UUID.randomUUID())
                .type("NEW_POST")
                .title("Community Legacy Title")
                .body("Community Legacy Body")
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].title").value("Community Legacy Title"))
                .andExpect(jsonPath("$.data.content[0].type").value("NEW_POST"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count returns historical Long unread count")
    void testLegacyCommunityUnreadCountContract() throws Exception {
        communityNotificationRepository.save(CommunityNotification.builder()
                .userId(alice.getId())
                .type("MEMBERSHIP_ACCEPTED")
                .title("Welcome")
                .body("You are now a member")
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read and read-all update Community notifications")
    void testLegacyCommunityPatchRead() throws Exception {
        CommunityNotification cn = communityNotificationRepository.save(CommunityNotification.builder()
                .userId(alice.getId())
                .type("INVITATION")
                .title("Invited")
                .body("You were invited")
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(patch("/api/v1/notifications/" + cn.getId() + "/read")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 3. COMMUNITY CANONICAL REST CONTRACT TESTS (/api/v1/communities/notifications)
    // =========================================================================

    @Test
    @DisplayName("Canonical Community routes /api/v1/communities/notifications/** work identically")
    void testCanonicalCommunityEndpoints() throws Exception {
        CommunityNotification cn = communityNotificationRepository.save(CommunityNotification.builder()
                .userId(alice.getId())
                .type("POLL_CREATED")
                .title("New Poll")
                .body("Vote now")
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/communities/notifications")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].type").value("POLL_CREATED"));

        mockMvc.perform(get("/api/v1/communities/notifications/unread-count")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(patch("/api/v1/communities/notifications/" + cn.getId() + "/read")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/communities/notifications/read-all")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 4. GLOBAL NOTIFICATION CENTER REST CONTRACT TESTS (/api/v1/notification-center)
    // =========================================================================

    @Test
    @DisplayName("Global Notification Center REST contract works under /api/v1/notification-center/**")
    void testGlobalNotificationCenterContract() throws Exception {
        Notification globalNotif = notificationRepository.save(Notification.builder()
                .recipientUserId(alice.getId())
                .actorUserId(bob.getId())
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .eventKey("GLOBAL_CONTRACT_TEST:" + UUID.randomUUID())
                .createdAt(Instant.now())
                .build());

        // 1. GET list with cursor pagination structure
        mockMvc.perform(get("/api/v1/notification-center")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].id").value(globalNotif.getId().toString()))
                .andExpect(jsonPath("$.data.hasMore").isBoolean());

        // 2. GET single notification
        mockMvc.perform(get("/api/v1/notification-center/" + globalNotif.getId())
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(globalNotif.getId().toString()))
                .andExpect(jsonPath("$.data.category").value("SOCIAL"));

        // 3. POST seen
        mockMvc.perform(post("/api/v1/notification-center/" + globalNotif.getId() + "/seen")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seenAt").isNotEmpty());

        // 4. POST read
        mockMvc.perform(post("/api/v1/notification-center/" + globalNotif.getId() + "/read")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        // 5. POST archive
        mockMvc.perform(post("/api/v1/notification-center/" + globalNotif.getId() + "/archive")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archivedAt").isNotEmpty());

        // 6. POST read-all
        mockMvc.perform(post("/api/v1/notification-center/read-all")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").isNumber());

        // 7. GET unread-count and summary
        mockMvc.perform(get("/api/v1/notification-center/unread-count")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").isNumber());

        mockMvc.perform(get("/api/v1/notification-center/summary")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").isNumber());

        // 8. GET & PATCH preferences
        mockMvc.perform(get("/api/v1/notification-center/preferences")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(patch("/api/v1/notification-center/preferences")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"SOCIAL\",\"pushEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("SOCIAL"))
                .andExpect(jsonPath("$.data.pushEnabled").value(false));

        // 9. POST, GET & DELETE devices
        String deviceToken = "test-token-compat-" + UUID.randomUUID();
        String registerJson = "{\"platform\":\"ANDROID\",\"provider\":\"FCM\",\"token\":\"" + deviceToken + "\"}";

        var regResult = mockMvc.perform(post("/api/v1/notification-center/devices")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn();

        String deviceId = com.jayway.jsonpath.JsonPath.read(regResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/v1/notification-center/devices")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(deviceId));

        mockMvc.perform(delete("/api/v1/notification-center/devices/" + deviceId)
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 5. UNREAD COUNT SEPARATION (COMMUNITY vs GLOBAL)
    // =========================================================================

    @Test
    @DisplayName("Community unread count and Global unread count remain strictly separate")
    void testUnreadCountSeparation() throws Exception {
        // Create 3 unread Community notifications for Alice
        for (int i = 0; i < 3; i++) {
            communityNotificationRepository.save(CommunityNotification.builder()
                    .userId(alice.getId())
                    .type("NEW_POST")
                    .title("Community Post " + i)
                    .body("Body " + i)
                    .createdAt(Instant.now())
                    .build());
        }

        // Create 8 unread Global notifications for Alice
        for (int i = 0; i < 8; i++) {
            notificationRepository.save(Notification.builder()
                    .recipientUserId(alice.getId())
                    .actorUserId(bob.getId())
                    .type(NotificationType.NEW_FOLLOW)
                    .category(NotificationCategory.SOCIAL)
                    .eventKey("UNREAD_SEP_TEST:" + UUID.randomUUID())
                    .createdAt(Instant.now())
                    .build());
        }

        // 1. Community unread endpoint returns 3
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));

        mockMvc.perform(get("/api/v1/communities/notifications/unread-count")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));

        // 2. Global unread endpoint returns 8
        mockMvc.perform(get("/api/v1/notification-center/unread-count")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(8));
    }

    // =========================================================================
    // 6. DOMAIN STATE SEPARATION (READ STATE INDEPENDENCE)
    // =========================================================================

    @Test
    @DisplayName("Marking Global notification read does NOT mutate Community notification row, and vice versa")
    void testDomainStateSeparation() throws Exception {
        CommunityNotification cn = communityNotificationRepository.save(CommunityNotification.builder()
                .userId(alice.getId())
                .type("EVENT_INVITE")
                .title("Community Event")
                .body("Join us")
                .createdAt(Instant.now())
                .readAt(null)
                .build());

        Notification gn = notificationRepository.save(Notification.builder()
                .recipientUserId(alice.getId())
                .actorUserId(bob.getId())
                .type(NotificationType.DIRECT_MESSAGE)
                .category(NotificationCategory.MESSAGING)
                .eventKey("STATE_SEP_TEST:" + UUID.randomUUID())
                .createdAt(Instant.now())
                .readAt(null)
                .build());

        // 1. Read Global notification
        mockMvc.perform(post("/api/v1/notification-center/" + gn.getId() + "/read")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk());

        // Verify Global row is READ, Community row remains UNREAD
        Notification gnReloaded = notificationRepository.findById(gn.getId()).orElseThrow();
        CommunityNotification cnReloaded = communityNotificationRepository.findById(cn.getId()).orElseThrow();
        assertThat(gnReloaded.getReadAt()).isNotNull();
        assertThat(cnReloaded.getReadAt()).isNull();

        // 2. Read Community notification
        mockMvc.perform(patch("/api/v1/notifications/" + cn.getId() + "/read")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk());

        CommunityNotification cnReloaded2 = communityNotificationRepository.findById(cn.getId()).orElseThrow();
        assertThat(cnReloaded2.getReadAt()).isNotNull();
    }

    // =========================================================================
    // 7. IDOR SECURITY REGRESSION
    // =========================================================================

    @Test
    @DisplayName("Users cannot view or mutate another user's Community or Global notifications")
    void testIdorSecurityProtection() throws Exception {
        CommunityNotification aliceCn = communityNotificationRepository.save(CommunityNotification.builder()
                .userId(alice.getId())
                .type("SECRET_POST")
                .title("Alice Only")
                .body("Do not share")
                .createdAt(Instant.now())
                .build());

        Notification aliceGn = notificationRepository.save(Notification.builder()
                .recipientUserId(alice.getId())
                .actorUserId(bob.getId())
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .eventKey("IDOR_TEST:" + UUID.randomUUID())
                .createdAt(Instant.now())
                .build());

        // Bob attempts to read Alice's Community notification -> Expect 400 (InvalidJoinException) or 403
        mockMvc.perform(patch("/api/v1/notifications/" + aliceCn.getId() + "/read")
                        .header("Authorization", bobToken))
                .andExpect(status().is4xxClientError());

        // Bob attempts to read Alice's Global notification -> Expect 403 Forbidden
        mockMvc.perform(get("/api/v1/notification-center/" + aliceGn.getId())
                        .header("Authorization", bobToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/notification-center/" + aliceGn.getId() + "/read")
                        .header("Authorization", bobToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/notification-center/" + aliceGn.getId() + "/archive")
                        .header("Authorization", bobToken))
                .andExpect(status().isForbidden());
    }
}
