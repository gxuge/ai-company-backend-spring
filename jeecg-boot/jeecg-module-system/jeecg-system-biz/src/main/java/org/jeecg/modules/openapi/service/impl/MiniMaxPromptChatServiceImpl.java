package org.jeecg.modules.openapi.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import jakarta.annotation.Resource;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.llm.consts.LLMConsts;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.jeecg.modules.airag.llm.mapper.AiragModelMapper;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Unified prompt chat service.
 *
 * Keeps historical bean name while routing model from AIRAG DB config.
 */
@Service("miniMaxPromptChatService")
@Slf4j
public class MiniMaxPromptChatServiceImpl implements IPromptChatService {

    @Resource
    private PromptChatConfigBean promptChatConfigBean;
    @Resource
    private AiragAppMapper airagAppMapper;
    @Resource
    private AiragModelMapper airagModelMapper;
    @Resource
    private IAIChatHandler aiChatHandler;

    @Override
    public String provider() {
        AiragModel model = resolvePromptModel();
        String provider = model.getProvider();
        return StringUtils.hasText(provider) ? provider.trim().toLowerCase(Locale.ROOT) : "unknown";
    }

    @Override
    public String chat(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new JeecgBootBizTipException("Prompt must not be blank");
        }
        AiragModel model = resolvePromptModel();
        PromptProviderBranch branch = resolveProviderBranch(model);
        logPromptRoute(model, branch, false);
        AIChatParams params = buildBaseParams(model, branch);
        List<ChatMessage> messages = List.of(new UserMessage(prompt));
        String content = aiChatHandler.completions(model.getId(), messages, params);
        if (!StringUtils.hasText(content)) {
            throw new JeecgBootBizTipException("Prompt chat response is empty");
        }
        return content.trim();
    }

    @Override
    public String chatToolCall(String developerPrompt, String userPrompt, String toolSchema) {
        if (!StringUtils.hasText(userPrompt)) {
            throw new JeecgBootBizTipException("User prompt must not be blank");
        }
        AiragModel model = resolvePromptModel();
        PromptProviderBranch branch = resolveProviderBranch(model);

        List<ChatMessage> messages = new ArrayList<>();
        if (StringUtils.hasText(developerPrompt)) {
            messages.add(SystemMessage.from(developerPrompt.trim()));
        }
        messages.add(UserMessage.from(userPrompt.trim()));
        AIChatParams params = buildBaseParams(model, branch);

        if (!StringUtils.hasText(toolSchema)) {
            logPromptRoute(model, branch, false);
            String content = aiChatHandler.completions(model.getId(), messages, params);
            if (!StringUtils.hasText(content)) {
                throw new JeecgBootBizTipException("Prompt chat response is empty");
            }
            return content.trim();
        }

        ToolSpecification toolSpecification = buildToolSpecification(toolSchema);
        if (toolSpecification == null) {
            logPromptRoute(model, branch, false);
            String content = aiChatHandler.completions(model.getId(), messages, params);
            if (!StringUtils.hasText(content)) {
                throw new JeecgBootBizTipException("Prompt chat response is empty");
            }
            return content.trim();
        }

        if (!supportsToolCall(model, branch)) {
            if (Boolean.FALSE.equals(promptChatConfigBean.getToolCallAutoDowngrade())) {
                throw new JeecgBootBizTipException("Current model does not support tool call, model=" + model.getModelName());
            }
            logPromptRoute(model, branch, false);
            String content = aiChatHandler.completions(model.getId(), messages, params);
            if (!StringUtils.hasText(content)) {
                throw new JeecgBootBizTipException("Prompt chat response is empty");
            }
            return content.trim();
        }

        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        tools.put(toolSpecification, (toolExecutionRequest, memoryId) -> toolExecutionRequest.arguments());
        params.setTools(tools);

        logPromptRoute(model, branch, true);
        String content = aiChatHandler.completions(model.getId(), messages, params);
        if (!StringUtils.hasText(content)) {
            throw new JeecgBootBizTipException("Prompt chat response is empty");
        }
        return content.trim();
    }

    private ToolSpecification buildToolSpecification(String toolSchema) {
        try {
            JSONObject root = JSONObject.parseObject(toolSchema);
            if (root == null) {
                return null;
            }
            String name = trimToNull(root.getString("name"));
            if (!StringUtils.hasText(name)) {
                return null;
            }
            String description = trimToNull(root.getString("description"));
            JSONObject parameters = root.getJSONObject("parameters");

            JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
            if (parameters != null) {
                JSONObject properties = parameters.getJSONObject("properties");
                if (properties != null) {
                    for (String propName : properties.keySet()) {
                        if (!StringUtils.hasText(propName)) {
                            continue;
                        }
                        JSONObject prop = properties.getJSONObject(propName);
                        if (prop == null) {
                            schemaBuilder.addStringProperty(propName, "");
                            continue;
                        }
                        String propType = trimToNull(prop.getString("type"));
                        String propDesc = trimToNull(prop.getString("description"));
                        if ("number".equalsIgnoreCase(propType) || "integer".equalsIgnoreCase(propType)) {
                            schemaBuilder.addNumberProperty(propName, propDesc == null ? "" : propDesc);
                        } else if ("boolean".equalsIgnoreCase(propType)) {
                            schemaBuilder.addBooleanProperty(propName, propDesc == null ? "" : propDesc);
                        } else {
                            // string / array / object fallback to string for robust compatibility
                            schemaBuilder.addStringProperty(propName, propDesc == null ? "" : propDesc);
                        }
                    }
                }

                JSONArray required = parameters.getJSONArray("required");
                if (required != null && !required.isEmpty()) {
                    List<String> requiredList = new ArrayList<>();
                    for (Object item : required) {
                        if (item == null) {
                            continue;
                        }
                        String key = trimToNull(String.valueOf(item));
                        if (StringUtils.hasText(key)) {
                            requiredList.add(key);
                        }
                    }
                    if (!requiredList.isEmpty()) {
                        schemaBuilder.required(requiredList.toArray(new String[0]));
                    }
                }
            }

            ToolSpecification.Builder builder = ToolSpecification.builder()
                    .name(name)
                    .parameters(schemaBuilder.build());
            if (StringUtils.hasText(description)) {
                builder.description(description);
            }
            return builder.build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private AiragModel resolvePromptModel() {
        String modelId = trimToNull(promptChatConfigBean.getModelId());
        if (!StringUtils.hasText(modelId)) {
            String appId = trimToNull(promptChatConfigBean.getAppId());
            if (!StringUtils.hasText(appId)) {
                throw new JeecgBootBizTipException("Please configure jeecg.airag.prompt-chat.app-id or model-id first");
            }
            AiragApp app = airagAppMapper.getByIdIgnoreTenant(appId);
            if (app == null) {
                throw new JeecgBootBizTipException("No AIRAG app found, appId=" + appId);
            }
            modelId = trimToNull(app.getModelId());
            if (!StringUtils.hasText(modelId)) {
                throw new JeecgBootBizTipException("App has no text model configured, appId=" + appId);
            }
        }

        AiragModel model = airagModelMapper.getByIdIgnoreTenant(modelId);
        if (model == null) {
            throw new JeecgBootBizTipException("No AIRAG model found, modelId=" + modelId);
        }
        if (!LLMConsts.MODEL_TYPE_LLM.equalsIgnoreCase(trimToNull(model.getModelType()))) {
            throw new JeecgBootBizTipException("Prompt chat only supports LLM model, current type=" + model.getModelType());
        }
        if (model.getActivateFlag() == null || model.getActivateFlag() != 1) {
            throw new JeecgBootBizTipException("Model is not activated. Please test/activate it first, modelId=" + modelId);
        }
        return model;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private AIChatParams buildBaseParams(AiragModel model, PromptProviderBranch branch) {
        AIChatParams params = new AIChatParams();
        boolean noThink = promptChatConfigBean.getNoThinkDefault() == null || promptChatConfigBean.getNoThinkDefault();
        params.setNoThinking(noThink);
        params.setReturnThinking(false);
        if (PromptProviderBranch.DEEPSEEK.equals(branch)) {
            params.setNoThinking(true);
            params.setReturnThinking(false);
        }
        return params;
    }

    private PromptProviderBranch resolveProviderBranch(AiragModel model) {
        String provider = trimToNull(model.getProvider());
        if (!StringUtils.hasText(provider)) {
            return PromptProviderBranch.OPENAI_COMPATIBLE;
        }
        return switch (provider.toUpperCase(Locale.ROOT)) {
            case "DEEPSEEK" -> PromptProviderBranch.DEEPSEEK;
            case "MINIMAX" -> PromptProviderBranch.MINIMAX;
            case "GEMINI" -> PromptProviderBranch.GEMINI;
            default -> PromptProviderBranch.OPENAI_COMPATIBLE;
        };
    }

    private boolean supportsToolCall(AiragModel model, PromptProviderBranch branch) {
        if (PromptProviderBranch.DEEPSEEK.equals(branch)) {
            String modelName = trimToNull(model.getModelName());
            if (StringUtils.hasText(modelName)) {
                return !LLMConsts.DEEPSEEK_REASONER.equalsIgnoreCase(modelName);
            }
        }
        return true;
    }

    private void logPromptRoute(AiragModel model, PromptProviderBranch branch, boolean withTools) {
        log.info("[PROMPT_CHAT_ROUTE] provider={} modelName={} modelType={} branch={} tools={}",
                trimToNull(model.getProvider()),
                trimToNull(model.getModelName()),
                trimToNull(model.getModelType()),
                branch.name(),
                withTools);
    }

    private enum PromptProviderBranch {
        DEEPSEEK,
        MINIMAX,
        GEMINI,
        OPENAI_COMPATIBLE
    }
}
