package org.jeecg.modules.airag.llm.adapter;

import lombok.Getter;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.llm.entity.AiragModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用参数适配上下文。
 */
@Getter
public class ChatParamAdaptContext {

    private final AiragModel model;
    private final AIChatParams params;
    private final ChatProviderCapability capability;
    private final List<String> warnings = new ArrayList<>();
    private final Map<String, Object> openAiCustomParameters = new LinkedHashMap<>();

    public ChatParamAdaptContext(AiragModel model, AIChatParams params, ChatProviderCapability capability) {
        this.model = model;
        this.params = params;
        this.capability = capability;
    }

    public void addWarning(String warning) {
        if (warning != null && !warning.isEmpty()) {
            warnings.add(warning);
        }
    }
}

