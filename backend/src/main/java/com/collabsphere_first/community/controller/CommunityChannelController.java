package com.collabsphere_first.community.controller;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.service.CommunityChannelService;
import com.collabsphere_first.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CommunityChannelController {
    private final CommunityChannelService channelService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/v1/communities/{communityId}/channels")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<List<CommunityChannelDto>>> list(@PathVariable UUID communityId) {
        return ResponseEntity.ok(ApiResponse.success(channelService.listChannels(communityId, currentUserService.getCurrentUser().getId())));
    }

    @PostMapping("/api/v1/communities/{communityId}/channels")
    @PreAuthorize("hasPermission(#communityId, 'MODERATOR')")
    public ResponseEntity<ApiResponse<CommunityChannelDto>> create(@PathVariable UUID communityId,
                                                                    @Valid @RequestBody CreateChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Channel created",
                channelService.create(communityId, currentUserService.getCurrentUser().getId(), request)));
    }

    @GetMapping("/api/v1/communities/{communityId}/channels/{channelId}")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<CommunityChannelDto>> get(@PathVariable UUID communityId, @PathVariable UUID channelId) {
        return ResponseEntity.ok(ApiResponse.success(channelService.get(communityId, channelId, currentUserService.getCurrentUser().getId())));
    }

    @PatchMapping("/api/v1/communities/{communityId}/channels/{channelId}")
    @PreAuthorize("hasPermission(#communityId, 'MODERATOR')")
    public ResponseEntity<ApiResponse<CommunityChannelDto>> update(@PathVariable UUID communityId, @PathVariable UUID channelId,
            @RequestBody UpdateChannelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(channelService.update(communityId, channelId,
                currentUserService.getCurrentUser().getId(), request)));
    }

    @DeleteMapping("/api/v1/communities/{communityId}/channels/{channelId}")
    @PreAuthorize("hasPermission(#communityId, 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID communityId, @PathVariable UUID channelId) {
        channelService.delete(communityId, channelId, currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Channel deleted", null));
    }

    @GetMapping("/api/v1/communities/{communityId}/channels/{channelId}/messages")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<Page<CommunityChannelMessageDto>>> listMessages(@PathVariable UUID communityId,
            @PathVariable UUID channelId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(channelService.listMessages(communityId, channelId,
                currentUserService.getCurrentUser().getId(), pageable)));
    }

    @PostMapping("/api/v1/communities/{communityId}/channels/{channelId}/messages")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<CommunityChannelMessageDto>> send(@PathVariable UUID communityId, @PathVariable UUID channelId,
            @Valid @RequestBody CreateChannelMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Message sent", channelService.send(communityId,
                channelId, currentUserService.getCurrentUser().getId(), request)));
    }

    @PatchMapping("/api/v1/communities/{communityId}/channels/{channelId}/messages/{messageId}")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<CommunityChannelMessageDto>> edit(@PathVariable UUID communityId, @PathVariable UUID channelId, @PathVariable UUID messageId,
            @Valid @RequestBody UpdateChannelMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(channelService.edit(communityId, channelId, messageId,
                currentUserService.getCurrentUser().getId(), request.getBody())));
    }

    @DeleteMapping("/api/v1/communities/{communityId}/channels/{channelId}/messages/{messageId}")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable UUID communityId, @PathVariable UUID channelId, @PathVariable UUID messageId) {
        channelService.remove(communityId, channelId, messageId, currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Message removed", null));
    }

    @PostMapping("/api/v1/communities/{communityId}/channels/{channelId}/messages/{messageId}/reactions")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<List<CommunityChannelMessageReactionDto>>> addReaction(@PathVariable UUID communityId,
            @PathVariable UUID channelId, @PathVariable UUID messageId, @RequestParam String reaction) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(channelService.addReaction(communityId, channelId,
                messageId, currentUserService.getCurrentUser().getId(), reaction)));
    }

    @DeleteMapping("/api/v1/communities/{communityId}/channels/{channelId}/messages/{messageId}/reactions/{reaction}")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<List<CommunityChannelMessageReactionDto>>> removeReaction(@PathVariable UUID communityId,
            @PathVariable UUID channelId, @PathVariable UUID messageId, @PathVariable String reaction) {
        return ResponseEntity.ok(ApiResponse.success(channelService.removeReaction(communityId, channelId,
                messageId, currentUserService.getCurrentUser().getId(), reaction)));
    }

    @GetMapping("/api/v1/communities/{communityId}/channels/unread-counts")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<List<ChannelUnreadDto>>> unreadCounts(@PathVariable UUID communityId) {
        return ResponseEntity.ok(ApiResponse.success(channelService.unreadCounts(communityId, currentUserService.getCurrentUser().getId())));
    }

    @PutMapping("/api/v1/communities/{communityId}/channels/{channelId}/read")
    @PreAuthorize("hasPermission(#communityId, 'MEMBER')")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable UUID communityId, @PathVariable UUID channelId,
            @Valid @RequestBody MarkChannelReadRequest request) {
        channelService.markRead(communityId, channelId, request.getLastReadMessageId(), currentUserService.getCurrentUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Channel marked as read", null));
    }
}
