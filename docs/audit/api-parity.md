# Collobster API Parity Inventory & Architecture Contract

**Repository:** `https://github.com/saiprakash-nexarats/collabster`  
**Branch:** `main`  
**Specification Date:** 2026-11-04  
**Audit Reference:** Phase 2 — API Contract Inventory  

---

## 1. Executive Summary

| Category | Total Operations | Implemented & Aligned | Scheduled (Phase 5) | Deprecated / Mismatch |
| :--- | :---: | :---: | :---: | :---: |
| **System & Health** | 1 | 1 | 0 | 0 |
| **Authentication & Session** | 10 | 10 | 0 | 0 |
| **Identity & Profile** | 5 | 5 | 0 | 0 |
| **Social, Feed & Interaction** | 14 | 14 | 0 | 0 |
| **Explore & Discovery** | 1 | 1 | 0 | 0 |
| **Notifications** | 4 | 4 | 0 | 0 |
| **Realtime Presence** | 1 | 1 | 0 | 0 |
| **Communities** | 9 | 9 | 0 | 0 |
| **Events** | 8 | 8 | 0 | 0 |
| **Direct & Group Messaging** | 21 | 21 | 0 | 0 |
| **Learning Module** | 10 | 0 | 10 | 0 |
| **Projects Module** | 12 | 0 | 12 | 0 |
| **Services Marketplace** | 8 | 0 | 8 | 0 |
| **Companies Module** | 11 | 0 | 11 | 0 |
| **Mentors Module** | 9 | 0 | 9 | 0 |
| **Career & Jobs Module** | 7 | 0 | 7 | 0 |
| **Storage Adapter** | 3 | 0 | 3 | 0 |
| **Payments & Payouts** | 7 | 0 | 7 | 0 |
| **TOTAL** | **141** | **74** | **67** | **0** |

---

## 2. Core Implemented Domains (100% Contract Parity)

### A. Health & System
- `GET /v1/system/health` -> `SystemHealthController.getHealth()`

### B. Authentication & Session
- `POST /v1/auth/register` -> `AuthenticationController.register()`
- `POST /v1/auth/login` -> `AuthenticationController.login()`
- `POST /v1/auth/verify-email` -> `AuthenticationController.verifyOtp()`
- `POST /v1/auth/resend-otp` -> `AuthenticationController.resendOtp()`
- `POST /v1/auth/oauth2/exchange` -> `AuthenticationController.exchangeOAuth2Code()`
- `POST /v1/auth/refresh` -> `AuthenticationController.refreshToken()`
- `POST /v1/auth/logout` -> `AuthenticationController.logout()`
- `POST /v1/auth/forgot-password` -> `AuthenticationController.forgotPassword()`
- `POST /v1/auth/reset-password` -> `AuthenticationController.resetPassword()`
- `DELETE /v1/identity/me` -> `AccountLifecycleController.deactivateAccount()`

### C. Identity & User Profiles
- `GET /v1/identity/me` -> `ProfileController.getMyProfile()`
- `GET /v1/identity/profiles/{id}` -> `ProfileController.getProfileById()`
- `GET /v1/identity/profiles` -> `ProfileController.searchProfiles()`
- `PATCH /v1/identity/me` -> `ProfileController.updateProfile()`
- `DELETE /v1/identity/me` -> `AccountLifecycleController.deactivateAccount()`

### D. Social, Feed & Community Interactions
- `GET /v1/social/feed` -> `FeedController.getFeed()` / `SocialBridgeController`
- `POST /v1/social/posts` -> `PostController.createPost()` / `SocialBridgeController`
- `POST /v1/social/posts/{id}/comments` -> `CommentController.createComment()`
- `PUT /v1/social/posts/{id}/reaction` -> `InteractionController.togglePostLike()`
- `DELETE /v1/social/posts/{id}/reaction` -> `InteractionController.removePostLike()`
- `POST /v1/social/posts/{postId}/polls/{pollId}/vote` -> `PollController.vote()`
- `POST /v1/social/profiles/{profileId}/follow` -> `NetworkController.follow()`
- `DELETE /v1/social/profiles/{profileId}/follow` -> `NetworkController.unfollow()`
- `GET /v1/social/profiles/{profileId}/follow/status` -> `NetworkController.getFollowStatus()`
- `POST /v1/social/reports` -> `ReportController.submitReport()`
- `POST /v1/social/posts/{postId}/save` -> `SavedPostController.savePost()`
- `DELETE /v1/social/posts/{postId}/save` -> `SavedPostController.unsavePost()`
- `GET /v1/social/saved` -> `SavedPostController.getSavedPosts()`
- `GET /v1/social/saved/ids` -> `SavedPostController.getSavedPostIds()`
- `GET /v1/explore` -> `ExploreController.search()`

### E. Notification Center
- `GET /v1/notifications` -> `NotificationController.getNotifications()`
- `PATCH /v1/notifications/{id}/read` -> `NotificationController.markRead()`
- `GET /v1/notifications/preferences` -> `NotificationPreferenceController.getPreferences()`
- `PATCH /v1/notifications/preferences` -> `NotificationPreferenceController.updatePreference()`

### F. Community Spaces & Topics
- `GET /v1/communities` -> `CommunityController.list()`
- `POST /v1/communities` -> `CommunityController.create()`
- `POST /v1/communities/{id}/join` -> `CommunityMembershipController.join()`
- `DELETE /v1/communities/{id}/join` -> `CommunityMembershipController.leave()`
- `GET /v1/communities/{id}/members` -> `CommunityMembershipController.members()`
- `GET /v1/communities/{id}/posts` -> `CommunityPostController.list()`
- `POST /v1/communities/{id}/posts` -> `CommunityPostController.create()`
- `GET /v1/communities/{communityId}/posts/{postId}/comments` -> `CommentController.getPostComments()`
- `POST /v1/communities/{communityId}/posts/{postId}/comments` -> `CommentController.createComment()`

### G. Events
- `GET /v1/events` -> `CommunityEventController.listGlobalEvents()`
- `GET /v1/events/tickets` -> `CommunityEventController.listTickets()`
- `POST /v1/events` -> `CommunityEventController.createGlobalEvent()`
- `POST /v1/events/{id}/registrations` -> `CommunityEventController.register()`
- `POST /v1/events/{id}/registrations/cancel` -> `CommunityEventController.cancelRegistration()`
- `POST /v1/events/{id}/tickets/{ticketId}/check-in` -> `CommunityEventController.checkIn()`
- `POST /v1/events/{id}/reminders/broadcast` -> `CommunityEventController.broadcastReminder()`
- `GET /v1/events/{id}/attendees` -> `CommunityEventController.attendees()`

### H. Messaging & Calls
- `GET /v1/messages/conversations` -> `MessagingController.getConversations()`
- `POST /v1/messages/conversations/direct` -> `MessagingController.getOrCreateDirectConversation()`
- `POST /v1/messages/conversations/group` -> `MessagingController.createGroupConversation()`
- `GET /v1/messages/conversations/{id}/members` -> `MessagingController.getConversationMembers()`
- `POST /v1/messages/conversations/{id}/members` -> `MessagingController.addMember()`
- `DELETE /v1/messages/conversations/{id}/members/{profileId}` -> `MessagingController.removeMember()`
- `GET /v1/messages/conversations/{id}/history` -> `MessagingController.getMessages()`
- `POST /v1/messages/conversations/{id}/messages` -> `MessagingController.sendMessage()`
- `PATCH /v1/messages/conversations/{convId}/messages/{msgId}` -> `MessagingController.editMessage()`
- `DELETE /v1/messages/conversations/{convId}/messages/{msgId}` -> `MessagingController.deleteMessage()`
- `PUT /v1/messages/conversations/{convId}/messages/{msgId}/reactions` -> `MessagingController.addOrUpdateReaction()`
- `DELETE /v1/messages/conversations/{convId}/messages/{msgId}/reactions` -> `MessagingController.removeReaction()`
- `POST /v1/messages/conversations/{convId}/messages/{msgId}/star` -> `MessagingController.starMessage()`
- `DELETE /v1/messages/conversations/{convId}/messages/{msgId}/star` -> `MessagingController.unstarMessage()`
- `GET /v1/messages/starred` -> `MessagingController.getStarredMessages()`
- `POST /v1/messages/conversations/{convId}/calls` -> `MessagingController.initiateCall()`
- `POST /v1/messages/conversations/{convId}/calls/signal` -> `MessagingController.signalCall()`
- `POST /v1/messages/calls/{callId}/end` -> `MessagingController.endCall()`
- `GET /v1/messages/calls/history` -> `MessagingController.getCallHistory()`
- `POST /v1/messages/conversations/{conversationId}/read` -> `MessagingController.markConversationRead()`
- `GET /v1/messages/conversations/{conversationId}/attachments/{objectId}/download` -> `MessagingController.downloadAttachment()`

---

## 3. Scheduled Domain Modules (Phase 5 Roadmap)

The remaining 67 operations belong to advanced business domains that are cleanly decoupled from the launch MVP and scheduled for dedicated implementation in Phase 5:

1. **Learning & Certifications (`/v1/learning/*`):** Course catalog, lesson completion tracking, and verifiable certificates.
2. **Projects & Tasks (`/v1/projects/*`):** Collaborative project workspaces, file manager, milestones, and kanban tasks.
3. **Services Marketplace (`/v1/services/*`):** Service provider listings, booking schedules, confirmation flow, and reviews.
4. **Companies & Workspaces (`/v1/companies/*`):** Company organizational hierarchy, team tasks, document vault, and role permissions.
5. **Mentors (`/v1/mentors/*`):** Mentor profiles, availability slots, booking requests, and session confirmations.
6. **Careers (`/v1/career/*`):** Job board, candidate applications, resume submission, and review pipelines.
7. **Storage Adapter (`/v1/storage/*`):** S3/GCS presigned direct upload ticket negotiation and verification.
8. **Payments & Payouts (`/v1/payments/*`):** Server-authoritative payment provider integration, webhook signature validation, and payout reconciliation.
