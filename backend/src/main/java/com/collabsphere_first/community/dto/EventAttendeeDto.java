package com.collabsphere_first.community.dto;
import com.collabsphere_first.community.enums.EventRsvpStatus;
import lombok.Builder;
import lombok.Value;
@Value @Builder public class EventAttendeeDto { Long userId; EventRsvpStatus status; }
