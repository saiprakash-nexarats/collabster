package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.*;
import com.collabsphere_first.community.entity.*;
import com.collabsphere_first.community.exception.InvalidJoinException;
import com.collabsphere_first.community.repository.*;
import com.collabsphere_first.community.security.CommunityAccessService;
import com.collabsphere_first.community.config.CommunitiesRateLimitConfig;
import com.collabsphere_first.community.exception.CommunityRateLimitException;
import com.collabsphere_first.community.enums.MembershipRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityPollService {
    private final CommunityPollRepository pollRepository;
    private final CommunityPollOptionRepository optionRepository;
    private final CommunityPollVoteRepository voteRepository;
    private final CommunityAccessService accessService;
    private final CommunitySettingsService settingsService;
    private final CommunityEventOutboxService outboxService;
    private final CommunitiesRateLimitConfig rateLimitConfig;
    private final CommunityNotificationService notificationService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommunityPollDto create(UUID communityId, Long userId, CreatePollRequest request) {
        var limit = rateLimitConfig.consumePoll(userId);
        if (!limit.allowed()) throw new CommunityRateLimitException("Poll creation rate limit exceeded", limit.retryAfterSeconds());
        var membership = accessService.requireWritableMember(communityId, userId);
        var settings = settingsService.getOrDefault(communityId);
        if (!settings.isPollsEnabled()) throw new InvalidJoinException("Community polls are disabled");
        if (!membership.getRole().hasAtLeastRank(settings.getPollCreationRole())) throw new InvalidJoinException("This role cannot create polls");
        if (request.getClosesAt() != null && !request.getClosesAt().isAfter(Instant.now())) {
            throw new InvalidJoinException("Poll close time must be in the future");
        }
        List<String> options = request.getOptions().stream().map(String::trim).toList();
        if (options.stream().anyMatch(String::isBlank)
                || options.stream().map(s -> s.toLowerCase(java.util.Locale.ROOT)).distinct().count() != options.size()) {
            throw new InvalidJoinException("Poll options must be non-empty and unique");
        }
        CommunityPoll poll = pollRepository.save(CommunityPoll.builder().community(accessService.requireCommunity(communityId))
                .createdBy(userId).question(request.getQuestion().trim()).allowMultiple(request.isAllowMultiple())
                .closesAt(request.getClosesAt()).createdAt(Instant.now()).build());
        for (int i = 0; i < options.size(); i++) {
            optionRepository.save(CommunityPollOption.builder().poll(poll).text(options.get(i)).position(i).build());
        }
        notificationService.notifyActiveMembers(communityId, userId, "POLL_CREATED", "New community poll",
                poll.getQuestion(), "POLL", poll.getId().toString());
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.PollLifecycleEvent(
                this, poll.getId(), communityId, userId, userId,
                com.collabsphere_first.community.event.PollLifecycleEvent.Action.CREATED, poll.getQuestion()));
        outboxService.record(communityId, "POLL_CREATED", poll.getId().toString(), java.util.Map.of("createdBy", userId));
        return toDto(poll);
    }

    @Transactional(readOnly = true)
    public Page<CommunityPollDto> list(UUID communityId, Long userId, Pageable pageable) {
        accessService.requireMember(communityId, userId);
        if (!settingsService.getOrDefault(communityId).isPollsEnabled()) throw new InvalidJoinException("Community polls are disabled");
        Pageable bounded = PageRequest.of(Math.max(0, pageable.getPageNumber()), Math.max(1, Math.min(pageable.getPageSize(), 100)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return pollRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, bounded).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public CommunityPollDto get(UUID pollId, Long userId) {
        CommunityPoll poll = findPoll(pollId);
        accessService.requireMember(poll.getCommunity().getId(), userId);
        if (!settingsService.getOrDefault(poll.getCommunity().getId()).isPollsEnabled()) throw new InvalidJoinException("Community polls are disabled");
        return toDto(poll);
    }

    @Transactional
    public CommunityPollDto vote(UUID pollId, Long userId, VotePollRequest request) {
        var limit = rateLimitConfig.consumePoll(userId);
        if (!limit.allowed()) throw new CommunityRateLimitException("Poll vote rate limit exceeded", limit.retryAfterSeconds());
        CommunityPoll poll = pollRepository.findByIdForUpdate(pollId).orElseThrow(() -> new InvalidJoinException("Poll not found"));
        accessService.requireWritableMember(poll.getCommunity().getId(), userId);
        if (!settingsService.getOrDefault(poll.getCommunity().getId()).isPollsEnabled()) throw new InvalidJoinException("Community polls are disabled");
        if (poll.getClosedAt() != null || (poll.getClosesAt() != null && !poll.getClosesAt().isAfter(Instant.now()))) {
            throw new InvalidJoinException("Poll is closed");
        }
        List<UUID> ids = request.getOptionIds().stream().distinct().toList();
        if (ids.size() != request.getOptionIds().size()) throw new InvalidJoinException("Duplicate poll options are not allowed");
        if (!poll.isAllowMultiple() && ids.size() != 1) throw new InvalidJoinException("Choose one option for this poll");
        if (!poll.isAllowMultiple() && voteRepository.existsByPollIdAndUserId(pollId, userId)) {
            throw new InvalidJoinException("You have already voted in this poll");
        }
        List<CommunityPollOption> options = optionRepository.findByIdInAndPollId(ids, pollId);
        if (options.size() != ids.size()) throw new InvalidJoinException("Every selected option must belong to this poll");
        for (CommunityPollOption option : options) {
            if (voteRepository.existsByPollIdAndUserIdAndOptionId(pollId, userId, option.getId())) {
                throw new InvalidJoinException("You have already selected one of these options");
            }
            voteRepository.save(CommunityPollVote.builder().poll(poll).option(option).userId(userId).createdAt(Instant.now()).build());
        }
        outboxService.record(poll.getCommunity().getId(), "POLL_VOTED", pollId.toString(), java.util.Map.of("userId", userId));
        return toDto(poll);
    }

    @Transactional
    public CommunityPollDto close(UUID pollId, Long userId) {
        CommunityPoll poll = pollRepository.findByIdForUpdate(pollId).orElseThrow(() -> new InvalidJoinException("Poll not found"));
        if (!poll.getCreatedBy().equals(userId)) accessService.requireRole(poll.getCommunity().getId(), userId, MembershipRole.MODERATOR);
        else accessService.requireWritableMember(poll.getCommunity().getId(), userId);
        if (poll.getClosedAt() == null) poll.setClosedAt(Instant.now());
        notificationService.notifyActiveMembers(poll.getCommunity().getId(), userId, "POLL_CLOSED", "Community poll closed",
                poll.getQuestion(), "POLL", poll.getId().toString());
        eventPublisher.publishEvent(new com.collabsphere_first.community.event.PollLifecycleEvent(
                this, poll.getId(), poll.getCommunity().getId(), poll.getCreatedBy(), userId,
                com.collabsphere_first.community.event.PollLifecycleEvent.Action.CLOSED, poll.getQuestion()));
        outboxService.record(poll.getCommunity().getId(), "POLL_CLOSED", pollId.toString(), java.util.Map.of("closedBy", userId));
        return toDto(pollRepository.save(poll));
    }

    private CommunityPoll findPoll(UUID id) {
        return pollRepository.findById(id).orElseThrow(() -> new InvalidJoinException("Poll not found"));
    }

    private CommunityPollDto toDto(CommunityPoll poll) {
        List<CommunityPollOptionDto> options = optionRepository.findByPollIdOrderByPositionAsc(poll.getId()).stream()
                .map(option -> CommunityPollOptionDto.builder().id(option.getId()).text(option.getText())
                        .votes(voteRepository.countByOptionId(option.getId())).build()).toList();
        boolean closed = poll.getClosedAt() != null || (poll.getClosesAt() != null && !poll.getClosesAt().isAfter(Instant.now()));
        return CommunityPollDto.builder().id(poll.getId()).communityId(poll.getCommunity().getId())
                .createdBy(poll.getCreatedBy()).question(poll.getQuestion()).allowMultiple(poll.isAllowMultiple())
                .closed(closed).closesAt(poll.getClosesAt()).createdAt(poll.getCreatedAt()).options(options).build();
    }
}
