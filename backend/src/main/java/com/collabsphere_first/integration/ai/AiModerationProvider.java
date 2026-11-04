package com.collabsphere_first.integration.ai;

/** Provider SPI used by the Community moderation facade. */
public interface AiModerationProvider {
    boolean isConfigured();
    AiModerationService.ModerationResult classify(String text);
}
