package org.jeecg.modules.openapi.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import jakarta.annotation.Resource;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.jeecg.modules.airag.agent.safety.GlobalSafetySkillPromptProvider;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.llm.consts.LLMConsts;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.jeecg.modules.airag.llm.mapper.AiragModelMapper;
import org.jeecg.modules.airag.safety.moderation.ModerationContextMessage;
import org.jeecg.modules.airag.safety.moderation.ModerationGuard;
import org.jeecg.modules.airag.safety.moderation.ModerationResult;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.jeecg.modules.system.monitor.TsAiLogCollector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.time.Duration;
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
    @Resource
    private TsAiLogCollector tsAiLogCollector;
    @Resource
    private GlobalSafetySkillPromptProvider globalSafetySkillPromptProvider;
    @Resource
    private ModerationGuard moderationGuard;

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
        ModerationResult inputModeration = moderationGuard.reviewInput(
                model.getId(), "prompt_chat", prompt.trim(), List.of(), null
        );
        if (!moderationGuard.isAllowed(inputModeration)) {
            return moderationGuard.safeReply();
        }
        PromptProviderBranch branch = resolveProviderBranch(model);
        AIChatParams params = buildBaseParams(model, branch);
        String safetySystemPrompt = buildSafeSystemPrompt(null);
        List<ChatMessage> messages = List.of(
                SystemMessage.from(safetySystemPrompt),
                UserMessage.from(prompt.trim())
        );
        logLlmRequest(model, branch, safetySystemPrompt, prompt, null, null, params, false);
        String content = aiChatHandler.completions(model.getId(), messages, params);
        logLlmResponse(model, content);
        if (!StringUtils.hasText(content)) {
            throw new JeecgBootBizTipException("Prompt chat response is empty");
        }
        return moderateOutput(model, branch, "prompt_chat", content.trim());
    }

    @Override
    public String chatToolCall(String developerPrompt, String userPrompt, String toolSchema) {
        if (!StringUtils.hasText(userPrompt)) {
            throw new JeecgBootBizTipException("User prompt must not be blank");
        }
        AiragModel model = resolvePromptModel();
        ModerationResult inputModeration = moderationGuard.reviewInput(
                model.getId(), "prompt_tool_call", userPrompt.trim(), List.of(), null
        );
        if (!moderationGuard.isAllowed(inputModeration)) {
            return moderationGuard.safeReply();
        }
        PromptProviderBranch branch = resolveProviderBranch(model);

        String safeSystemPrompt = buildSafeSystemPrompt(developerPrompt);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(safeSystemPrompt));
        messages.add(UserMessage.from(userPrompt.trim()));
        AIChatParams params = buildBaseParams(model, branch);

        if (!StringUtils.hasText(toolSchema)) {
            logLlmRequest(model, branch, safeSystemPrompt, userPrompt, null, null, params, false);
            String content = aiChatHandler.completions(model.getId(), messages, params);
            logLlmResponse(model, content);
            if (!StringUtils.hasText(content)) {
                throw new JeecgBootBizTipException("Prompt chat response is empty");
            }
            return moderateOutput(model, branch, "prompt_tool_call", content.trim());
        }

        ToolSpecification toolSpecification = buildToolSpecification(toolSchema);
        if (toolSpecification == null) {
            throw new JeecgBootBizTipException("Tool schema parse failed, model=" + model.getModelName());
        }
        String toolChoiceName = trimToNull(toolSpecification.name());

        if (!supportsToolCall(model, branch)) {
            if (Boolean.FALSE.equals(promptChatConfigBean.getToolCallAutoDowngrade())) {
                throw new JeecgBootBizTipException("Current model does not support tool call, model=" + model.getModelName());
            }
            logLlmRequest(model, branch, safeSystemPrompt, userPrompt, toolSchema, toolChoiceName, params, false);
            String content = aiChatHandler.completions(model.getId(), messages, params);
            logLlmResponse(model, content);
            if (!StringUtils.hasText(content)) {
                throw new JeecgBootBizTipException("Prompt chat response is empty");
            }
            return moderateOutput(model, branch, "prompt_tool_call", content.trim());
        }

        logLlmRequest(model, branch, safeSystemPrompt, userPrompt, toolSchema, toolChoiceName, params, true);
        String content = chatToolCallSingleRound(model, branch, messages, toolSpecification);
        logLlmResponse(model, content);
        if (!StringUtils.hasText(content)) {
            throw new JeecgBootBizTipException("Prompt chat response is empty");
        }
        return moderateOutput(model, branch, "prompt_tool_call", content.trim());
    }

    /**
     * 构建安全规则优先的 System Prompt。
     *
     * @param developerPrompt 业务开发者提示词
     * @return 最终 System Prompt
     */
    String buildSafeSystemPrompt(String developerPrompt) {
        return this.globalSafetySkillPromptProvider.prependToSystemPrompt(developerPrompt);
    }

    /**
     * 审核主模型输出，并在中风险时进行一次结构保持的安全改写。
     */
    private String moderateOutput(AiragModel model,
                                  PromptProviderBranch branch,
                                  String scene,
                                  String content) {
        return this.moderationGuard.reviewOutput(
                model.getId(),
                scene,
                content,
                List.<ModerationContextMessage>of(),
                null,
                unsafeOutput -> rewriteUnsafeOutput(model, branch, unsafeOutput)
        );
    }

    /**
     * 使用当前文本模型安全改写风险输出。
     */
    private String rewriteUnsafeOutput(AiragModel model,
                                       PromptProviderBranch branch,
                                       String unsafeOutput) {
        String rewriteInstruction =
                "请安全改写下面的模型输出。保持原有语言、JSON结构、字段和有效信息，"
                        + "删除或概括不安全细节，不新增敏感内容。只返回改写后的结果。\n\n"
                        + unsafeOutput;
        List<ChatMessage> messages = List.of(
                SystemMessage.from(buildSafeSystemPrompt(null)),
                UserMessage.from(rewriteInstruction)
        );
        AIChatParams params = buildBaseParams(model, branch);
        return this.aiChatHandler.completions(model.getId(), messages, params);
    }

    private String chatToolCallSingleRound(AiragModel model,
                                           PromptProviderBranch branch,
                                           List<ChatMessage> messages,
                                           ToolSpecification toolSpecification) {
        OpenAiChatModel chatModel = buildSingleRoundChatModel(model, branch);
        String modelName = trimToNull(model == null ? null : model.getModelName());
        if (!StringUtils.hasText(modelName)) {
            throw new JeecgBootBizTipException("Prompt chat modelName is required, modelId=" + (model == null ? null : model.getId()));
        }
        JSONObject modelParams = parseModelParams(model);
        OpenAiChatRequestParameters.Builder requestBuilder = OpenAiChatRequestParameters.builder()
                .modelName(modelName)
                .toolSpecifications(toolSpecification)
                .toolChoice(ToolChoice.REQUIRED)
                .parallelToolCalls(false)
                .customParameters(buildSingleRoundCustomParameters(branch));
        if (modelParams != null) {
            applyOptionalRequestParams(requestBuilder, modelParams);
        }
        OpenAiChatRequestParameters requestParameters = requestBuilder.build();
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .parameters(requestParameters)
                .build();
        ChatResponse response = chatModel.doChat(chatRequest);
        AiMessage aiMessage = response == null ? null : response.aiMessage();
        if (aiMessage == null) {
            return null;
        }
        List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();
        if (toolRequests != null && !toolRequests.isEmpty()) {
            ToolExecutionRequest firstRequest = toolRequests.get(0);
            return firstRequest == null ? null : firstRequest.arguments();
        }
        return trimToNull(aiMessage.text());
    }

    private OpenAiChatModel buildSingleRoundChatModel(AiragModel model,
                                                      PromptProviderBranch branch) {
        String apiKey = extractApiKey(model);
        String baseUrl = normalizeOpenAiBaseUrl(model, branch);
        String modelName = trimToNull(model.getModelName());
        if (!StringUtils.hasText(apiKey)) {
            throw new JeecgBootBizTipException("Prompt chat model apiKey is required, modelId=" + model.getId());
        }
        if (!StringUtils.hasText(baseUrl)) {
            throw new JeecgBootBizTipException("Prompt chat model baseUrl is required, modelId=" + model.getId());
        }
        if (!StringUtils.hasText(modelName)) {
            throw new JeecgBootBizTipException("Prompt chat modelName is required, modelId=" + model.getId());
        }

        JSONObject modelParams = parseModelParams(model);
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(positiveOrDefault(modelParams == null ? null : modelParams.getInteger("timeout"), 120)))
                .maxRetries(0)
                .returnThinking(false);
        if (modelParams != null) {
            applyOptionalModelParams(builder, modelParams);
        }
        return builder.build();
    }

    private void applyOptionalModelParams(OpenAiChatModel.OpenAiChatModelBuilder builder, JSONObject modelParams) {
        Double temperature = modelParams.getDouble("temperature");
        if (temperature != null) {
            builder.temperature(temperature);
        }
        Double topP = modelParams.getDouble("topP");
        if (topP != null) {
            builder.topP(topP);
        }
        Double presencePenalty = modelParams.getDouble("presencePenalty");
        if (presencePenalty != null) {
            builder.presencePenalty(presencePenalty);
        }
        Double frequencyPenalty = modelParams.getDouble("frequencyPenalty");
        if (frequencyPenalty != null) {
            builder.frequencyPenalty(frequencyPenalty);
        }
        Integer maxTokens = modelParams.getInteger("maxTokens");
        if (maxTokens != null && maxTokens > 0) {
            builder.maxTokens(maxTokens);
        }
    }
    private void applyOptionalRequestParams(OpenAiChatRequestParameters.Builder builder, JSONObject modelParams) {
        Double temperature = modelParams.getDouble("temperature");
        if (temperature != null) {
            builder.temperature(temperature);
        }
        Double topP = modelParams.getDouble("topP");
        if (topP != null) {
            builder.topP(topP);
        }
        Double presencePenalty = modelParams.getDouble("presencePenalty");
        if (presencePenalty != null) {
            builder.presencePenalty(presencePenalty);
        }
        Double frequencyPenalty = modelParams.getDouble("frequencyPenalty");
        if (frequencyPenalty != null) {
            builder.frequencyPenalty(frequencyPenalty);
        }
        Integer maxTokens = modelParams.getInteger("maxTokens");
        if (maxTokens != null && maxTokens > 0) {
            builder.maxOutputTokens(maxTokens);
        }
    }

    private Map<String, Object> buildSingleRoundCustomParameters(PromptProviderBranch branch) {
        Map<String, Object> customParameters = new LinkedHashMap<>();
        if (PromptProviderBranch.DEEPSEEK.equals(branch)) {
            Map<String, Object> thinking = new LinkedHashMap<>();
            thinking.put("type", "disabled");
            customParameters.put("thinking", thinking);
        }
        return customParameters;
    }

    private String extractApiKey(AiragModel model) {
        if (model == null || !StringUtils.hasText(model.getCredential())) {
            return null;
        }
        try {
            JSONObject credential = JSONObject.parseObject(model.getCredential());
            if (credential == null) {
                return null;
            }
            return trimToNull(credential.getString("apiKey"));
        } catch (Exception ex) {
            log.warn("extract prompt model apiKey failed: {}", ex.getMessage());
            return null;
        }
    }

    private JSONObject parseModelParams(AiragModel model) {
        if (model == null || !StringUtils.hasText(model.getModelParams())) {
            return null;
        }
        try {
            return JSONObject.parseObject(model.getModelParams());
        } catch (Exception ex) {
            log.warn("parse prompt model params failed: {}", ex.getMessage());
            return null;
        }
    }

    private String normalizeOpenAiBaseUrl(AiragModel model, PromptProviderBranch branch) {
        String baseUrl = trimToNull(model == null ? null : model.getBaseUrl());
        if (PromptProviderBranch.MINIMAX.equals(branch)) {
            String value = StringUtils.hasText(baseUrl) ? baseUrl : "https://api.minimax.io/v1";
            while (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            return value.endsWith("/v1") ? value : value + "/v1";
        }
        return baseUrl;
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
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
            JsonSchemaElement schemaElement = buildSchemaElement(parameters);
            if (!(schemaElement instanceof JsonObjectSchema objectSchema)) {
                return null;
            }

            ToolSpecification.Builder builder = ToolSpecification.builder()
                    .name(name)
                    .parameters(objectSchema);
            if (StringUtils.hasText(description)) {
                builder.description(description);
            }
            return builder.build();
        } catch (Exception ex) {
            log.warn("buildToolSpecification failed: {}", ex.getMessage());
            return null;
        }
    }

    private JsonSchemaElement buildSchemaElement(JSONObject schemaNode) {
        if (schemaNode == null) {
            return JsonStringSchema.builder().build();
        }
        String description = trimToNull(schemaNode.getString("description"));

        JsonSchemaElement enumElement = buildEnumElement(schemaNode, description);
        if (enumElement != null) {
            return enumElement;
        }

        JsonSchemaElement anyOfElement = buildAnyOfElement(schemaNode, description);
        if (anyOfElement != null) {
            return anyOfElement;
        }

        Object constValue = schemaNode.get("const");
        if (constValue != null) {
            JsonSchemaElement constElement = buildConstElement(constValue, description);
            if (constElement != null) {
                return constElement;
            }
        }

        String type = trimToNull(schemaNode.getString("type"));
        if (!StringUtils.hasText(type)) {
            return JsonStringSchema.builder()
                    .description(description)
                    .build();
        }

        return switch (type.toLowerCase(Locale.ROOT)) {
            case "object" -> buildObjectSchema(schemaNode, description);
            case "array" -> buildArraySchema(schemaNode, description);
            case "integer" -> JsonIntegerSchema.builder().description(description).build();
            case "number" -> JsonNumberSchema.builder().description(description).build();
            case "boolean" -> JsonBooleanSchema.builder().description(description).build();
            case "string" -> JsonStringSchema.builder().description(description).build();
            default -> JsonStringSchema.builder().description(description).build();
        };
    }

    private JsonObjectSchema buildObjectSchema(JSONObject schemaNode, String description) {
        JsonObjectSchema.Builder builder = JsonObjectSchema.builder()
                .description(description);
        if (schemaNode.containsKey("additionalProperties")) {
            builder.additionalProperties(schemaNode.getBoolean("additionalProperties"));
        }

        JSONObject properties = schemaNode.getJSONObject("properties");
        if (properties != null) {
            for (String propName : properties.keySet()) {
                if (!StringUtils.hasText(propName)) {
                    continue;
                }
                JSONObject propSchema = properties.getJSONObject(propName);
                builder.addProperty(propName, buildSchemaElement(propSchema));
            }
        }

        List<String> requiredList = parseRequiredList(schemaNode.getJSONArray("required"));
        if (!requiredList.isEmpty()) {
            builder.required(requiredList);
        }

        JSONObject definitions = schemaNode.getJSONObject("definitions");
        if (definitions != null && !definitions.isEmpty()) {
            Map<String, JsonSchemaElement> definitionMap = new LinkedHashMap<>();
            for (String key : definitions.keySet()) {
                if (!StringUtils.hasText(key)) {
                    continue;
                }
                definitionMap.put(key, buildSchemaElement(definitions.getJSONObject(key)));
            }
            if (!definitionMap.isEmpty()) {
                builder.definitions(definitionMap);
            }
        }

        return builder.build();
    }

    private JsonArraySchema buildArraySchema(JSONObject schemaNode, String description) {
        JsonArraySchema.Builder builder = JsonArraySchema.builder()
                .description(description);
        JSONObject items = schemaNode.getJSONObject("items");
        if (items != null) {
            builder.items(buildSchemaElement(items));
        }
        return builder.build();
    }

    private JsonSchemaElement buildEnumElement(JSONObject schemaNode, String description) {
        JSONArray enumValues = schemaNode.getJSONArray("enum");
        if (enumValues == null || enumValues.isEmpty()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (Object item : enumValues) {
            if (item == null) {
                continue;
            }
            values.add(String.valueOf(item));
        }
        if (values.isEmpty()) {
            return null;
        }
        return JsonEnumSchema.builder()
                .description(description)
                .enumValues(values)
                .build();
    }

    private JsonSchemaElement buildAnyOfElement(JSONObject schemaNode, String description) {
        JSONArray oneOf = schemaNode.getJSONArray("oneOf");
        if (oneOf == null || oneOf.isEmpty()) {
            return null;
        }

        List<String> constStrings = new ArrayList<>();
        boolean allConstStrings = true;
        List<JsonSchemaElement> schemaElements = new ArrayList<>();
        for (Object item : oneOf) {
            if (!(item instanceof JSONObject child)) {
                allConstStrings = false;
                continue;
            }
            Object constValue = child.get("const");
            if (constValue instanceof String value) {
                constStrings.add(value);
            } else {
                allConstStrings = false;
            }
            schemaElements.add(buildSchemaElement(child));
        }

        if (allConstStrings && !constStrings.isEmpty()) {
            return JsonEnumSchema.builder()
                    .description(description)
                    .enumValues(constStrings)
                    .build();
        }
        if (schemaElements.isEmpty()) {
            return null;
        }
        return JsonAnyOfSchema.builder()
                .description(description)
                .anyOf(schemaElements)
                .build();
    }

    private JsonSchemaElement buildConstElement(Object constValue, String description) {
        if (constValue instanceof Boolean) {
            return JsonBooleanSchema.builder().description(description).build();
        }
        if (constValue instanceof Byte || constValue instanceof Short || constValue instanceof Integer || constValue instanceof Long) {
            return JsonIntegerSchema.builder().description(description).build();
        }
        if (constValue instanceof Number) {
            return JsonNumberSchema.builder().description(description).build();
        }
        if (constValue instanceof String value) {
            return JsonEnumSchema.builder()
                    .description(description)
                    .enumValues(value)
                    .build();
        }
        return null;
    }

    private List<String> parseRequiredList(JSONArray required) {
        List<String> requiredList = new ArrayList<>();
        if (required == null || required.isEmpty()) {
            return requiredList;
        }
        for (Object item : required) {
            if (item == null) {
                continue;
            }
            String key = trimToNull(String.valueOf(item));
            if (StringUtils.hasText(key)) {
                requiredList.add(key);
            }
        }
        return requiredList;
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

    private void logLlmRequest(AiragModel model,
                               PromptProviderBranch branch,
                               String developerPrompt,
                               String userPrompt,
                               String toolSchema,
                               String toolChoiceName,
                               AIChatParams params,
                               boolean withTools) {
        tsAiLogCollector.markModel(trimToNull(model.getProvider()), trimToNull(model.getModelName()), trimToNull(model.getId()));
        tsAiLogCollector.appendStep("llm_request", "模型请求", "success", step -> {
            step.setProvider(trimToNull(model.getProvider()));
            step.setModelName(trimToNull(model.getModelName()));
            step.setModelId(trimToNull(model.getId()));
            step.setDeveloperPrompt(trimToNull(developerPrompt));
            step.setUserPrompt(trimToNull(userPrompt));
            step.setToolSchema(trimToNull(toolSchema));
            JSONObject payload = new JSONObject();
            payload.put("provider", trimToNull(model.getProvider()));
            payload.put("modelId", trimToNull(model.getId()));
            payload.put("modelName", trimToNull(model.getModelName()));
            payload.put("branch", branch.name());
            payload.put("noThinking", params == null ? null : params.getNoThinking());
            payload.put("returnThinking", params == null ? null : params.getReturnThinking());
            payload.put("withTools", withTools);
            payload.put("toolSchemaProvided", StringUtils.hasText(toolSchema));
            payload.put("toolChoice", trimToNull(toolChoiceName));
            payload.put("toolChoiceApplied", StringUtils.hasText(toolChoiceName) && withTools);
            step.setRequestPayloadJson(payload.toJSONString());
        });
    }

    private void logLlmResponse(AiragModel model, String content) {
        tsAiLogCollector.appendStep("llm_response", "模型返回", StringUtils.hasText(content) ? "success" : "failed", step -> {
            step.setProvider(trimToNull(model.getProvider()));
            step.setModelName(trimToNull(model.getModelName()));
            step.setModelId(trimToNull(model.getId()));
            step.setResponseRaw(trimToNull(content));
        });
    }

    private enum PromptProviderBranch {
        DEEPSEEK,
        MINIMAX,
        GEMINI,
        OPENAI_COMPATIBLE
    }
}
