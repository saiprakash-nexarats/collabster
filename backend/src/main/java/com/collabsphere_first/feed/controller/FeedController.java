package com.collabsphere_first.feed.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.feed.dto.FeedPageResponse;
import com.collabsphere_first.feed.service.FeedService;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<FeedPageResponse>> getFeed(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit
    ) {
        User currentUser = currentUserService.getCurrentUser();
        FeedPageResponse page = feedService.getFeed(currentUser, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}
