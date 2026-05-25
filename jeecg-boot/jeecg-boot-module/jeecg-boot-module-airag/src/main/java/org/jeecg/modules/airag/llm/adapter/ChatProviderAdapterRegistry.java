package org.jeecg.modules.airag.llm.adapter;

import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provider 适配器注册表。
 */
@Component
public class ChatProviderAdapterRegistry {

    @Autowired(required = false)
    private List<ChatProviderParamAdapter> adapters;

    private final ChatProviderParamAdapter noopAdapter = new ChatProviderParamAdapter() {
        @Override
        public boolean supports(AiragModel model, AIChatParams params) {
            return true;
        }

        @Override
        public ChatProviderCapability capability(AiragModel model, AIChatParams params) {
            return ChatProviderCapability.defaultCapability();
        }

        @Override
        public void adapt(ChatParamAdaptContext context) {
            // no-op
        }
    };

    public ChatProviderParamAdapter resolve(AiragModel model, AIChatParams params) {
        if (adapters == null || adapters.isEmpty()) {
            return noopAdapter;
        }
        for (ChatProviderParamAdapter adapter : adapters) {
            if (adapter.supports(model, params)) {
                return adapter;
            }
        }
        return noopAdapter;
    }
}

