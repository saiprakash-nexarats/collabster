package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.entity.*;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.repository.CommunityChannelMessageRepository;
import com.collabsphere_first.community.repository.CommunityChannelMessageReactionRepository;
import com.collabsphere_first.community.repository.CommunityChannelReadStateRepository;
import com.collabsphere_first.community.repository.CommunityChannelRepository;
import com.collabsphere_first.community.security.CommunityAccessService;
import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import com.collabsphere_first.community.exception.CommunityRateLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityChannelService {
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9][a-z0-9_-]{0,59}");
    private final CommunityChannelRepository channelRepository;
    private final CommunityChannelMessageRepository messageRepository;
    private final CommunityChannelMessageReactionRepository reactionRepository;
    private final CommunityChannelReadStateRepository readStateRepository;
    private final CommunityAccessService accessService;
    private final CommunityAuditLogService auditLogService;
    private final CommunitySettingsService settingsService;
    private final CommunityEventOutboxService outboxService;
    private final CommunitiesRateLimitConfig rateLimitConfig;
    private final CommunityRealtimePublisher realtimePublisher;
    private final CommunityNotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CommunityChannelDto> listChannels(UUID communityId, Long userId) {
        accessService.requireMember(communityId, userId);
        if (!settingsService.getOrDefault(communityId).isChannelsEnabled()) return List.of();
        return channelRepository.findByCommunityIdOrderByNameAsc(communityId).stream().map(this::toDto).toList();
    }

    @Transactional
    public CommunityChannelDto create(UUID communityId, Long userId, CreateChannelRequest request) {
        if (!settingsService.getOrDefault(communityId).isChannelsEnabled()) throw new InvalidJoinException("Community channels are disabled");
        MembershipRole allowedRole = settingsService.getOrDefault(communityId).getChannelCreationRole();
        accessService.requireRole(communityId, userId, allowedRole);
        String name = normalizeName(request.getName());
        if (channelRepository.existsByCommunityIdAndNameIgnoreCase(communityId, name)) throw new InvalidJoinException("Channel name is already in use");
        CommunityChannel channel = CommunityChannel.builder().community(accessService.requireCommunity(communityId))
                .name(name).description(request.getDescription()).readOnly(request.isReadOnly()).createdBy(userId).build();
        CommunityChannel saved = channelRepository.save(channel);
        auditLogService.record(saved.getCommunity(), userId, "CHANNEL_CREATED", "CHANNEL", saved.getId().toString(), "name=" + name);
        outboxService.record(communityId, "CHANNEL_CREATED", saved.getId().toString(), java.util.Map.of("name", name, "createdBy", userId));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public CommunityChannelDto get(UUID communityId, UUID channelId, Long userId) {
        accessService.requireMember(communityId, userId);
        if (!settingsService.getOrDefault(communityId).isChannelsEnabled()) throw new InvalidJoinException("Community channels are disabled");
        return toDto(findChannel(communityId, channelId));
    }

    @Transactional
    public CommunityChannelDto update(UUID communityId, UUID channelId, Long userId, UpdateChannelRequest request) {
        accessService.requireRole(communityId, userId, MembershipRole.MODERATOR);
        CommunityChannel channel = findChannel(communityId, channelId);
        if (request.getName() != null) {
            String name = normalizeName(request.getName());
            if (!name.equals(channel.getName()) && channelRepository.existsByCommunityIdAndNameIgnoreCase(communityId, name)) {
                throw new InvalidJoinException("Channel name is already in use");
            }
            channel.setName(name);
        }
        if (request.getDescription() != null) channel.setDescription(request.getDescription());
        if (request.getReadOnly() != null) channel.setReadOnly(request.getReadOnly());
        CommunityChannel saved = channelRepository.save(channel);
        auditLogService.record(saved.getCommunity(), userId, "CHANNEL_UPDATED", "CHANNEL", saved.getId().toString(), null);
        outboxService.record(communityId, "CHANNEL_UPDATED", saved.getId().toString(), java.util.Map.of("updatedBy", userId));
        return toDto(saved);
    }

    @Transactional
    public void delete(UUID communityId, UUID channelId, Long userId) {
        accessService.requireRole(communityId, userId, MembershipRole.ADMIN);
        CommunityChannel channel = findChannel(communityId, channelId);
        if (channel.getName().equals("general")) throw new InvalidJoinException("The general channel cannot be deleted");
        auditLogService.record(channel.getCommunity(), userId, "CHANNEL_DELETED", "CHANNEL", channelId.toString(), "name=" + channel.getName());
        outboxService.record(communityId, "CHANNEL_DELETED", channelId.toString(), java.util.Map.of("deletedBy", userId));
        channelRepository.delete(channel);
    }

    @Transactional(readOnly = true)
    public Page<CommunityChannelMessageDto> listMessages(UUID communityId, UUID channelId, Long userId, Pageable pageable) {
        accessService.requireMember(communityId, userId);
        if (!settingsService.getOrDefault(communityId).isChannelsEnabled()) throw new InvalidJoinException("Community channels are disabled");
        findChannel(communityId, channelId);
        Pageable bounded = PageRequest.of(Math.max(0, pageable.getPageNumber()),
                Math.max(1, Math.min(pageable.getPageSize(), 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommunityChannelMessage> messages = messageRepository.findByChannelIdAndCommunityId(channelId, communityId, bounded);
        Map<UUID, List<CommunityChannelMessageReaction>> reactions = reactionsFor(messages.getContent());
        return messages.map(message -> toDto(message, userId, reactions.getOrDefault(message.getId(), List.of())));
    }

//    @Transactional
//    public CommunityChannelMessageDto send(UUID communityId, UUID channelId, Long userId, CreateChannelMessageRequest request) {
//        var limit = rateLimitConfig.consumeMessage(userId);
//        if (!limit.allowed()) throw new CommunityRateLimitException("Message rate limit exceeded", limit.retryAfterSeconds());
//        var membership = accessService.requireWritableMember(communityId, userId);
//        var settings = settingsService.getOrDefault(communityId);
//        if (!settings.isMessagingEnabled()) throw new InvalidJoinException("Community messaging is disabled");
//        if (!membership.getRole().hasAtLeastRank(settings.getMessageSendRole())) throw new InvalidJoinException("This role cannot send channel messages");
//        CommunityChannel channel = findChannel(communityId, channelId);
//        if (channel.isReadOnly() && !membership.getRole().hasAtLeastRank(MembershipRole.MODERATOR)) {
//            throw new InvalidJoinException("Only moderators can post in this channel");
//        }
//        CommunityChannelMessage parent = null;
//        if (request.getParentMessageId() != null) {
//            parent = messageRepository.findById(request.getParentMessageId())
//                    .filter(m -> m.getCommunityId().equals(communityId) && m.getChannel().getId().equals(channelId))
//                    .orElseThrow(() -> new InvalidJoinException("Parent message must belong to the same channel"));
//        }
//        Instant now = Instant.now();
//        CommunityChannelMessage message = CommunityChannelMessage.builder().channel(channel).communityId(communityId)
//                .senderId(userId).body(request.getBody().trim()).parentMessage(parent).createdAt(now).updatedAt(now).build();
//        CommunityChannelMessage saved = messageRepository.save(message);
//        outboxService.record(communityId, "CHANNEL_MESSAGE_CREATED", saved.getId().toString(), java.util.Map.of("senderId", userId, "channelId", channelId));
//        CommunityChannelMessageDto dto = toDto(saved, userId, List.of());
//        realtimePublisher.messageChanged(communityId, channelId, "CREATED", dto);
//        return dto;
//    }

    @Transactional
    public CommunityChannelMessageDto send(
            UUID communityId,
            UUID channelId,
            Long userId,
            CreateChannelMessageRequest request
    ) {
        var membership = accessService.requireWritableMember(communityId, userId);

        var settings = settingsService.getOrDefault(communityId);
        if (!settings.isMessagingEnabled()) {
            throw new InvalidJoinException("Community messaging is disabled");
        }

        if (!membership.getRole().hasAtLeastRank(settings.getMessageSendRole())) {
            throw new InvalidJoinException("This role cannot send channel messages");
        }

        CommunityChannel channel = findChannel(communityId, channelId);

        if (channel.isReadOnly()
                && !membership.getRole().hasAtLeastRank(MembershipRole.MODERATOR)) {
            throw new InvalidJoinException(
                    "Only moderators can post in this channel"
            );
        }

        String body = request.getBody() == null ? "" : request.getBody().trim();

        List<String> attachmentUrls = normalizeAttachmentUrls(
                request.getAttachmentUrls()
        );

        if (body.isBlank() && attachmentUrls.isEmpty()) {
            throw new InvalidJoinException(
                    "A message must contain text or at least one attachment"
            );
        }

        if (body.length() > 10_000) {
            throw new InvalidJoinException(
                    "Message body must contain at most 10000 characters"
            );
        }

        if (attachmentUrls.size() > 10) {
            throw new InvalidJoinException(
                    "A message can include at most 10 attachments"
            );
        }

        CommunityChannelMessage parent = resolveParentMessage(
                request.getParentMessageId(),
                communityId,
                channelId
        );

        var limit = rateLimitConfig.consumeMessage(userId);
        if (!limit.allowed()) {
            throw new CommunityRateLimitException(
                    "Message rate limit exceeded",
                    limit.retryAfterSeconds()
            );
        }

        Instant now = Instant.now();

        CommunityChannelMessage message = CommunityChannelMessage.builder()
                .channel(channel)
                .communityId(communityId)
                .senderId(userId)
                .body(body)
                .parentMessage(parent)
                .createdAt(now)
                .updatedAt(now)
                .build();

        attachmentUrls.forEach(message::addAttachment);

        CommunityChannelMessage saved = messageRepository.save(message);

        if (parent != null && !parent.getSenderId().equals(userId)) {
            notificationService.createDirect(parent.getSenderId(), communityId, "REPLY_CREATED",
                    "New reply in channel " + channel.getName(),
                    body.isBlank() ? "Replied with an attachment" : body,
                    "CHANNEL_MESSAGE", saved.getId().toString());
        }

        outboxService.record(
                communityId,
                "CHANNEL_MESSAGE_CREATED",
                saved.getId().toString(),
                Map.of(
                        "senderId", userId,
                        "channelId", channelId,
                        "attachmentCount", attachmentUrls.size()
                )
        );

        CommunityChannelMessageDto dto = toDto(saved, userId, List.of());

        realtimePublisher.messageChanged(
                communityId,
                channelId,
                "CREATED",
                dto
        );

        return dto;
    }

    private CommunityChannelMessage resolveParentMessage(
            UUID parentMessageId,
            UUID communityId,
            UUID channelId
    ) {
        if (parentMessageId == null) {
            return null;
        }

        return messageRepository.findById(parentMessageId)
                .filter(message ->
                        message.getCommunityId().equals(communityId)
                                && message.getChannel().getId().equals(channelId)
                                && !message.isRemoved()
                )
                .orElseThrow(() -> new InvalidJoinException(
                        "Parent message must belong to the same channel"
                ));
    }

    private List<String> normalizeAttachmentUrls(List<String> rawAttachmentUrls) {
        if (rawAttachmentUrls == null || rawAttachmentUrls.isEmpty()) {
            return List.of();
        }

        return rawAttachmentUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .peek(this::validateAttachmentUrl)
                .distinct()
                .toList();
    }

    private void validateAttachmentUrl(String attachmentUrl) {
        if (attachmentUrl.length() > 2048) {
            throw new InvalidJoinException(
                    "Attachment URL must not exceed 2048 characters"
            );
        }

        try {
            URI uri = URI.create(attachmentUrl);

            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new InvalidJoinException(
                        "Attachment URLs must be valid HTTPS URLs"
                );
            }

            // Optional: enforce your own S3 bucket/CDN host.
            // Example:
            // if (!uri.getHost().equalsIgnoreCase(storageProperties.getPublicHost())) {
            //     throw new InvalidJoinException("Attachment URL is not from approved storage");
            // }
        } catch (IllegalArgumentException exception) {
            throw new InvalidJoinException("Attachment URL is invalid");
        }
    }

//    public void addAttachment(String attachmentUrl) {
//        CommunityChannelMessageAttachment attachment =
//                CommunityChannelMessageAttachment.builder()
//                        .message(this)
//                        .attachmentUrl(attachmentUrl)
//                        .build();
//
//        this.attachments.add(attachment);
//    }

    @Transactional
    public CommunityChannelMessageDto edit(UUID communityId, UUID channelId, UUID messageId, Long userId, String body) {
        CommunityChannelMessage message = findMessage(communityId, messageId);
        if (!message.getChannel().getId().equals(channelId)) throw new InvalidJoinException("Message not found in this channel");
        if (!message.getSenderId().equals(userId)) accessService.requireRole(communityId, userId, MembershipRole.MODERATOR);
        accessService.requireWritableMember(communityId, userId);
        if (body == null || body.isBlank() || body.length() > 10000) throw new InvalidJoinException("Message body must contain 1 to 10000 characters");
        message.setBody(body.trim());
        message.setUpdatedAt(Instant.now());
        CommunityChannelMessageDto dto = toDto(messageRepository.save(message), userId,
                reactionsFor(List.of(message)).getOrDefault(messageId, List.of()));
        realtimePublisher.messageChanged(communityId, channelId, "UPDATED", dto);
        return dto;
    }

    @Transactional
    public void remove(UUID communityId, UUID channelId, UUID messageId, Long userId) {
        CommunityChannelMessage message = findMessage(communityId, messageId);
        if (!message.getChannel().getId().equals(channelId)) throw new InvalidJoinException("Message not found in this channel");
        if (!message.getSenderId().equals(userId)) accessService.requireRole(communityId, userId, MembershipRole.MODERATOR);
        else accessService.requireWritableMember(communityId, userId);
        message.setRemoved(true);
        message.setBody("");
        message.setUpdatedAt(Instant.now());
        CommunityChannelMessage saved = messageRepository.save(message);
        realtimePublisher.messageChanged(communityId, channelId, "REMOVED", toDto(saved, userId,
                reactionsFor(List.of(saved)).getOrDefault(messageId, List.of())));
    }

    @Transactional
    public List<CommunityChannelMessageReactionDto> addReaction(UUID communityId, UUID channelId, UUID messageId, Long userId, String reaction) {
        accessService.requireWritableMember(communityId, userId);
        findChannel(communityId, channelId);
        CommunityChannelMessage message = findMessage(communityId, messageId);
        if (!message.getChannel().getId().equals(channelId) || message.isRemoved()) throw new InvalidJoinException("Message not found in this channel");
        String normalized = normalizeReaction(reaction);
        if (reactionRepository.findByMessageIdAndUserIdAndReaction(messageId, userId, normalized).isEmpty()) {
            reactionRepository.save(CommunityChannelMessageReaction.builder().message(message).communityId(communityId)
                    .userId(userId).reaction(normalized).createdAt(Instant.now()).build());
            outboxService.record(communityId, "CHANNEL_MESSAGE_REACTION_ADDED", messageId.toString(),
                    Map.of("channelId", channelId, "userId", userId, "reaction", normalized));
        }
        List<CommunityChannelMessageReaction> reactions = reactionsFor(List.of(message)).getOrDefault(messageId, List.of());
        realtimePublisher.messageChanged(communityId, channelId, "REACTIONS_UPDATED", toDto(message, userId, reactions));
        return toReactionDtos(reactions, userId);
    }

    @Transactional
    public List<CommunityChannelMessageReactionDto> removeReaction(UUID communityId, UUID channelId, UUID messageId, Long userId, String reaction) {
        accessService.requireWritableMember(communityId, userId);
        findChannel(communityId, channelId);
        CommunityChannelMessage message = findMessage(communityId, messageId);
        if (!message.getChannel().getId().equals(channelId)) throw new InvalidJoinException("Message not found in this channel");
        reactionRepository.findByMessageIdAndUserIdAndReaction(messageId, userId, normalizeReaction(reaction)).ifPresent(reactionRepository::delete);
        List<CommunityChannelMessageReaction> reactions = reactionsFor(List.of(message)).getOrDefault(messageId, List.of());
        realtimePublisher.messageChanged(communityId, channelId, "REACTIONS_UPDATED", toDto(message, userId, reactions));
        return toReactionDtos(reactions, userId);
    }

    @Transactional(readOnly = true)
    public List<ChannelUnreadDto> unreadCounts(UUID communityId, Long userId) {
        accessService.requireMember(communityId, userId);
        Map<UUID, Long> counts = messageRepository.countUnreadByChannel(communityId, userId).stream()
                .collect(Collectors.toMap(CommunityChannelMessageRepository.ChannelUnreadCount::getChannelId,
                        CommunityChannelMessageRepository.ChannelUnreadCount::getUnreadCount));
        return channelRepository.findByCommunityIdOrderByNameAsc(communityId).stream()
                .map(channel -> ChannelUnreadDto.builder().channelId(channel.getId()).unreadCount(counts.getOrDefault(channel.getId(), 0L)).build())
                .toList();
    }

    @Transactional
    public void markRead(UUID communityId, UUID channelId, UUID messageId, Long userId) {
        accessService.requireMember(communityId, userId);
        CommunityChannel channel = findChannel(communityId, channelId);
        CommunityChannelMessage message = findMessage(communityId, messageId);
        if (!message.getChannel().getId().equals(channelId)) throw new InvalidJoinException("Message not found in this channel");
        CommunityChannelReadState state = readStateRepository.findByChannelIdAndUserId(channelId, userId)
                .orElseGet(() -> CommunityChannelReadState.builder().channel(channel).communityId(communityId).userId(userId).build());
        if (state.getLastReadMessage() == null || !state.getLastReadAt().isAfter(message.getCreatedAt())) {
            state.setLastReadMessage(message);
            state.setLastReadAt(message.getCreatedAt());
            readStateRepository.save(state);
        }
    }

    private CommunityChannel findChannel(UUID communityId, UUID channelId) {
    return channelRepository.findChannelDirect(channelId, communityId)
            .orElseThrow(() ->
                    new InvalidJoinException("Channel not found in this community")
            );
}

    private CommunityChannelMessage findMessage(UUID communityId, UUID messageId) {
        CommunityChannelMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new InvalidJoinException("Message not found"));
        if (!message.getCommunityId().equals(communityId)) throw new InvalidJoinException("Message not found in this community");
        return message;
    }

    private String normalizeName(String raw) {
        String name = raw.trim().toLowerCase(Locale.ROOT).replaceFirst("^#", "");
        if (!VALID_NAME.matcher(name).matches()) throw new InvalidJoinException("Channel names may contain lowercase letters, numbers, _ and -");
        return name;
    }

    private String normalizeReaction(String raw) {
        if (raw == null || raw.isBlank()) throw new InvalidJoinException("Reaction is required");
        String reaction = raw.trim();
        if (reaction.codePointCount(0, reaction.length()) > 32) throw new InvalidJoinException("Reaction must contain at most 32 characters");
        return reaction;
    }

    private CommunityChannelDto toDto(CommunityChannel channel) {
        return CommunityChannelDto.builder().id(channel.getId()).communityId(channel.getCommunity().getId())
                .name(channel.getName()).description(channel.getDescription()).readOnly(channel.isReadOnly())
                .createdBy(channel.getCreatedBy()).createdAt(channel.getCreatedAt()).build();
    }

    private Map<UUID, List<CommunityChannelMessageReaction>> reactionsFor(List<CommunityChannelMessage> messages) {
        if (messages.isEmpty()) return Map.of();
        return reactionRepository.findByMessageIdIn(messages.stream().map(CommunityChannelMessage::getId).toList()).stream()
                .collect(Collectors.groupingBy(reaction -> reaction.getMessage().getId()));
    }

    private List<CommunityChannelMessageReactionDto> toReactionDtos(List<CommunityChannelMessageReaction> reactions, Long userId) {
        return reactions.stream().collect(Collectors.groupingBy(CommunityChannelMessageReaction::getReaction)).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> CommunityChannelMessageReactionDto.builder().reaction(entry.getKey()).count(entry.getValue().size())
                        .reactedByCurrentUser(entry.getValue().stream().anyMatch(r -> r.getUserId().equals(userId))).build())
                .toList();
    }

    private CommunityChannelMessageDto toDto(CommunityChannelMessage message, Long userId, List<CommunityChannelMessageReaction> reactions) {
        return CommunityChannelMessageDto.builder().id(message.getId()).communityId(message.getCommunityId())
                .channelId(message.getChannel().getId()).senderId(message.getSenderId())
                .body(message.isRemoved() ? "" : message.getBody())
                .parentMessageId(message.getParentMessage() == null ? null : message.getParentMessage().getId())
                .attachmentUrls(
                        message.isRemoved()
                                ? List.of()
                                : message.getAttachments().stream()
                                .map(CommunityChannelMessageAttachment::getAttachmentUrl)
                                .toList()
                )
                .reactions(toReactionDtos(reactions, userId))
                .removed(message.isRemoved()).createdAt(message.getCreatedAt()).updatedAt(message.getUpdatedAt()).build();
    }
}
