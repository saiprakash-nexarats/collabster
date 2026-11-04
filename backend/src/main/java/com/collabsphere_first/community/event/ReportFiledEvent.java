package com.collabsphere_first.community.event;

import com.collabsphere_first.community.enums.ReportTargetType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class ReportFiledEvent extends ApplicationEvent {
    private final UUID reportId;
    private final UUID communityId;
    private final ReportTargetType targetType;
    private final String targetId;
    private final Long reporterId;

    public ReportFiledEvent(Object source, UUID reportId, UUID communityId, ReportTargetType targetType, String targetId, Long reporterId) {
        super(source);
        this.reportId = reportId;
        this.communityId = communityId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reporterId = reporterId;
    }
}
