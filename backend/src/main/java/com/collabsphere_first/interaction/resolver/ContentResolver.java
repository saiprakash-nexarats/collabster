package com.collabsphere_first.interaction.resolver;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.user.entity.User;

public interface ContentResolver {

    boolean supports(ContentType type);

    ResolvedContent resolveVisible(ContentRef contentRef, User viewer);
}
