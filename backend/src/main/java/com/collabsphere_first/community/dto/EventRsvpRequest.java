package com.collabsphere_first.community.dto;
import com.collabsphere_first.community.enums.EventRsvpStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class EventRsvpRequest { @NotNull private EventRsvpStatus status; }
