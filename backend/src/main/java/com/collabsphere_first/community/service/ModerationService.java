package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CreateReportRequest;
import com.collabsphere_first.community.dto.ReportDto;
import com.collabsphere_first.community.dto.ResolveReportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ModerationService {
    void triggerAsyncPostModeration(UUID postId, String contentText);
    void triggerAsyncCommentModeration(UUID commentId, String contentText);
    void triggerAsyncResourceModeration(UUID resourceId, String contentText);

    ReportDto fileReport(CreateReportRequest request, Long reporterId);
    Page<ReportDto> getModerationQueue(UUID communityId, Long requestingUserId, Pageable pageable);
    ReportDto resolveReport(UUID reportId, ResolveReportRequest request, Long requestingUserId);

    Page<com.collabsphere_first.community.dto.PostDto> getPendingPostsQueue(UUID communityId, Long requestingUserId, Pageable pageable);
    com.collabsphere_first.community.dto.PostDto approveOrRejectPost(UUID postId, boolean approve, Long requestingUserId);
}
