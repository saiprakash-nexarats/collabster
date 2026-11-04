package com.collabsphere_first.integration.ai;

import java.util.concurrent.CompletableFuture;

public interface AiModerationService {
    enum ModerationResult {
        PASSED,
        FLAGGED,
        VIOLATION
    }

    CompletableFuture<ModerationResult> classifyContentAsync(String text);
}
