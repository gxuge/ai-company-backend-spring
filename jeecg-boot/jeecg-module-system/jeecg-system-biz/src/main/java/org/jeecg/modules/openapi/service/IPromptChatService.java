package org.jeecg.modules.openapi.service;

/**
 * Prompt chat service for one-shot prompt generation.
 */
public interface IPromptChatService {
    /**
     * Provider name.
     */
    String provider();

    /**
     * Send already-rendered prompt text.
     */
    String chat(String prompt);

    /**
     * Send structured prompt sections.
     */
    String chatToolCall(String developerPrompt, String userPrompt, String toolSchema);
}

