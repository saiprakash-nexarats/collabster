package com.collabsphere_first.interaction.adapter;

import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.interaction.dto.LikeResponse;
import com.collabsphere_first.interaction.dto.SaveResponse;
import com.collabsphere_first.user.entity.User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Domain-specific interaction adapter interface.
 * Dispatches likes, saves, and interaction summaries to the authoritative domain owner
 * based on ContentType while preserving a uniform global interaction contract.
 */
public interface ContentInteractionAdapter {

    ContentType supports();

    LikeResponse like(User user, UUID contentId);

    LikeResponse unlike(User user, UUID contentId);

    SaveResponse save(User user, UUID contentId);

    SaveResponse unsave(User user, UUID contentId);

    Map<UUID, Long> getLikeCounts(Collection<UUID> contentIds);

    Map<UUID, Long> getSaveCounts(Collection<UUID> contentIds);

    Set<UUID> getLikedContentIds(Long userId, Collection<UUID> contentIds);

    Set<UUID> getSavedContentIds(Long userId, Collection<UUID> contentIds);
}
