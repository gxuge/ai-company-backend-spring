package org.jeecg.modules.openapi.service;

/**
 * Prompt 文本生成服务（模板拼接后调用）。
 */
public interface IPromptChatService {
    /**
     * 提供商名称。
     */
    String provider();

    /**
     * 发送拼接后的 Prompt 并返回文本结果。
     */
    String chat(String prompt);
}

