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
 * Gemini OpenAI-compatible 参数适配。
 */
@Component
public class GeminiChatProviderParamAdapter implements ChatProviderParamAdapter {

    private static final String GEMINI_DEFAULT_OPENAI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai";

    @Override
    public boolean supports(AiragModel model, AIChatParams params) {
        return model != null
                && "GEMINI".equalsIgnoreCase(oConvertUtils.getString(model.getProvider()))
                && LLMConsts.MODEL_TYPE_LLM.equalsIgnoreCase(oConvertUtils.getString(model.getModelType()));
    }

    @Override
    public ChatProviderCapability capability(AiragModel model, AIChatParams params) {
        return ChatProviderCapability.builder()
                .supportsTools(true)
                .supportsThinking(false)
                .supportsReasoningEffort(false)
                .supportsSearch(true)
                .supportsPresencePenalty(false)
                .supportsFrequencyPenalty(false)
                .defaultParams(Map.of("noThinking", true))
                .build();
    }

    @Override
    public void adapt(ChatParamAdaptContext context) {
        AIChatParams params = context.getParams();
        params.setProvider("OPENAI");
        params.setBaseUrl(normalizeGeminiOpenAiBaseUrl(params.getBaseUrl()));
        if (params.getNoThinking() == null) {
            params.setNoThinking(true);
        }
    }

    private String normalizeGeminiOpenAiBaseUrl(String baseUrl) {
        String value = StringUtils.hasText(baseUrl) ? baseUrl.trim() : GEMINI_DEFAULT_OPENAI_BASE_URL;
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (!value.toLowerCase().endsWith("/v1beta/openai")) {
            if (value.toLowerCase().endsWith("/v1beta")) {
                value = value + "/openai";
            } else if (value.toLowerCase().endsWith("/openai")) {
                // keep
            } else {
                value = value + "/v1beta/openai";
            }
        }
        return value;
    }
}

