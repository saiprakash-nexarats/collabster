package com.collabsphere_first.community.event;

import com.collabsphere_first.community.enums.ReportStatus;
import com.collabsphere_first.community.enums.ReportTargetType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class ReportLifecycleEvent extends ApplicationEvent {
    public enum Action {
        RESOLVED,
        DISMISSED
    }

    private final UUID reportId;
    private final UUID communityId;
    private final Long reporterId;
    private final Long resolverId;
    private final Action action;
    private final ReportStatus status;
    private final ReportTargetType targetType;
    private final String targetId;

    public ReportLifecycleEvent(
            Object source,
            UUID reportId,
            UUID communityId,
            Long reporterId,
            Long resolverId,
            Action action,
            ReportStatus status,
            ReportTargetType targetType,
            String targetId
    ) {
        super(source);
        this.reportId = reportId;
        this.communityId = communityId;
        this.reporterId = reporterId;
        this.resolverId = resolverId;
        this.action = action;
        this.status = status;
        this.targetType = targetType;
        this.targetId = targetId;
    }
}
