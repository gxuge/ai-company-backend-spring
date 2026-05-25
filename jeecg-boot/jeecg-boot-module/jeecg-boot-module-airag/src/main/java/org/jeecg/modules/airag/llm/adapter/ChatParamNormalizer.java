package org.jeecg.modules.airag.llm.adapter;

import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 通用参数归一化与裁剪。
 */
@Component
public class ChatParamNormalizer {

    public void normalize(ChatParamAdaptContext context) {
        AIChatParams params = context.getParams();
        ChatProviderCapability capability = context.getCapability();

        applyDefaultParams(params, capability.getDefaultParams());
        dropUnsupportedParams(params, capability.getUnsupportedParams(), context);
        dropByCapability(params, capability, context);
    }

    private void applyDefaultParams(AIChatParams params, Map<String, Object> defaultParams) {
        if (defaultParams == null || defaultParams.isEmpty()) {
            return;
        }
        if (params.getTemperature() == null && defaultParams.containsKey("temperature")) {
            params.setTemperature(toDouble(defaultParams.get("temperature")));
        }
        if (params.getTopP() == null && defaultParams.containsKey("topP")) {
            params.setTopP(toDouble(defaultParams.get("topP")));
        }
        if (params.getMaxTokens() == null && defaultParams.containsKey("maxTokens")) {
            params.setMaxTokens(toInteger(defaultParams.get("maxTokens")));
        }
        if (params.getTimeout() == null && defaultParams.containsKey("timeout")) {
            params.setTimeout(toInteger(defaultParams.get("timeout")));
        }
        if (params.getEnableSearch() == null && defaultParams.containsKey("enableSearch")) {
            params.setEnableSearch(toBoolean(defaultParams.get("enableSearch")));
        }
        if (params.getNoThinking() == null && defaultParams.containsKey("noThinking")) {
            params.setNoThinking(toBoolean(defaultParams.get("noThinking")));
        }
        if (params.getReturnThinking() == null && defaultParams.containsKey("returnThinking")) {
            params.setReturnThinking(toBoolean(defaultParams.get("returnThinking")));
        }
    }

    private void dropUnsupportedParams(AIChatParams params, Set<String> unsupportedParams, ChatParamAdaptContext context) {
        if (unsupportedParams == null || unsupportedParams.isEmpty()) {
            return;
        }
        for (String param : unsupportedParams) {
            switch (param) {
                case "temperature" -> dropTemperature(params, context, "unsupportedParams");
                case "topP" -> dropTopP(params, context, "unsupportedParams");
                case "presencePenalty" -> dropPresencePenalty(params, context, "unsupportedParams");
                case "frequencyPenalty" -> dropFrequencyPenalty(params, context, "unsupportedParams");
                case "tools" -> dropTools(params, context, "unsupportedParams");
                case "search" -> dropSearch(params, context, "unsupportedParams");
                default -> {
                }
            }
        }
    }

    private void dropByCapability(AIChatParams params, ChatProviderCapability capability, ChatParamAdaptContext context) {
        if (!capability.isSupportsTemperature()) {
            dropTemperature(params, context, "capability");
        }
        if (!capability.isSupportsTopP()) {
            dropTopP(params, context, "capability");
        }
        if (!capability.isSupportsPresencePenalty()) {
            dropPresencePenalty(params, context, "capability");
        }
        if (!capability.isSupportsFrequencyPenalty()) {
            dropFrequencyPenalty(params, context, "capability");
        }
        if (!capability.isSupportsTools()) {
            dropTools(params, context, "capability");
        }
        if (!capability.isSupportsSearch()) {
            dropSearch(params, context, "capability");
        }
        if (!capability.isSupportsThinking()) {
            if (params.getNoThinking() != null || params.getReturnThinking() != null) {
                context.addWarning("thinking 参数不受支持，已忽略 noThinking/returnThinking");
            }
            params.setNoThinking(null);
            params.setReturnThinking(null);
        }
    }

    private void dropTemperature(AIChatParams params, ChatParamAdaptContext context, String source) {
        if (params.getTemperature() != null) {
            params.setTemperature(null);
            context.addWarning("temperature 不支持，已自动删除（source=" + source + "）");
        }
    }

    private void dropTopP(AIChatParams params, ChatParamAdaptContext context, String source) {
        if (params.getTopP() != null) {
            params.setTopP(null);
            context.addWarning("topP 不支持，已自动删除（source=" + source + "）");
        }
    }

    private void dropPresencePenalty(AIChatParams params, ChatParamAdaptContext context, String source) {
        if (params.getPresencePenalty() != null) {
            params.setPresencePenalty(null);
            context.addWarning("presencePenalty 不支持，已自动删除（source=" + source + "）");
        }
    }

    private void dropFrequencyPenalty(AIChatParams params, ChatParamAdaptContext context, String source) {
        if (params.getFrequencyPenalty() != null) {
            params.setFrequencyPenalty(null);
            context.addWarning("frequencyPenalty 不支持，已自动删除（source=" + source + "）");
        }
    }

    private void dropTools(AIChatParams params, ChatParamAdaptContext context, String source) {
        boolean hasTools = params.getTools() != null && !params.getTools().isEmpty();
        boolean hasPlugins = params.getPluginIds() != null && !params.getPluginIds().isEmpty();
        if (hasTools || hasPlugins) {
            context.addWarning("tools 不支持，已自动删除插件工具（source=" + source + "）");
        }
        params.setTools(null);
        params.setPluginIds(null);
        params.setMcpToolProviders(null);
        params.setMcpToolProviderWrappers(null);
    }

    private void dropSearch(AIChatParams params, ChatParamAdaptContext context, String source) {
        if (params.getEnableSearch() != null) {
            context.addWarning("search 不支持，已自动删除（source=" + source + "）");
            params.setEnableSearch(null);
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
            return false;
        }
        return null;
    }
}
