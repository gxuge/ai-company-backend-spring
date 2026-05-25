package org.jeecg.modules.airag.llm.adapter.impl;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.llm.adapter.ChatParamAdaptContext;
import org.jeecg.modules.airag.llm.adapter.ChatProviderCapability;
import org.jeecg.modules.airag.llm.adapter.ChatProviderParamAdapter;
import org.jeecg.modules.airag.llm.consts.LLMConsts;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * MiniMax 文本模型参数适配。
 */
@Component
public class MiniMaxChatProviderParamAdapter implements ChatProviderParamAdapter {

    private static final String MINIMAX_DEFAULT_OPENAI_BASE_URL = "https://api.minimax.io/v1";

    @Override
    public boolean supports(AiragModel model, AIChatParams params) {
        return model != null
                && "MINIMAX".equalsIgnoreCase(oConvertUtils.getString(model.getProvider()))
                && LLMConsts.MODEL_TYPE_LLM.equalsIgnoreCase(oConvertUtils.getString(model.getModelType()));
    }

    @Override
    public ChatProviderCapability capability(AiragModel model, AIChatParams params) {
        return ChatProviderCapability.builder()
                .supportsTools(true)
                .supportsThinking(false)
                .supportsReasoningEffort(false)
                .supportsSearch(false)
                .supportsPresencePenalty(false)
                .supportsFrequencyPenalty(false)
                .defaultParams(Map.of("noThinking", true))
                .build();
    }

    @Override
    public void adapt(ChatParamAdaptContext context) {
        AIChatParams params = context.getParams();
        params.setProvider("OPENAI");
        params.setBaseUrl(normalizeBaseUrl(params.getBaseUrl(), MINIMAX_DEFAULT_OPENAI_BASE_URL));
        if (params.getNoThinking() == null) {
            params.setNoThinking(true);
        }
    }

    private String normalizeBaseUrl(String baseUrl, String defaultValue) {
        String value = StringUtils.hasText(baseUrl) ? baseUrl.trim() : defaultValue;
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (!value.endsWith("/v1")) {
            value = value + "/v1";
        }
        return value;
    }
}

