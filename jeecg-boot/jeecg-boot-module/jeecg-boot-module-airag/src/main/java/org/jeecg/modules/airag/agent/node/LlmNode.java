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
import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;

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
     * 提示词编码。
     */
    private final String promptCode;
    /**
     * 提示词版本。
     */
    private final String promptVersion;
    /**
     * 原始系统提示词模板。
     */
    private final String systemPromptTemplate;
    /**
     * 原始用户提示词模板。
     */
    private final String userPromptTemplate;
    /**
     * 工具结构定义。
     */
    private final String toolSchema;
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
     * 构造函数。
     *
     * @param nodeName 节点名
     * @param displayName 展示名
     * @param promptCode 模板编码
     * @param promptVersion 模板版本
     * @param systemPromptTemplate 系统提示词模板
     * @param userPromptTemplate 用户提示词模板
     * @param toolSchema 工具结构
     * @param promptTemplateService 模板服务
     * @param modelResolver 模型解析器
     * @param aiChatHandler 大模型处理器
     * @param eventPublisher 事件发布器
     */
    protected LlmNode(String nodeName,
                      String displayName,
                      String promptCode,
                      String promptVersion,
                      String systemPromptTemplate,
                      String userPromptTemplate,
                      String toolSchema,
                      IAiragPromptTemplateService promptTemplateService,
                      AgentModelResolver modelResolver,
                      IAIChatHandler aiChatHandler,
                      AgentEventPublisher eventPublisher) {
        super(nodeName, displayName, NodeKind.LLM);
        this.promptCode = promptCode;
        this.promptVersion = promptVersion;
        this.systemPromptTemplate = systemPromptTemplate;
        this.userPromptTemplate = userPromptTemplate;
        this.toolSchema = toolSchema;
        this.promptTemplateService = promptTemplateService;
        this.modelResolver = modelResolver;
        this.aiChatHandler = aiChatHandler;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public NodeResult execute(AgentContext context) throws Exception {
        Map<String, String> promptVariables = buildPromptVariables(context);
        List<ChatMessage> messages = buildMessages(promptVariables);
        String modelId = this.modelResolver.resolveTextModelId(context.getAppId());
        AIChatParams params = buildChatParams(context);
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
     * 组装对话消息。
     *
     * @param promptVariables 提示词变量
     * @return 消息列表
     */
    protected List<ChatMessage> buildMessages(Map<String, String> promptVariables) {
        String developerPrompt = renderDeveloperPrompt(promptVariables);
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
        if (oConvertUtils.isNotEmpty(this.promptCode) && oConvertUtils.isNotEmpty(this.promptVersion)) {
            return this.promptTemplateService.renderSection(this.promptCode, this.promptVersion, "developer_prompt", promptVariables);
        }
        return replaceVariables(this.systemPromptTemplate, promptVariables);
    }

    /**
     * 渲染用户提示词。
     *
     * @param promptVariables 变量集合
     * @return 用户提示词
     */
    protected String renderUserPrompt(Map<String, String> promptVariables) {
        if (oConvertUtils.isNotEmpty(this.promptCode) && oConvertUtils.isNotEmpty(this.promptVersion)) {
            return this.promptTemplateService.renderSection(this.promptCode, this.promptVersion, "user_prompt_template", promptVariables);
        }
        return replaceVariables(this.userPromptTemplate, promptVariables);
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
}
