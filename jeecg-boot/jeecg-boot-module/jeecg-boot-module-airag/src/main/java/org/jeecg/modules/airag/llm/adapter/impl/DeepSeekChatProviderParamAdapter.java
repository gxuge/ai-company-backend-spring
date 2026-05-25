package org.jeecg.modules.airag.llm.adapter.impl;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.llm.adapter.ChatParamAdaptContext;
import org.jeecg.modules.airag.llm.adapter.ChatProviderCapability;
import org.jeecg.modules.airag.llm.adapter.ChatProviderParamAdapter;
import org.jeecg.modules.airag.llm.consts.LLMConsts;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DeepSeek 参数适配。
 */
@Component
public class DeepSeekChatProviderParamAdapter implements ChatProviderParamAdapter {

    @Override
    public boolean supports(AiragModel model, AIChatParams params) {
        return model != null && "DEEPSEEK".equalsIgnoreCase(oConvertUtils.getString(model.getProvider()));
    }

    @Override
    public ChatProviderCapability capability(AiragModel model, AIChatParams params) {
        boolean isLegacyReasoner = model != null
                && LLMConsts.DEEPSEEK_REASONER.equalsIgnoreCase(oConvertUtils.getString(model.getModelName()));
        return ChatProviderCapability.builder()
                .supportsTools(!isLegacyReasoner)
                .supportsThinking(true)
                .supportsReasoningEffort(true)
                .supportsTemperature(false)
                .supportsTopP(false)
                .supportsPresencePenalty(false)
                .supportsFrequencyPenalty(false)
                .supportsSearch(false)
                .unsupportedParams(Set.of("presencePenalty", "frequencyPenalty"))
                .defaultParams(Map.of("noThinking", true))
                .build();
    }

    @Override
    public void adapt(ChatParamAdaptContext context) {
        AIChatParams params = context.getParams();
        String thinkingType = resolveThinkingType(context.getModel(), params);
        Map<String, Object> thinking = new HashMap<>(1);
        thinking.put("type", thinkingType);
        context.getOpenAiCustomParameters().put("thinking", thinking);
        if ("disabled".equals(thinkingType)) {
            params.setReturnThinking(false);
        }
    }

    private String resolveThinkingType(AiragModel model, AIChatParams params) {
        if (params != null && params.getNoThinking() != null) {
            return params.getNoThinking() ? "disabled" : "enabled";
        }
        String fromModel = findThinkingTypeFromModelParams(model);
        if (oConvertUtils.isNotEmpty(fromModel)) {
            return fromModel;
        }
        Boolean noThinking = params.getNoThinking();
        return (noThinking == null || noThinking) ? "disabled" : "enabled";
    }

    private String findThinkingTypeFromModelParams(AiragModel model) {
        if (model == null || oConvertUtils.isEmpty(model.getModelParams())) {
            return null;
        }
        try {
            JSONObject modelParams = JSONObject.parseObject(model.getModelParams());
            if (modelParams == null) {
                return null;
            }
            String direct = modelParams.getString("thinkingType");
            if (isThinkingType(direct)) {
                return direct.trim().toLowerCase();
            }
            JSONObject thinkingObj = modelParams.getJSONObject("thinking");
            if (thinkingObj != null) {
                String nested = thinkingObj.getString("type");
                if (isThinkingType(nested)) {
                    return nested.trim().toLowerCase();
                }
            }
        } catch (Exception ignore) {
            // ignore
        }
        return null;
    }

    private boolean isThinkingType(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return "enabled".equals(v) || "disabled".equals(v) || "auto".equals(v);
    }
}
