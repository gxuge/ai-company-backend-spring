package org.jeecg.modules.airag.agent.node;

import com.alibaba.fastjson2.JSON;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.service.TokenStream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.agent.runtime.DeepAgentsProperties;
import org.jeecg.modules.airag.agent.skill.runtime.SkillProperties;
import org.jeecg.modules.airag.agent.skill.model.SkillLoadResult;
import org.jeecg.modules.airag.agent.skill.runtime.SkillRuntimeService;
import org.jeecg.modules.airag.agent.skill.tool.SkillTools;
import org.jeecg.modules.airag.agent.tool.control.AgentControlToolService;
import org.jeecg.modules.airag.agent.trace.AgentLlmTraceRequest;
import org.jeecg.modules.airag.agent.trace.AgentLlmTraceResponse;
import org.jeecg.modules.airag.agent.trace.AgentLlmTraceSink;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LLM 节点基类。
 *
 * @author codex
 * @date 2026/6/16
 */
@Slf4j
@Getter
public abstract class LlmNode extends BaseAgentNode {
    /**
     * LLM 节点定义。
     */
    private final LlmNodeDefinition definition;
    /**
     * 模板服务。
     */
    private final IAiragPromptTemplateService promptTemplateService;
    /**
     * 模型解析器。
     */
    private final AgentModelResolver modelResolver;
    /**
     * 大模型处理器。
     */
    private final IAIChatHandler aiChatHandler;
    /**
     * 事件发布器。
     */
    private final AgentEventPublisher eventPublisher;
    /**
     * Skill 运行时服务。
     */
    @Autowired(required = false)
    private SkillRuntimeService skillRuntimeService;
    /**
     * Skill 配置。
     */
    @Autowired(required = false)
    private SkillProperties skillProperties;
    /**
     * Skill 工具。
     */
    @Autowired(required = false)
    private SkillTools skillTools;
    /**
     * DeepAgents 配置。
     */
    @Autowired(required = false)
    private DeepAgentsProperties deepAgentsProperties;
    /**
     * Agent 公共控制工具。
     */
    @Autowired(required = false)
    private AgentControlToolService agentControlToolService;
    /**
     * LLM trace sinks.
     */
    @Autowired(required = false)
    private List<AgentLlmTraceSink> llmTraceSinks;
    /**
     * 构造函数。
     *
     * @param nodeName 节点名
     * @param displayName 展示名
     * @param definition 节点定义
     * @param promptTemplateService 模板服务
     * @param modelResolver 模型解析器
     * @param aiChatHandler 大模型处理器
     * @param eventPublisher 事件发布器
     */
    protected LlmNode(String nodeName,
                      String displayName,
                      LlmNodeDefinition definition,
                      IAiragPromptTemplateService promptTemplateService,
                      AgentModelResolver modelResolver,
                      IAIChatHandler aiChatHandler,
                      AgentEventPublisher eventPublisher) {
        super(nodeName, displayName, NodeKind.LLM);
        this.definition = definition == null ? new LlmNodeDefinition() : definition;
        this.promptTemplateService = promptTemplateService;
        this.modelResolver = modelResolver;
        this.aiChatHandler = aiChatHandler;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public NodeResult execute(AgentContext context) throws Exception {
        Map<String, String> promptVariables = buildPromptVariables(context);
        if (context != null) {
            context.putAttribute("llmNodeDefinition", this.definition == null ? null : this.definition.toMap());
            context.putAttribute("llmNodeSkills", this.definition == null ? null : this.definition.getSkills());
            context.putAttribute("llmNodeTools", buildEffectiveNodeTools(context));
            context.putAttribute("llmNodePermissions", buildEffectiveNodePermissions(context));
            context.putAttribute("llmNodeResponseFormat", this.definition == null ? null : this.definition.getResponseFormat());
        }
        SkillLoadResult skillLoadResult = prepareSkillLoadResult(context);
        List<ChatMessage> messages = buildMessages(promptVariables, skillLoadResult, context);
        String modelId = this.modelResolver.resolveTextModelId(context.getAppId());
        AIChatParams params = buildChatParams(context, skillLoadResult);
        traceLlmRequest(context, modelId, messages, params);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<String> textRef = new AtomicReference<>("");
        AtomicReference<String> finishReasonRef = new AtomicReference<>();
        AtomicBoolean terminalReceived = new AtomicBoolean(false);
        TokenStream tokenStream = this.aiChatHandler.chat(modelId, messages, params);

        tokenStream.onPartialResponse(delta -> {
            if (terminalReceived.get()) {
                return;
            }
            if (!shouldPublishPartialResponse()) {
                return;
            }
            String safeDelta = delta == null ? "" : delta;
            this.eventPublisher.publishLlmDelta(context, nodeName(), safeDelta);
        }).onCompleteResponse(response -> {
            if (!terminalReceived.compareAndSet(false, true)) {
                return;
            }
            String finalText = response == null || response.aiMessage() == null ? "" : response.aiMessage().text();
            FinishReason finishReason = response == null ? null : response.finishReason();
            finishReasonRef.set(finishReason == null ? null : finishReason.name());
            if (FinishReason.STOP.equals(finishReason) || finishReason == null) {
                textRef.set(finalText);
            } else {
                errorRef.set(new RuntimeException(finalText));
            }
            done.countDown();
        }).onError(error -> {
            if (!terminalReceived.compareAndSet(false, true)) {
                return;
            }
            errorRef.set(error);
            done.countDown();
        }).start();

        done.await(300, TimeUnit.SECONDS);
        if (errorRef.get() != null) {
            traceLlmResponse(context, modelId, null, finishReasonRef.get(), false, errorRef.get());
            throw new RuntimeException(errorRef.get());
        }
        String finalText = textRef.get();
        if (oConvertUtils.isEmpty(finalText)) {
            finalText = this.eventPublisher.readBuffer(this.eventPublisher.buildLlmBufferKey(context, nodeName()));
        }
        traceLlmResponse(context, modelId, finalText, finishReasonRef.get(), true, null);
        context.setLatestContent(finalText);
        NodeResult nodeResult = parseResult(finalText, context);
        AgentHandoffSupport.attachToNodeResult(nodeResult, context);
        return nodeResult;
    }

    /**
     * 构造提示词变量。
     *
     * @param context 运行上下文
     * @return 变量集合
     */
    protected abstract Map<String, String> buildPromptVariables(AgentContext context);

    /**
     * 解析模型输出结果。
     *
     * @param finalText 完整输出文本
     * @param context 运行上下文
     * @return 节点结果
     */
    protected abstract NodeResult parseResult(String finalText, AgentContext context);

    /**
     * 是否需要把 LLM 增量响应实时推送到前端。
     *
     * @return true 表示推送增量；false 表示仅保留最终结果
     */
    protected boolean shouldPublishPartialResponse() {
        return true;
    }

    /**
     * 构造聊天参数。
     *
     * @param context 运行上下文
     * @return 聊天参数
     */
    protected AIChatParams buildChatParams(AgentContext context) {
        AIChatParams params = new AIChatParams();
        params.setTemperature(0.7);
        return params;
    }

    /**
     * 构造聊天参数，并注入 Skill 工具。
     *
     * @param context 运行上下文
     * @param skillLoadResult Skill 准备结果
     * @return 聊天参数
     */
    protected AIChatParams buildChatParams(AgentContext context, SkillLoadResult skillLoadResult) {
        AIChatParams params = buildChatParams(context);
        if (params == null) {
            params = new AIChatParams();
        }
        if (!hasPreloadedNodeSkills(context) && this.skillTools != null && skillLoadResult != null && skillLoadResult.getActivation() != null) {
            Map<dev.langchain4j.agent.tool.ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> skillToolsMap =
                    this.skillTools.buildToolMap(skillLoadResult.getActivation());
            if (skillToolsMap != null && !skillToolsMap.isEmpty()) {
                if (params.getTools() == null) {
                    params.setTools(new LinkedHashMap<>());
                }
                params.getTools().putAll(skillToolsMap);
            }
        }
        if (skillLoadResult != null) {
            context.putAttribute("skillLoadResult", skillLoadResult);
            context.putAttribute("skillActivation", skillLoadResult.getActivation());
            context.putAttribute("skillRootDir", this.skillProperties == null ? null : this.skillProperties.getRootDir());
        }
        if (this.agentControlToolService != null) {
            Map<dev.langchain4j.agent.tool.ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> controlToolsMap =
                    this.agentControlToolService.buildToolMap(context);
            if (controlToolsMap != null && !controlToolsMap.isEmpty()) {
                if (params.getTools() == null) {
                    params.setTools(new LinkedHashMap<>());
                }
                params.getTools().putAll(controlToolsMap);
            }
        }
        return params;
    }

    private boolean hasPreloadedNodeSkills(AgentContext context) {
        Object value = context == null ? null : context.getAttribute("loadedNodeSkillCodes");
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return false;
    }

    /**
     * 组装对话消息。
     *
     * @param promptVariables 提示词变量
     * @return 消息列表
     */
    protected List<ChatMessage> buildMessages(Map<String, String> promptVariables) {
        return buildMessages(promptVariables, null, null);
    }

    /**
     * 组装对话消息。
     *
     * @param promptVariables 提示词变量
     * @param skillLoadResult Skill 准备结果
     * @return 消息列表
     */
    protected List<ChatMessage> buildMessages(Map<String, String> promptVariables, SkillLoadResult skillLoadResult) {
        return buildMessages(promptVariables, skillLoadResult, null);
    }

    /**
     * 组装对话消息。
     *
     * @param promptVariables 提示词变量
     * @param skillLoadResult Skill 准备结果
     * @param context 运行上下文
     * @return 消息列表
     */
    protected List<ChatMessage> buildMessages(Map<String, String> promptVariables,
                                              SkillLoadResult skillLoadResult,
                                              AgentContext context) {
        String developerPrompt = renderDeveloperPrompt(promptVariables);
        String nodeSkillPrompt = buildNodeSkillPrompt(context);
        String deepAgentsPrompt = org.jeecg.modules.airag.agent.runtime.DeepAgentsPromptSupport.buildBasePrompt(context, skillLoadResult);
        String controlPrompt = this.agentControlToolService == null ? "" : this.agentControlToolService.buildControlPrompt(context);
        if (oConvertUtils.isNotEmpty(controlPrompt)) {
            if (oConvertUtils.isNotEmpty(developerPrompt)) {
                developerPrompt = controlPrompt + "\n\n" + developerPrompt;
            } else {
                developerPrompt = controlPrompt;
            }
        }
        if (oConvertUtils.isNotEmpty(nodeSkillPrompt)) {
            if (oConvertUtils.isNotEmpty(developerPrompt)) {
                developerPrompt = developerPrompt + "\n\n" + nodeSkillPrompt;
            } else {
                developerPrompt = nodeSkillPrompt;
            }
        }
        if (oConvertUtils.isNotEmpty(deepAgentsPrompt)) {
            if (oConvertUtils.isNotEmpty(developerPrompt)) {
                developerPrompt = deepAgentsPrompt + "\n\n" + developerPrompt;
            } else {
                developerPrompt = deepAgentsPrompt;
            }
        } else {
            String skillIndexPrompt = skillLoadResult == null ? "" : skillLoadResult.getSkillIndexPrompt();
            if (oConvertUtils.isEmpty(nodeSkillPrompt) && oConvertUtils.isNotEmpty(skillIndexPrompt)) {
                if (oConvertUtils.isNotEmpty(developerPrompt)) {
                    developerPrompt = developerPrompt + "\n\n" + skillIndexPrompt;
                } else {
                    developerPrompt = skillIndexPrompt;
                }
            }
        }
        String userPrompt = renderUserPrompt(promptVariables);
        List<ChatMessage> messages = new ArrayList<>();
        if (oConvertUtils.isNotEmpty(developerPrompt)) {
            messages.add(new SystemMessage(developerPrompt));
        }
        messages.add(new UserMessage(userPrompt));
        return messages;
    }

    /**
     * Trace LLM request.
     */
    private void traceLlmRequest(AgentContext context, String modelId, List<ChatMessage> messages, AIChatParams params) {
        if (this.llmTraceSinks == null || this.llmTraceSinks.isEmpty()) {
            return;
        }
        AgentLlmTraceRequest request = new AgentLlmTraceRequest();
        request.setContext(context);
        request.setNodeName(nodeName());
        request.setPromptCode(getPromptCode());
        request.setPromptVersion(getPromptVersion());
        request.setModelId(modelId);
        request.setDeveloperPrompt(extractMessageText(messages, "SYSTEM"));
        request.setUserPrompt(extractMessageText(messages, "USER"));
        request.setRenderedPrompt(buildRenderedPromptForTrace(request.getDeveloperPrompt(), request.getUserPrompt()));
        request.setToolSchema(buildToolSchemaForTrace(params));
        request.setRequestPayload(buildRequestPayloadForTrace(modelId, messages, params));
        for (AgentLlmTraceSink sink : this.llmTraceSinks) {
            try {
                sink.onRequest(request);
            } catch (Exception ex) {
                log.debug("Agent LLM request trace failed, nodeName={}", nodeName(), ex);
            }
        }
    }

    /**
     * Trace LLM response.
     */
    private void traceLlmResponse(AgentContext context,
                                  String modelId,
                                  String responseRaw,
                                  String finishReason,
                                  boolean success,
                                  Throwable error) {
        if (this.llmTraceSinks == null || this.llmTraceSinks.isEmpty()) {
            return;
        }
        AgentLlmTraceResponse response = new AgentLlmTraceResponse();
        response.setContext(context);
        response.setNodeName(nodeName());
        response.setPromptCode(getPromptCode());
        response.setPromptVersion(getPromptVersion());
        response.setModelId(modelId);
        response.setResponseRaw(responseRaw);
        response.setFinishReason(finishReason);
        response.setSuccess(success);
        response.setErrorMessage(error == null ? null : error.getMessage());
        Map<String, Object> extraInfo = new LinkedHashMap<>();
        extraInfo.put("finishReason", finishReason);
        extraInfo.put("nodeName", nodeName());
        response.setExtraInfo(extraInfo);
        for (AgentLlmTraceSink sink : this.llmTraceSinks) {
            try {
                sink.onResponse(response);
            } catch (Exception ex) {
                log.debug("Agent LLM response trace failed, nodeName={}", nodeName(), ex);
            }
        }
    }

    private String extractMessageText(List<ChatMessage> messages, String role) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage message : messages) {
            if (message == null || !role.equalsIgnoreCase(String.valueOf(message.type()))) {
                continue;
            }
            String text = invokeMessageText(message);
            if (oConvertUtils.isEmpty(text)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(text);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String invokeMessageText(ChatMessage message) {
        Object value = invokeGetterMethod(message, "text");
        if (value == null) {
            value = invokeGetterMethod(message, "singleText");
        }
        return value == null ? null : String.valueOf(value);
    }

    private String buildRenderedPromptForTrace(String developerPrompt, String userPrompt) {
        StringBuilder sb = new StringBuilder();
        if (oConvertUtils.isNotEmpty(developerPrompt)) {
            sb.append(developerPrompt);
        }
        if (oConvertUtils.isNotEmpty(userPrompt)) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(userPrompt);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private Map<String, Object> buildRequestPayloadForTrace(String modelId, List<ChatMessage> messages, AIChatParams params) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("modelId", modelId);
        payload.put("nodeName", nodeName());
        payload.put("messageCount", messages == null ? 0 : messages.size());
        payload.put("messageRoles", buildMessageRoles(messages));
        payload.put("params", snapshotParams(params));
        return payload;
    }

    private List<String> buildMessageRoles(List<ChatMessage> messages) {
        List<String> roles = new ArrayList<>();
        if (messages == null) {
            return roles;
        }
        for (ChatMessage message : messages) {
            roles.add(message == null ? null : String.valueOf(message.type()));
        }
        return roles;
    }

    private Map<String, Object> snapshotParams(AIChatParams params) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (params == null) {
            return snapshot;
        }
        String[] fields = new String[]{
                "provider", "modelName", "baseUrl",
                "temperature", "topP", "presencePenalty", "frequencyPenalty",
                "maxTokens", "timeout", "enableSearch",
                "noThinking", "returnThinking", "reasoningEffort",
                "pluginIds", "knowIds"
        };
        for (String field : fields) {
            snapshot.put(field, invokeGetter(params, field));
        }
        snapshot.put("tools", buildToolNames(params));
        return snapshot;
    }

    private List<String> buildToolNames(AIChatParams params) {
        Object tools = invokeGetter(params, "tools");
        List<String> names = new ArrayList<>();
        if (tools instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                Object name = invokeGetterMethod(key, "name");
                names.add(name == null ? String.valueOf(key) : String.valueOf(name));
            }
        }
        return names;
    }

    private String buildToolSchemaForTrace(AIChatParams params) {
        Object tools = invokeGetter(params, "tools");
        if (tools == null) {
            return null;
        }
        try {
            return JSON.toJSONString(buildToolNames(params));
        } catch (Exception ex) {
            return String.valueOf(tools);
        }
    }

    private Object invokeGetter(Object target, String field) {
        if (target == null || oConvertUtils.isEmpty(field)) {
            return null;
        }
        String methodName = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
        return invokeGetterMethod(target, methodName);
    }

    private Object invokeGetterMethod(Object target, String methodName) {
        if (target == null || oConvertUtils.isEmpty(methodName)) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 渲染系统提示词。
     *
     * @param promptVariables 变量集合
     * @return 系统提示词
     */
    protected String renderDeveloperPrompt(Map<String, String> promptVariables) {
        if (oConvertUtils.isNotEmpty(getPromptCode()) && oConvertUtils.isNotEmpty(getPromptVersion())) {
            return this.promptTemplateService.renderSection(getPromptCode(), getPromptVersion(), "developer_prompt", promptVariables);
        }
        return replaceVariables(getSystemPromptTemplate(), promptVariables);
    }

    /**
     * 渲染用户提示词。
     *
     * @param promptVariables 变量集合
     * @return 用户提示词
     */
    protected String renderUserPrompt(Map<String, String> promptVariables) {
        if (oConvertUtils.isNotEmpty(getPromptCode()) && oConvertUtils.isNotEmpty(getPromptVersion())) {
            return this.promptTemplateService.renderSection(getPromptCode(), getPromptVersion(), "user_prompt_template", promptVariables);
        }
        return replaceVariables(getUserPromptTemplate(), promptVariables);
    }

    /**
     * 简单替换 {{key}} 占位符。
     *
     * @param template 模板文本
     * @param variables 变量集合
     * @return 渲染结果
     */
    protected String replaceVariables(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        String rendered = template;
        if (variables == null) {
            return rendered;
        }
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }

    /**
     * 尝试将文本解析为 JSON 对象。
     *
     * @param text 输出文本
     * @return JSON 对象
     */
    protected Map<String, Object> parseJsonObject(String text) {
        try {
            return JSON.parseObject(text, LinkedHashMap.class);
        } catch (Exception ex) {
            log.warn("LLM结果不是合法JSON，nodeName={}", nodeName(), ex);
            return new LinkedHashMap<>();
        }
    }

    /**
     * 准备 Skill 上下文。
     *
     * @param context 运行上下文
     * @return Skill 准备结果
     */
    protected SkillLoadResult prepareSkillLoadResult(AgentContext context) {
        if (this.skillRuntimeService == null || context == null) {
            return null;
        }
        if (hasPreloadedNodeSkills(context)) {
            return null;
        }
        boolean deepAgentsMode = org.jeecg.modules.airag.agent.runtime.DeepAgentsPromptSupport.isEnabled(context);
        String skillDomain = oConvertUtils.getString(context.getAttribute("skillDomain"));
        if (!org.springframework.util.StringUtils.hasText(skillDomain) && this.definition != null) {
            skillDomain = oConvertUtils.getString(this.definition.getSkillDomain());
        }
        if (!org.springframework.util.StringUtils.hasText(skillDomain) && deepAgentsMode && this.deepAgentsProperties != null) {
            skillDomain = oConvertUtils.getString(this.deepAgentsProperties.getDefaultSkillDomain());
        }
        if (!org.springframework.util.StringUtils.hasText(skillDomain) && !deepAgentsMode) {
            return null;
        }
        Integer topK = resolveSkillTopK(context.getAttribute("skillTopK"));
        if (topK == null && this.definition != null && this.definition.getSkillTopK() != null) {
            topK = this.definition.getSkillTopK();
        }
        if (topK == null && deepAgentsMode && this.deepAgentsProperties != null) {
            topK = this.deepAgentsProperties.getDefaultSkillTopK();
        }
        SkillLoadResult loadResult = this.skillRuntimeService.prepare(context.getUserInput(), skillDomain, topK == null ? 3 : topK);
        return loadResult;
    }

    /**
     * 构建当前节点显式绑定的完整 Skill 提示词。
     *
     * @param context 运行上下文
     * @return Skill 正文提示词
     */
    protected String buildNodeSkillPrompt(AgentContext context) {
        return oConvertUtils.getString(context == null ? null : context.getAttribute("nodeSkillPrompt"));
    }

    /**
     * 构建当前上下文下的有效工具列表。
     *
     * @param context 运行上下文
     * @return 工具列表
     */
    protected List<String> buildEffectiveNodeTools(AgentContext context) {
        List<String> tools = new ArrayList<>();
        appendUnique(tools, this.definition == null ? null : this.definition.getTools());
        if (this.agentControlToolService != null && this.agentControlToolService.isEnabled(context)) {
            appendUnique(tools, AgentControlToolService.TOOL_HANDOFF_TO_MAIN);
        }
        return tools;
    }

    /**
     * 构建当前上下文下的有效权限列表。
     *
     * @param context 运行上下文
     * @return 权限列表
     */
    protected List<String> buildEffectiveNodePermissions(AgentContext context) {
        List<String> permissions = new ArrayList<>();
        appendUnique(permissions, this.definition == null ? null : this.definition.getPermissions());
        if (this.agentControlToolService != null && this.agentControlToolService.isEnabled(context)) {
            appendUnique(permissions, AgentControlToolService.TOOL_HANDOFF_TO_MAIN);
        }
        return permissions;
    }

    private void appendUnique(List<String> target, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (String value : values) {
            appendUnique(target, value);
        }
    }

    private void appendUnique(List<String> target, String value) {
        if (!org.springframework.util.StringUtils.hasText(value) || target.contains(value)) {
            return;
        }
        target.add(value);
    }

    /**
     * 返回节点定义。
     *
     * @return 节点定义
     */
    public LlmNodeDefinition getDefinition() {
        return this.definition;
    }

    /**
     * 返回提示词编码。
     *
     * @return 提示词编码
     */
    public String getPromptCode() {
        return this.definition == null ? null : this.definition.getPromptCode();
    }

    /**
     * 返回提示词版本。
     *
     * @return 提示词版本
     */
    public String getPromptVersion() {
        return this.definition == null ? null : this.definition.getPromptVersion();
    }

    /**
     * 返回系统提示词模板。
     *
     * @return 系统提示词模板
     */
    public String getSystemPromptTemplate() {
        return this.definition == null ? null : this.definition.getSystemPromptTemplate();
    }

    /**
     * 返回用户提示词模板。
     *
     * @return 用户提示词模板
     */
    public String getUserPromptTemplate() {
        return this.definition == null ? null : this.definition.getUserPromptTemplate();
    }

    private Integer resolveSkillTopK(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            String text = String.valueOf(value).trim();
            if (!org.springframework.util.StringUtils.hasText(text)) {
                return null;
            }
            return Integer.parseInt(text);
        } catch (Exception ex) {
            return null;
        }
    }
}
