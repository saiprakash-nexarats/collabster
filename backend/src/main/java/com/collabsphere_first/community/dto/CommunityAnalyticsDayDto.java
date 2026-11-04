package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class CommunityAnalyticsDayDto {
    LocalDate day;
    long newMembers;
    long posts;
    long comments;
    long resources;
    long messages;
    long polls;
    long events;
    long eventRsvps;
}
