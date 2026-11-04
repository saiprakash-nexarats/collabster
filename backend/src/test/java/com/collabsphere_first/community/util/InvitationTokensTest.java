package com.collabsphere_first.community.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvitationTokensTest {
    @Test
    void hashesTokensDeterministicallyWithoutReturningRawValue() {
        String raw = "secure-invite-token";
        String hash = InvitationTokens.sha256(raw);
        assertEquals(64, hash.length());
        assertEquals(hash, InvitationTokens.sha256(raw));
        assertNotEquals(raw, hash);
        assertNotEquals(hash, InvitationTokens.sha256(raw + "-other"));
    }
}
