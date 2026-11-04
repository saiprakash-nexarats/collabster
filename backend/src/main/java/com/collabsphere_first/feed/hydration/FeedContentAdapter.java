package com.collabsphere_first.feed.hydration;

import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.user.entity.User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface FeedContentAdapter {

    ContentType supportedType();

    Map<UUID, HydratedFeedItem> batchHydrate(Collection<UUID> ids, User viewer);
}
