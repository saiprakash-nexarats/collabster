package com.collabsphere_first.interaction.resolver;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.interaction.exception.ContentInaccessibleException;
import com.collabsphere_first.interaction.exception.InvalidInteractionException;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentResolutionService {

    private final List<ContentResolver> resolvers;
    private final NetworkAccessService networkAccessService;

    public ResolvedContent resolveVisible(ContentRef contentRef, User viewer) {
        if (contentRef == null || contentRef.type() == null || contentRef.id() == null) {
            throw new InvalidInteractionException("ContentRef must contain non-null type and id");
        }

        ContentResolver resolver = resolvers.stream()
                .filter(r -> r.supports(contentRef.type()))
                .findFirst()
                .orElseThrow(() -> new InvalidInteractionException("Unsupported content type: " + contentRef.type()));

        ResolvedContent resolved = resolver.resolveVisible(contentRef, viewer);
        if (!resolved.accessible()) {
            throw new ContentInaccessibleException("Referenced content does not exist or is not accessible: " + contentRef);
        }

        // Centralized security & block enforcement
        if (resolved.authorId() != null && viewer != null && viewer.getId() != null) {
            Long viewerId = viewer.getId();
            Long authorId = resolved.authorId();

            if (!viewerId.equals(authorId) && networkAccessService.isBlockedEitherDirection(viewerId, authorId)) {
                log.warn("Blocked interaction attempt between viewer {} and author {} on content {}", viewerId, authorId, contentRef);
                throw new ContentInaccessibleException("Interaction not permitted due to an active block relationship.");
            }
        }

        return resolved;
    }
}
