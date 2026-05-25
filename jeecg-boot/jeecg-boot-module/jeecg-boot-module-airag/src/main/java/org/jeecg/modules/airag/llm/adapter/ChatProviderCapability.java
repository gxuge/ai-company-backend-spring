package org.jeecg.modules.airag.llm.adapter;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 模型能力声明（简化版）。
 * 说明：只覆盖当前 airag 调用链中已存在参数。
 */
@Data
@Builder
public class ChatProviderCapability {

    @Builder.Default
    private boolean supportsTools = true;

    @Builder.Default
    private boolean supportsThinking = true;

    @Builder.Default
    private boolean supportsReasoningEffort = true;

    @Builder.Default
    private boolean supportsTemperature = true;

    @Builder.Default
    private boolean supportsTopP = true;

    @Builder.Default
    private boolean supportsPresencePenalty = true;

    @Builder.Default
    private boolean supportsFrequencyPenalty = true;

    @Builder.Default
    private boolean supportsSearch = true;

    @Builder.Default
    private Set<String> unsupportedParams = new HashSet<>();

    @Builder.Default
    private Map<String, Object> defaultParams = Collections.emptyMap();

    public static ChatProviderCapability defaultCapability() {
        return ChatProviderCapability.builder().build();
    }
}

