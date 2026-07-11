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
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.agent.runtime.DeepAgentsProperties;
import org.jeecg.modules.airag.agent.skill.runtime.SkillProperties;
import org.jeecg.modules.airag.agent.tool.DeepAgentTaskToolService;
import org.jeecg.modules.airag.agent.skill.model.SkillLoadResult;
import org.jeecg.modules.airag.agent.skill.runtime.SkillRuntimeService;
import org.jeecg.modules.airag.agent.skill.tool.SkillTools;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;

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
     * DeepAgents task 工具。
     */
    @Autowired(required = false)
    private DeepAgentTaskToolService deepAgentTaskToolService;
    /**
     * DeepAgents 配置。
     */
    @Autowired(required = false)
    private DeepAgentsProperties deepAgentsProperties;

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
            context.putAttribute("llmNodeTools", this.definition == null ? null : this.definition.getTools());
            context.putAttribute("llmNodePermissions", this.definition == null ? null : this.definition.getPermissions());
            context.putAttribute("llmNodeResponseFormat", this.definition == null ? null : this.definition.getResponseFormat());
        }
        SkillLoadResult skillLoadResult = prepareSkillLoadResult(context);
        List<ChatMessage> messages = buildMessages(promptVariables, skillLoadResult, context);
        String modelId = this.modelResolver.resolveTextModelId(context.getAppId());
        AIChatParams params = buildChatParams(context, skillLoadResult);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<String> textRef = new AtomicReference<>("");
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
            throw new RuntimeException(errorRef.get());
        }
        String finalText = textRef.get();
        if (oConvertUtils.isEmpty(finalText)) {
            finalText = this.eventPublisher.readBuffer(this.eventPublisher.buildLlmBufferKey(context, nodeName()));
        }
        context.setLatestContent(finalText);
        return parseResult(finalText, context);
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
        if (this.skillTools != null && skillLoadResult != null && skillLoadResult.getActivation() != null) {
            Map<dev.langchain4j.agent.tool.ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> skillToolsMap =
                    this.skillTools.buildToolMap(skillLoadResult.getActivation());
            if (skillToolsMap != null && !skillToolsMap.isEmpty()) {
                if (params.getTools() == null) {
                    params.setTools(new LinkedHashMap<>());
                }
                params.getTools().putAll(skillToolsMap);
            }
        }
        if (this.deepAgentTaskToolService != null) {
            Map<dev.langchain4j.agent.tool.ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> taskTools =
                    this.deepAgentTaskToolService.buildToolMap(context);
            if (taskTools != null && !taskTools.isEmpty()) {
                if (params.getTools() == null) {
                    params.setTools(new LinkedHashMap<>());
                }
                params.getTools().putAll(taskTools);
            }
        }
        if (skillLoadResult != null) {
            context.putAttribute("skillLoadResult", skillLoadResult);
            context.putAttribute("skillActivation", skillLoadResult.getActivation());
            context.putAttribute("skillRootDir", this.skillProperties == null ? null : this.skillProperties.getRootDir());
        }
        return params;
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
        String nodeDirectivePrompt = buildNodeDirectivePrompt();
        String developerPrompt = renderDeveloperPrompt(promptVariables);
        String deepAgentsPrompt = org.jeecg.modules.airag.agent.runtime.DeepAgentsPromptSupport.buildBasePrompt(context, skillLoadResult);
        if (oConvertUtils.isNotEmpty(nodeDirectivePrompt)) {
            if (oConvertUtils.isNotEmpty(developerPrompt)) {
                developerPrompt = nodeDirectivePrompt + "\n\n" + developerPrompt;
            } else {
                developerPrompt = nodeDirectivePrompt;
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
            if (oConvertUtils.isNotEmpty(skillIndexPrompt)) {
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
     * 渲染节点级约束说明。
     *
     * @return 节点级约束说明
     */
    protected String buildNodeDirectivePrompt() {
        if (this.definition == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendDirectiveLine(sb, "node_name", this.definition.getName());
        appendDirectiveLine(sb, "node_description", this.definition.getDescription());
        appendDirectiveLine(sb, "skill_domain", this.definition.getSkillDomain());
        appendDirectiveLine(sb, "skill_top_k", this.definition.getSkillTopK() == null ? null : String.valueOf(this.definition.getSkillTopK()));
        appendDirectiveLine(sb, "node_skills", joinList(this.definition.getSkills()));
        appendDirectiveLine(sb, "node_tools", joinList(this.definition.getTools()));
        appendDirectiveLine(sb, "node_permissions", joinList(this.definition.getPermissions()));
        appendDirectiveLine(sb, "response_format", this.definition.getResponseFormat());
        appendDirectiveLine(sb, "input_constraints", this.definition.getInputConstraints());
        appendDirectiveLine(sb, "output_constraints", this.definition.getOutputConstraints());
        appendDirectiveLine(sb, "next_step_condition", this.definition.getNextStepCondition());
        if (this.definition.getMetadata() != null && !this.definition.getMetadata().isEmpty()) {
            appendDirectiveLine(sb, "metadata", JSON.toJSONString(this.definition.getMetadata()));
        }
        return sb.toString().trim();
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

    /**
     * 将列表合并为文本。
     *
     * @param values 列表
     * @return 文本
     */
    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(", ", values);
    }

    /**
     * 追加节点约束行。
     *
     * @param sb 字符串构造器
     * @param key 键
     * @param value 值
     */
    private void appendDirectiveLine(StringBuilder sb, String key, String value) {
        if (!org.springframework.util.StringUtils.hasText(value)) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append("- ").append(key).append(": ").append(value);
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
