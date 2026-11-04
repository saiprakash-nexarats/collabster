package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CommunityAnalyticsDto;
import com.collabsphere_first.community.dto.CommunityAnalyticsDayDto;
import com.collabsphere_first.community.dto.CommunityMemberLeaderboardDto;
import com.collabsphere_first.community.dto.CommunityChannelLeaderboardDto;
import com.collabsphere_first.community.dto.CommunityDashboardDto;
import com.collabsphere_first.community.enums.*;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.exception.CommunityNotFoundException;
import com.collabsphere_first.community.repository.*;
import com.collabsphere_first.community.security.CommunityRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class CommunityDashboardService {
    private final MembershipRepository membershipRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ResourceRepository resourceRepository;
    private final ReportRepository reportRepository;
    private final CommunityChannelRepository channelRepository;
    private final CommunityChannelMessageRepository messageRepository;
    private final CommunityPollRepository pollRepository;
    private final CommunityEventRepository eventRepository;
    private final CommunityEventRsvpRepository eventRsvpRepository;
    private final CommunityRepository communityRepository;
    private final CommunityRoleService roleService;

    @Transactional(readOnly = true)
    public CommunityDashboardDto dashboard(UUID communityId, Long userId) {
        var community = communityRepository.findById(communityId).orElseThrow(() -> new CommunityNotFoundException(communityId));
        var membership = membershipRepository.findByCommunityIdAndUserId(communityId, userId);
        boolean admin = roleService.isPlatformAdmin(userId) || membership.filter(m -> m.getStatus() == MembershipStatus.ACTIVE
                && m.getRole().hasAtLeastRank(MembershipRole.ADMIN)).isPresent();
        if (!admin && membership.filter(m -> m.getStatus() == MembershipStatus.ACTIVE
                && m.getRole().hasAtLeastRank(MembershipRole.MODERATOR)).isEmpty()) {
            throw new InsufficientRoleException("Moderator access is required to view this dashboard");
        }
        long pendingReports = reportRepository.countByCommunityIdAndStatus(communityId, ReportStatus.PENDING);
        long pendingModeration = postRepository.countByCommunityIdAndModerationStatus(communityId, ModerationStatus.PENDING_REVIEW)
                + commentRepository.countByCommunityIdAndModerationStatus(communityId, ModerationStatus.PENDING_REVIEW)
                + resourceRepository.countByCommunity_IdAndModerationStatus(communityId, ModerationStatus.PENDING_REVIEW);
        if (!admin) return CommunityDashboardDto.builder().communityId(communityId).moderationView(true)
                .pendingReports(pendingReports).pendingModeration(pendingModeration).build();
        Instant now = Instant.now();
        Instant since = now.minus(30, ChronoUnit.DAYS);
        return CommunityDashboardDto.builder().communityId(communityId).moderationView(false)
                .memberCount((long) community.getMemberCount())
                .activeMembers(membershipRepository.countByCommunityIdAndStatus(communityId, MembershipStatus.ACTIVE))
                .pendingMembers(membershipRepository.countByCommunityIdAndStatus(communityId, MembershipStatus.PENDING_APPROVAL))
                .newMembers(membershipRepository.countByCommunityIdAndJoinedAtBetween(communityId, since, now))
                .postCount(postRepository.countByCommunityId(communityId)).commentCount(commentRepository.countByCommunityId(communityId))
                .resourceCount(resourceRepository.countByCommunity_Id(communityId)).channelCount(channelRepository.countByCommunity_Id(communityId))
                .messageCount(messageRepository.countByCommunityId(communityId)).pollCount(pollRepository.countByCommunityId(communityId))
                .reportCount(reportRepository.countByCommunityId(communityId)).pendingReports(pendingReports)
                .pendingModeration(pendingModeration).activityScore(community.getActivityScore()).build();
    }

    @Transactional(readOnly = true)
    public CommunityAnalyticsDto analytics(UUID communityId, Long userId, Instant from, Instant to) {
        boolean admin = roleService.isPlatformAdmin(userId) || membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE && m.getRole().hasAtLeastRank(MembershipRole.ADMIN)).isPresent();
        if (!admin) throw new InsufficientRoleException("Only community administrators can view analytics");
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(30, ChronoUnit.DAYS) : from;
        if (!end.isAfter(start)) throw new IllegalArgumentException("Analytics end time must be after start time");
        if (ChronoUnit.DAYS.between(start, end) > 366) throw new IllegalArgumentException("Analytics range must not exceed 366 days");
        communityRepository.findById(communityId).orElseThrow(() -> new CommunityNotFoundException(communityId));
        Map<LocalDate, DayCounts> daily = new LinkedHashMap<>();
        LocalDate firstDay = start.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate lastDay = end.minusMillis(1).atZone(ZoneOffset.UTC).toLocalDate();
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) daily.put(day, new DayCounts());

        membershipRepository.findByCommunityIdAndJoinedAtBetween(communityId, start, end).forEach(m -> record(daily, m.getJoinedAt(), DayCounts::member));
        postRepository.findByCommunityIdAndCreatedAtBetween(communityId, start, end).forEach(p -> record(daily, p.getCreatedAt(), DayCounts::post));
        commentRepository.findByCommunityIdAndCreatedAtBetween(communityId, start, end).forEach(c -> record(daily, c.getCreatedAt(), DayCounts::comment));
        resourceRepository.findByCommunity_IdAndCreatedAtBetween(communityId, start, end).forEach(r -> record(daily, r.getCreatedAt(), DayCounts::resource));
        var messages = messageRepository.findByCommunityIdAndCreatedAtBetween(communityId, start, end);
        messages.forEach(m -> record(daily, m.getCreatedAt(), DayCounts::message));
        pollRepository.findByCommunity_IdAndCreatedAtBetween(communityId, start, end).forEach(p -> record(daily, p.getCreatedAt(), DayCounts::poll));
        eventRepository.findByCommunity_IdAndCreatedAtBetween(communityId, start, end).forEach(e -> record(daily, e.getCreatedAt(), DayCounts::event));
        eventRsvpRepository.findByEvent_Community_IdAndUpdatedAtBetween(communityId, start, end).forEach(r -> record(daily, r.getUpdatedAt(), DayCounts::rsvp));

        List<CommunityAnalyticsDayDto> dailyDtos = daily.entrySet().stream().map(entry -> {
            DayCounts count = entry.getValue();
            return CommunityAnalyticsDayDto.builder().day(entry.getKey()).newMembers(count.newMembers).posts(count.posts)
                    .comments(count.comments).resources(count.resources).messages(count.messages).polls(count.polls)
                    .events(count.events).eventRsvps(count.eventRsvps).build();
        }).toList();
        List<CommunityMemberLeaderboardDto> topMembers = membershipRepository
                .findTop10ByCommunityIdOrderByContributionScoreDescUserIdAsc(communityId).stream()
                .map(member -> CommunityMemberLeaderboardDto.builder().userId(member.getUserId())
                        .contributionScore(member.getContributionScore()).build()).toList();
        Map<UUID, Long> channelMessageCounts = messageRepository.countMessagesByChannel(communityId, start, end).stream()
                .collect(java.util.stream.Collectors.toMap(
                        CommunityChannelMessageRepository.ChannelMessageCount::getChannelId,
                        CommunityChannelMessageRepository.ChannelMessageCount::getMessageCount));
        List<CommunityChannelLeaderboardDto> topChannels = channelRepository.findByCommunityIdOrderByNameAsc(communityId).stream()
                .map(channel -> CommunityChannelLeaderboardDto.builder().channelId(channel.getId()).channelName(channel.getName())
                        .messageCount(channelMessageCounts.getOrDefault(channel.getId(), 0L)).build())
                .sorted(Comparator.comparingLong(CommunityChannelLeaderboardDto::getMessageCount).reversed()
                        .thenComparing(CommunityChannelLeaderboardDto::getChannelName)).limit(10).toList();
        return CommunityAnalyticsDto.builder().communityId(communityId).from(start).to(end)
                .newMembers(membershipRepository.countByCommunityIdAndJoinedAtBetween(communityId, start, end))
                .posts(postRepository.countByCommunityIdAndCreatedAtBetween(communityId, start, end))
                .comments(commentRepository.countByCommunityIdAndCreatedAtBetween(communityId, start, end))
                .resources(resourceRepository.countByCommunity_IdAndCreatedAtBetween(communityId, start, end))
                .messages(messageRepository.countByCommunityIdAndCreatedAtBetween(communityId, start, end))
                .polls(pollRepository.countByCommunity_IdAndCreatedAtBetween(communityId, start, end))
                .events(eventRepository.countByCommunity_IdAndCreatedAtBetween(communityId, start, end))
                .eventRsvps(eventRsvpRepository.countByEvent_Community_IdAndUpdatedAtBetween(communityId, start, end))
                .daily(dailyDtos).topMembers(topMembers).topChannels(topChannels)
                .build();
    }

    private void record(Map<LocalDate, DayCounts> daily, Instant instant, java.util.function.Consumer<DayCounts> increment) {
        DayCounts count = daily.get(instant.atZone(ZoneOffset.UTC).toLocalDate());
        if (count != null) increment.accept(count);
    }

    private static final class DayCounts {
        private long newMembers, posts, comments, resources, messages, polls, events, eventRsvps;
        void member() { newMembers++; }
        void post() { posts++; }
        void comment() { comments++; }
        void resource() { resources++; }
        void message() { messages++; }
        void poll() { polls++; }
        void event() { events++; }
        void rsvp() { eventRsvps++; }
    }
}
