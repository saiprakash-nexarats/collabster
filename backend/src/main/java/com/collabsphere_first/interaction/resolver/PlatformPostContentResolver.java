package com.collabsphere_first.interaction.resolver;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.post.dto.response.PlatformPostResponse;
import com.collabsphere_first.post.exception.PlatformPostAccessDeniedException;
import com.collabsphere_first.post.exception.PlatformPostNotFoundException;
import com.collabsphere_first.post.service.PlatformPostService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformPostContentResolver implements ContentResolver {

    private final PlatformPostService platformPostService;

    @Override
    public boolean supports(ContentType type) {
        return type == ContentType.PLATFORM_POST;
    }

    @Override
    public ResolvedContent resolveVisible(ContentRef contentRef, User viewer) {
        try {
            PlatformPostResponse post = platformPostService.getPostById(contentRef.id());
            Long authorId = post.getAuthor() != null ? post.getAuthor().getId() : null;
            String body = post.getBody();
            String summary = body != null && body.length() > 100 ? body.substring(0, 100) + "..." : body;
            return ResolvedContent.of(contentRef, authorId, null, summary);
        } catch (PlatformPostNotFoundException | PlatformPostAccessDeniedException ex) {
            log.debug("PlatformPost {} is inaccessible: {}", contentRef.id(), ex.getMessage());
            return ResolvedContent.inaccessible(contentRef);
        } catch (Exception ex) {
            log.warn("Error resolving PlatformPost {}: {}", contentRef.id(), ex.getMessage());
            return ResolvedContent.inaccessible(contentRef);
        }
    }
}
