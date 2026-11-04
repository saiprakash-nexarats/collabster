package com.collabsphere_first.interaction.resolver;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.community.dto.PostDto;
import com.collabsphere_first.community.exception.PostNotFoundException;
import com.collabsphere_first.community.service.CommunityContentAccessService;
import com.collabsphere_first.community.service.ContentService;
import com.collabsphere_first.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class CommunityPostContentResolver implements ContentResolver {

    private final CommunityContentAccessService communityContentAccessService;
    private final ContentService contentService;

    public CommunityPostContentResolver(CommunityContentAccessService communityContentAccessService) {
        this.communityContentAccessService = communityContentAccessService;
        this.contentService = null;
    }

    public CommunityPostContentResolver(ContentService contentService) {
        this.communityContentAccessService = null;
        this.contentService = contentService;
    }

    @Autowired
    public CommunityPostContentResolver(
            CommunityContentAccessService communityContentAccessService,
            @Lazy ContentService contentService
    ) {
        this.communityContentAccessService = communityContentAccessService;
        this.contentService = contentService;
    }

    @Override
    public boolean supports(ContentType type) {
        return type == ContentType.COMMUNITY_POST;
    }

    @Override
    public ResolvedContent resolveVisible(ContentRef contentRef, User viewer) {
        Long requestingUserId = viewer != null ? viewer.getId() : null;

        if (communityContentAccessService != null) {
            Optional<PostDto> postOpt = communityContentAccessService.getPostIfAccessible(contentRef.id(), requestingUserId);
            if (postOpt.isPresent()) {
                PostDto postDto = postOpt.get();
                Long authorId = postDto.getAuthorId();
                String title = postDto.getTitle();
                String body = postDto.getBody();
                String summary = body != null && body.length() > 100 ? body.substring(0, 100) + "..." : body;
                return ResolvedContent.of(contentRef, authorId, title, summary);
            }
            return ResolvedContent.inaccessible(contentRef);
        }

        if (contentService != null) {
            try {
                PostDto postDto = contentService.getPost(contentRef.id(), requestingUserId);
                Long authorId = postDto.getAuthorId();
                String title = postDto.getTitle();
                String body = postDto.getBody();
                String summary = body != null && body.length() > 100 ? body.substring(0, 100) + "..." : body;
                return ResolvedContent.of(contentRef, authorId, title, summary);
            } catch (PostNotFoundException ex) {
                log.debug("CommunityPost {} not found: {}", contentRef.id(), ex.getMessage());
                return ResolvedContent.inaccessible(contentRef);
            } catch (Exception ex) {
                log.debug("CommunityPost {} not accessible: {}", contentRef.id(), ex.getMessage());
                return ResolvedContent.inaccessible(contentRef);
            }
        }

        return ResolvedContent.inaccessible(contentRef);
    }
}
