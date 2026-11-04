package com.collabsphere_first.feed.service;

import com.collabsphere_first.feed.dto.FeedPageResponse;
import com.collabsphere_first.user.entity.User;

public interface FeedService {

    FeedPageResponse getFeed(User currentUser, String cursor, int limit);
}
