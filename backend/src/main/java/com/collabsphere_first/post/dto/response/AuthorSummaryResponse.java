package com.collabsphere_first.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Safe public projection of a post author.
 * Strictly excludes passwords, refresh tokens, private emails, and sensitive user state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorSummaryResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String profilePicture;
}
