package com.collabsphere_first.integration.ai.impl;

import com.collabsphere_first.integration.ai.AiModerationService;
import com.collabsphere_first.integration.ai.AiModerationProvider;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiModerationServiceImpl implements AiModerationService {

    private final ObjectProvider<AiModerationProvider> providers;

    @Value("${collabster.communities.ai-moderation.mock-enabled:false}")
    private boolean mockEnabled;

    @Override
    public CompletableFuture<ModerationResult> classifyContentAsync(String text) {
        return CompletableFuture.supplyAsync(() -> classify(text));
    }

    private ModerationResult classify(String text) {
        AiModerationProvider provider = providers.getIfAvailable();
        if (provider != null && provider.isConfigured()) {
            try {
                return provider.classify(text);
            } catch (RuntimeException failure) {
                log.warn("AI moderation provider failed; routing content to manual review ({})", failure.getClass().getSimpleName());
                return ModerationResult.FLAGGED;
            }
        }
        if (!mockEnabled) return ModerationResult.FLAGGED;
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return normalized.contains("spam") ? ModerationResult.FLAGGED : ModerationResult.PASSED;
    }
}
