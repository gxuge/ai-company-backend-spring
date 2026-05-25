package org.jeecg.modules.airag.llm.adapter;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.ai.factory.AiModelFactory;
import org.jeecg.ai.factory.AiModelOptions;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Provider 适配执行入口。
 */
@Slf4j
@Component
public class ChatProviderAdaptService {

    @Autowired
    private ChatProviderAdapterRegistry adapterRegistry;

    @Autowired
    private ChatParamNormalizer paramNormalizer;

    public ChatParamAdaptContext adapt(AiragModel model, AIChatParams params) {
        ChatProviderParamAdapter adapter = adapterRegistry.resolve(model, params);
        ChatProviderCapability capability = adapter.capability(model, params);
        ChatParamAdaptContext context = new ChatParamAdaptContext(model, params, capability);
        paramNormalizer.normalize(context);
        adapter.adapt(context);
        return context;
    }

    public void prepareModelCacheIfNeeded(ChatParamAdaptContext context) {
        if (context == null || context.getOpenAiCustomParameters().isEmpty()) {
            return;
        }
        AIChatParams params = context.getParams();
        if (!"OPENAI".equalsIgnoreCase(oConvertUtils.getString(params.getProvider()))) {
            return;
        }
        try {
            AiModelOptions options = params.toModelOptions();
            if (!StringUtils.hasText(options.getApiKey()) || !StringUtils.hasText(options.getModelName())) {
                return;
            }
            String chatCacheKey = options.toString();
            String streamCacheKey = "STEAM_" + options;
            logNativeRequest(options, context.getOpenAiCustomParameters());
            Object chatModel = buildOpenAiChatModel(options, context.getOpenAiCustomParameters());
            Object streamModel = buildOpenAiStreamingModel(options, context.getOpenAiCustomParameters());
            cacheModel(chatCacheKey, chatModel);
            cacheModel(streamCacheKey, streamModel);
        } catch (Exception e) {
            log.warn("Prepare provider custom model cache failed: {}", e.getMessage());
        }
    }

    private OpenAiChatModel buildOpenAiChatModel(AiModelOptions options, Map<String, Object> customParameters) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(options.getApiKey())
                .baseUrl(options.getBaseUrl())
                .modelName(options.getModelName())
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .presencePenalty(options.getPresencePenalty())
                .frequencyPenalty(options.getFrequencyPenalty())
                .timeout(Duration.ofSeconds(positiveOrDefault(options.getTimeout(), 120)))
                .maxRetries(0)
                .returnThinking(options.getReturnThinking())
                .customParameters(customParameters);
        if (options.getMaxTokens() != null && options.getMaxTokens() > 0) {
            builder.maxTokens(options.getMaxTokens());
        }
        return builder.build();
    }

    private OpenAiStreamingChatModel buildOpenAiStreamingModel(AiModelOptions options, Map<String, Object> customParameters) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(options.getApiKey())
                .baseUrl(options.getBaseUrl())
                .modelName(options.getModelName())
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .presencePenalty(options.getPresencePenalty())
                .frequencyPenalty(options.getFrequencyPenalty())
                .timeout(Duration.ofSeconds(positiveOrDefault(options.getTimeout(), 120)))
                .returnThinking(options.getReturnThinking())
                .customParameters(customParameters);
        if (options.getMaxTokens() != null && options.getMaxTokens() > 0) {
            builder.maxTokens(options.getMaxTokens());
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private void cacheModel(String cacheKey, Object model) throws NoSuchFieldException, IllegalAccessException {
        Field cacheField = AiModelFactory.class.getDeclaredField("chatModelCache");
        cacheField.setAccessible(true);
        Object cache = cacheField.get(null);
        if (cache instanceof Map<?, ?> map) {
            ((Map<String, Object>) map).put(cacheKey, model);
        }
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return (value == null || value <= 0) ? defaultValue : value;
    }

    private void logNativeRequest(AiModelOptions options, Map<String, Object> customParameters) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("provider", "OPENAI");
            payload.put("modelName", options.getModelName());
            payload.put("baseUrl", options.getBaseUrl());
            payload.put("temperature", options.getTemperature());
            payload.put("topP", options.getTopP());
            payload.put("presencePenalty", options.getPresencePenalty());
            payload.put("frequencyPenalty", options.getFrequencyPenalty());
            payload.put("maxTokens", options.getMaxTokens());
            payload.put("timeout", options.getTimeout());
            payload.put("returnThinking", options.getReturnThinking());
            payload.put("apiKey", maskSecret(options.getApiKey()));
            payload.put("customParameters", sanitizeSecrets(customParameters));
            log.info("[LLM_NATIVE_REQUEST] {}", payload);
        } catch (Exception e) {
            log.warn("打印底层模型请求参数失败: {}", e.getMessage());
        }
    }

    private Object sanitizeSecrets(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "null" : String.valueOf(entry.getKey());
                Object val = entry.getValue();
                if (isSensitiveKey(key)) {
                    sanitized.put(key, maskSecret(val == null ? null : String.valueOf(val)));
                } else {
                    sanitized.put(key, sanitizeSecrets(val));
                }
            }
            return sanitized;
        }
        return value;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("apikey")
                || lower.contains("api_key")
                || lower.contains("secret")
                || lower.contains("token")
                || lower.contains("authorization")
                || lower.contains("password");
    }

    private String maskSecret(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        if (text.length() <= 8) {
            return "****";
        }
        return text.substring(0, 4) + "****" + text.substring(text.length() - 4);
    }
}
