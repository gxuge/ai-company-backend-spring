package org.jeecg.modules.airag.agent.runtime;

import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorSupport;
import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.sse.SseConnectionManager;
import org.jeecg.modules.airag.agent.sse.SsePayload;
import org.jeecg.modules.airag.agent.service.TsAgentChatMessageEventService;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 事件发布器。
 *
 * @author codex
 * @date 2026/6/16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentEventPublisher {
    /**
     * DeepAgents 内部委托工具名。
     */
    private static final String INTERNAL_TASK_TOOL = "task";

    /**
     * 展示摘要最大长度。
     */
    private static final int SUMMARY_MAX_LENGTH = 200;

    /**
     * 当前 Tool 完整事件暂存集合键。
     */
    private static final String ATTR_TOOL_EVENT_STATES = AgentEventPublisher.class.getName() + ".toolEventStates";

    /**
     * 当前 LLM 完整事件暂存集合键。
     */
    private static final String ATTR_LLM_EVENT_STATES = AgentEventPublisher.class.getName() + ".llmEventStates";

    /**
     * 当前 Run 已在 Tool 前落库的 LLM 文本段集合键。
     */
    private static final String ATTR_LLM_PERSISTED_CONTENT = AgentEventPublisher.class.getName() + ".llmPersistedContent";

    /**
     * 事件落库服务。
     */
    private final TsAgentChatMessageEventService eventService;
    /**
     * SSE 管理器。
     */
    private final SseConnectionManager sseConnectionManager;
    /**
     * 当前进程内的 LLM 增量文本缓冲。
     */
    private final Map<String, StringBuffer> llmBuffers = new ConcurrentHashMap<>();

    /**
     * 发送 agent.start。
     *
     * @param context 运行上下文
     * @param agentName Agent 名称
     */
    public void publishAgentStart(AgentContext context, String agentName) {
        Map<String, Object> dbData = buildEventData(
                "agent.start",
                null,
                null,
                null,
                null,
                agentName,
                "Starting " + safeText(agentName, "Agent"),
                null,
                null
        );
        fillContextFields(dbData, context);
        Map<String, Object> sseData = buildCompactAgentEventData(context, agentName, null, "running", null);
        recordEventTrail(context, dbData);
        sendOnlyCompact(context, "agent.start", null, agentName, "Starting " + safeText(agentName, "Agent"), 2, sseData);
    }

    /**
     * 发送 agent.end。
     *
     * @param context 运行上下文
     * @param agentName Agent 名称
     * @param result 执行结果
     */
    public void publishAgentEnd(AgentContext context, String agentName, AgentResult result) {
        Map<String, Object> dbData = buildEventData(
                "agent.end",
                null,
                null,
                null,
                null,
                agentName,
                result == null ? null : result.getContent(),
                result == null || result.getStatus() == null ? null : result.getStatus().name(),
                result == null ? null : result.getData()
        );
        fillContextFields(dbData, context);
        Map<String, Object> sseData = buildCompactAgentEventData(
                context,
                agentName,
                result == null ? null : result.getError(),
                result == null || result.getStatus() == null ? null : result.getStatus().name(),
                result
        );
        recordEventTrail(context, dbData);
        sendOnlyCompact(context, "agent.end", null, agentName, buildAgentEndContent(result), result == null || result.getStatus() == null ? null : eventStatus(result), sseData);
    }

    /**
     * 发送子 Agent 开始事件。
     *
     * @param context 运行上下文
     * @param subAgentName 子 Agent 名称
     * @param payload 扩展数据
     */
    public void publishSubAgentStart(AgentContext context, String subAgentName, Map<String, Object> payload) {
        context.setLastCompletedSubAgentEventId(null);
        sendOnlyCustomCompact(context, "subagent.start", "subagent", subAgentName,
                "Starting " + safeText(subAgentName, "SubAgent"), 2);
    }

    /**
     * 发送子 Agent 成功结束事件。
     *
     * @param context 运行上下文
     * @param subAgentName 子 Agent 名称
     * @param result 执行结果
     * @param payload 扩展数据
     */
    public void publishSubAgentEnd(AgentContext context,
                                   String subAgentName,
                                   AgentResult result,
                                   Map<String, Object> payload) {
        context.setLastCompletedSubAgentEventId(null);
        sendOnlyCustomCompact(context, "subagent.end", "subagent", subAgentName,
                buildSubAgentEndContent(result),
                result == null || result.getStatus() == null ? 0 : eventStatus(result));
    }

    /**
     * 发送子 Agent 错误事件。
     *
     * @param context 运行上下文
     * @param subAgentName 子 Agent 名称
     * @param error 错误对象
     * @param payload 扩展数据
     */
    public void publishSubAgentError(AgentContext context,
                                     String subAgentName,
                                     Throwable error,
                                     Map<String, Object> payload) {
        AgentErrorSupport.ResolvedError resolved = AgentErrorSupport.resolve(
                error,
                AgentErrorCode.RUNTIME_SUBAGENT_EXECUTION_FAILED
        );
        Map<String, Object> errorPayload = AgentErrorSupport.toPayload(resolved);
        sendOnlyCustomCompact(context, "subagent.error", "subagent", subAgentName,
                resolved.code().defaultMessage(), 0, errorPayload);
    }

    /**
     * 发送 llm.start，并暂存轻量 LLM 执行状态。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param promptCode 模板编码
     */
    public void publishLlmStart(AgentContext context, String nodeName, String promptCode) {
        getLlmStates(context).put(buildLlmStateKey(nodeName), new LlmEventState(
                UUIDGenerator.generate(),
                System.currentTimeMillis(),
                promptCode
        ));
        Map<String, Object> dbData = buildEventData(
                "llm.start",
                NodeKind.LLM,
                nodeName,
                promptCode,
                null,
                null,
                "Generating",
                2,
                null
        );
        recordEventTrail(context, dbData);
        sendOnlyCompact(context, "llm.start", NodeKind.LLM, nodeName, "Generating", 2, null);
    }

    /**
     * 更新 LLM 调用元数据，Prompt 和消息上下文不进入事件表。
     */
    public void updateLlmExecutionMetadata(AgentContext context,
                                           String nodeName,
                                           String modelId,
                                           String provider,
                                           String modelName,
                                           String finishReason,
                                           Integer inputTokens,
                                           Integer outputTokens,
                                           Integer totalTokens) {
        LlmEventState state = getLlmState(context, nodeName, null);
        state.updateModel(modelId, provider, modelName);
        state.updateResult(finishReason, inputTokens, outputTokens, totalTokens);
    }

    /**
     * 发送 llm.delta，并写入进程内 buffer。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param delta 增量文本
     */
    public void publishLlmDelta(AgentContext context, String nodeName, String delta) {
        String bufferKey = buildLlmBufferKey(context, nodeName);
        appendBuffer(bufferKey, delta);
        Map<String, Object> dbData = buildEventData(
                "llm.delta",
                NodeKind.LLM,
                nodeName,
                null,
                null,
                null,
                delta,
                2,
                null
        );
        recordEventTrail(context, dbData);
        sendOnly(context, "llm.delta", NodeKind.LLM, nodeName, delta, delta);
    }

    /**
     * 发送 llm.error，并暂存错误信息。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param promptCode 模板编码
     * @param error 错误对象
     */
    public void publishLlmError(AgentContext context, String nodeName, String promptCode, Throwable error) {
        AgentErrorSupport.ResolvedError resolved = AgentErrorSupport.resolve(
                error,
                AgentErrorCode.LLM_CHAT_EXECUTION_FAILED
        );
        String errorText = resolved.code().defaultMessage();
        Map<String, Object> payload = AgentErrorSupport.toPayload(resolved);
        Map<String, Object> dbData = buildEventData(
                "llm.error",
                NodeKind.LLM,
                nodeName,
                promptCode,
                null,
                null,
                errorText,
                0,
                payload
        );
        recordEventTrail(context, dbData);
        LlmEventState state = getLlmState(context, nodeName, promptCode);
        state.setError(new LinkedHashMap<>(payload));
        sendOnlyCompact(context, "llm.error", NodeKind.LLM, nodeName, errorText, 0, payload);
    }

    /**
     * 发送 llm.end，并将本次完整文本段落与执行信息写入事件表。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param promptCode 模板编码
     * @param success 是否成功
     * @param payload 扩展数据
     */
    public void publishLlmEnd(AgentContext context,
                              String nodeName,
                              String promptCode,
                              boolean success,
                              Map<String, Object> payload) {
        publishLlmEnd(context, nodeName, promptCode, success ? 1 : 0, payload);
    }

    public void publishLlmEnd(AgentContext context,
                              String nodeName,
                              String promptCode,
                              int status,
                              Map<String, Object> payload) {
        String bufferKey = buildLlmBufferKey(context, nodeName);
        String content = readBuffer(bufferKey);
        clearBuffer(bufferKey);
        LlmEventState state = removeLlmState(context, nodeName, promptCode);
        String fallbackContent = extractText(payload, "reply", "content", "rawText", "text", "summary");
        if ((content == null || content.isBlank()) && !state.hasPersistedSegment()) {
            content = fallbackContent;
        }
        if ((content == null || content.isBlank()) && status == 1 && !state.hasPersistedSegment()) {
            content = context.getLatestContent();
        }
        String persistedContent = removePersistedLlmContent(context, nodeName);
        if (status == 3) {
            context.putAttribute("interruptedLlmContent", joinLlmContent(persistedContent, content));
        }
        String sseContent = content;
        if (sseContent == null || sseContent.isBlank()) {
            sseContent = fallbackContent;
        }
        Map<String, Object> dbData = buildEventData(
                "llm.end",
                NodeKind.LLM,
                nodeName,
                promptCode,
                null,
                null,
                content,
                status,
                payload
        );
        recordEventTrail(context, dbData);
        if ((content != null && !content.isBlank()) || status != 1) {
            persistLlmSegment(context, nodeName, state.eventId(), state, content, status);
        }
        sendOnlyCompact(context, "llm.end", NodeKind.LLM, nodeName, sseContent, status, null);
    }

    /**
     * 发送确认节点等待用户选择事件。
     *
     * <p>前端载荷只包含 question 和 options。</p>
     *
     * @param context 运行上下文
     * @param eventName SSE 事件名
     * @param nodeName 节点名
     * @param question 确认问题
     * @param options 确认选项
     */
    public void publishConfirmStart(AgentContext context,
                                    String eventName,
                                    String nodeName,
                                    String question,
                                    List<Map<String, String>> options) {
        Map<String, Object> payload = buildOptionPromptSseData(question, options);
        Map<String, Object> dbData = buildEventData(
                eventName,
                NodeKind.CONFIRM,
                nodeName,
                null,
                null,
                null,
                question,
                2,
                payload
        );
        recordEventTrail(context, dbData);
        persistInteractiveStart(context, NodeKind.CONFIRM, nodeName, question, options);
        sendOptionPromptRaw(context, eventName, payload);
    }

    /**
     * 发送确认节点选择完成事件，并完成原运行中事件。
     *
     * @param context 运行上下文
     * @param eventName SSE 事件名
     * @param nodeName 节点名
     * @param question 确认问题
     * @param options 确认选项
     * @param resultData 节点选择结果
     */
    public void publishConfirmEnd(AgentContext context,
                                  String eventName,
                                  String nodeName,
                                  String question,
                                  List<Map<String, String>> options,
                                  Map<String, Object> resultData) {
        publishInteractiveEnd(
                context,
                eventName,
                NodeKind.CONFIRM,
                nodeName,
                question,
                options,
                resultData
        );
    }

    /**
     * 发送确认节点异常事件。
     *
     * <p>为了保持确认事件协议稳定，前端载荷仍只包含 question 和 options。</p>
     *
     * @param context 运行上下文
     * @param eventName SSE 事件名
     * @param nodeName 节点名
     * @param error 异常
     */
    public void publishConfirmError(AgentContext context,
                                    String eventName,
                                    String nodeName,
                                    String originalQuestion,
                                    List<Map<String, String>> options,
                                    Throwable error) {
        String question = "Confirmation failed";
        Map<String, Object> payload = buildOptionPromptSseData(question, List.of());
        Map<String, Object> dbData = buildEventData(
                eventName,
                NodeKind.CONFIRM,
                nodeName,
                null,
                null,
                null,
                question,
                0,
                payload
        );
        recordEventTrail(context, dbData);
        persistInteractiveError(
                context,
                NodeKind.CONFIRM,
                nodeName,
                originalQuestion,
                options,
                error
        );
        sendOptionPromptRaw(context, eventName, payload);
    }

    /**
     * 发送候选项节点等待用户选择事件。
     *
     * <p>前端载荷只包含 question 和 options。</p>
     *
     * @param context 运行上下文
     * @param eventName SSE 事件名
     * @param nodeName 节点名
     * @param question 选择问题
     * @param options 候选项
     */
    public void publishOptionsStart(AgentContext context,
                                    String eventName,
                                    String nodeName,
                                    String question,
                                    List<Map<String, String>> options) {
        Map<String, Object> payload = buildOptionPromptSseData(question, options);
        Map<String, Object> dbData = buildEventData(
                eventName,
                NodeKind.OPTIONS,
                nodeName,
                null,
                null,
                null,
                question,
                2,
                payload
        );
        recordEventTrail(context, dbData);
        persistInteractiveStart(context, NodeKind.OPTIONS, nodeName, question, options);
        sendOptionPromptRaw(context, eventName, payload);
    }

    /**
     * 发送候选项节点选择完成事件，并完成原运行中事件。
     *
     * @param context 运行上下文
     * @param eventName SSE 事件名
     * @param nodeName 节点名
     * @param question 选择问题
     * @param options 候选项
     * @param resultData 节点选择结果
     */
    public void publishOptionsEnd(AgentContext context,
                                  String eventName,
                                  String nodeName,
                                  String question,
                                  List<Map<String, String>> options,
                                  Map<String, Object> resultData) {
        publishInteractiveEnd(
                context,
                eventName,
                NodeKind.OPTIONS,
                nodeName,
                question,
                options,
                resultData
        );
    }

    /**
     * 发送候选项节点异常事件。
     *
     * @param context 运行上下文
     * @param eventName SSE 事件名
     * @param nodeName 节点名
     * @param error 异常
     */
    public void publishOptionsError(AgentContext context,
                                    String eventName,
                                    String nodeName,
                                    String originalQuestion,
                                    List<Map<String, String>> options,
                                    Throwable error) {
        String question = "Option selection failed";
        Map<String, Object> payload = buildOptionPromptSseData(question, List.of());
        Map<String, Object> dbData = buildEventData(
                eventName,
                NodeKind.OPTIONS,
                nodeName,
                null,
                null,
                null,
                question,
                0,
                payload
        );
        recordEventTrail(context, dbData);
        persistInteractiveError(
                context,
                NodeKind.OPTIONS,
                nodeName,
                originalQuestion,
                options,
                error
        );
        sendOptionPromptRaw(context, eventName, payload);
    }

    /**
     * 发送 tool.start，并暂存完整 Tool 事件的输入与开始时间。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param toolName 工具名
     * @param payload 扩展数据
     */
    public void publishToolStart(AgentContext context, String nodeName, String toolName, Map<String, Object> payload) {
        flushLlmSegmentBeforeTool(context, nodeName);
        CompleteEventState toolState = new CompleteEventState(
                UUIDGenerator.generate(),
                System.currentTimeMillis(),
                buildToolInput(payload)
        );
        getToolStates(context).put(buildToolStateKey(nodeName, toolName), toolState);
        String content = "Calling " + safeText(toolName, "Tool");
        Map<String, Object> dbData = buildEventData(
                "tool.start",
                NodeKind.TOOL,
                nodeName,
                null,
                toolName,
                null,
                content,
                2,
                payload
        );
        Map<String, Object> sseData = buildCompactToolEventData("tool.start", toolName, content, 2, payload, null);
        recordEventTrail(context, dbData);
        if (!isInternalTaskToolEvent(NodeKind.TOOL, dbData)) {
            sendOnlyCompact(context, "tool.start", NodeKind.TOOL, nodeName, content, 2, sseData);
        }
    }

    /**
     * 发送异步 tool.start，并立即保存运行中事件。
     *
     * @param context 运行上下文
     * @param eventId 预分配事件ID
     * @param nodeName 节点名
     * @param toolName 工具名
     * @param payload 扩展数据
     */
    public void publishAsyncToolStart(AgentContext context,
                                      String eventId,
                                      String nodeName,
                                      String toolName,
                                      Map<String, Object> payload) {
        flushLlmSegmentBeforeTool(context, nodeName);
        CompleteEventState toolState = new CompleteEventState(
                eventId,
                System.currentTimeMillis(),
                buildToolInput(payload)
        );
        getToolStates(context).put(buildToolStateKey(nodeName, toolName), toolState);
        String content = "Calling " + safeText(toolName, "Tool");
        Map<String, Object> dbData = buildEventData(
                "tool.start",
                NodeKind.TOOL,
                nodeName,
                null,
                toolName,
                null,
                content,
                2,
                payload
        );
        Map<String, Object> completeData = buildPendingToolData(toolState);
        completeData.put("async", Boolean.TRUE);
        fillContextFields(completeData, context);
        this.eventService.saveEvent(
                eventId,
                eventMessageId(context),
                context.getSessionId(),
                context.getAgentSessionId(),
                "tool",
                safeText(toolName, nodeName),
                nodeName,
                NodeKind.TOOL.name().toLowerCase(),
                summarize(content),
                2,
                completeData
        );
        recordEventTrail(context, dbData);
        this.sseConnectionManager.retain(context.getSseConnectionKey());
        Map<String, Object> sseData = buildCompactToolEventData("tool.start", toolName, content, 2, payload, null);
        sendOnlyCompact(context, "tool.start", NodeKind.TOOL, nodeName, content, 2, sseData);
    }

    /**
     * 完成异步 Tool，并更新开始时保存的同一条事件。
     *
     * @param context 运行上下文
     * @param eventId 事件ID
     * @param nodeName 节点名
     * @param toolName 工具名
     * @param content 结果摘要
     * @param payload 结果载荷
     */
    public void publishAsyncToolEnd(AgentContext context,
                                    String eventId,
                                    String nodeName,
                                    String toolName,
                                    String content,
                                    Map<String, Object> payload) {
        String summary = safeText(summarize(content), "Completed " + safeText(toolName, "Tool"));
        try {
            CompleteEventState state = removeToolState(context, nodeName, toolName);
            Map<String, Object> completeData = buildCompleteToolData(true, content, payload, state);
            completeData.put("async", Boolean.TRUE);
            fillContextFields(completeData, context);
            this.eventService.updateEventResult(eventId, summary, 1, completeData);

            Map<String, Object> dbData = buildEventData(
                    "tool.end",
                    NodeKind.TOOL,
                    nodeName,
                    null,
                    toolName,
                    null,
                    summary,
                    1,
                    payload
            );
            recordEventTrail(context, dbData);
            Map<String, Object> sseData = buildCompactToolEventData(
                    "tool.end",
                    toolName,
                    summary,
                    1,
                    payload,
                    content
            );
            sendOnlyCompact(context, "tool.end", NodeKind.TOOL, nodeName, summary, 1, sseData);
        } finally {
            this.sseConnectionManager.release(context == null ? null : context.getSseConnectionKey());
        }
    }

    /**
     * 结束失败的异步 Tool，并更新开始时保存的同一条事件。
     *
     * @param context 运行上下文
     * @param eventId 事件ID
     * @param nodeName 节点名
     * @param toolName 工具名
     * @param error 失败原因
     * @param payload 错误载荷
     */
    public void publishAsyncToolError(AgentContext context,
                                      String eventId,
                                      String nodeName,
                                      String toolName,
                                      Throwable error,
                                      Map<String, Object> payload) {
        AgentErrorCode fallbackCode = AgentErrorSupport.toolExecutionCode(toolName);
        AgentErrorSupport.ResolvedError resolved = AgentErrorSupport.resolve(error, fallbackCode);
        String message = resolved.code().defaultMessage();
        try {
            CompleteEventState state = removeToolState(context, nodeName, toolName);
            Map<String, Object> resolvedPayload = AgentErrorSupport.toPayload(resolved);
            state.setError(new LinkedHashMap<>(resolvedPayload));
            Map<String, Object> errorPayload = new LinkedHashMap<>();
            if (payload != null) {
                errorPayload.putAll(payload);
            }
            errorPayload.putAll(resolvedPayload);
            Map<String, Object> completeData = buildCompleteToolData(false, message, errorPayload, state);
            completeData.put("async", Boolean.TRUE);
            fillContextFields(completeData, context);
            this.eventService.updateEventResult(eventId, summarize(message), 0, completeData);

            Map<String, Object> dbData = buildEventData(
                    "tool.error",
                    NodeKind.TOOL,
                    nodeName,
                    null,
                    toolName,
                    null,
                    message,
                    0,
                    errorPayload
            );
            recordEventTrail(context, dbData);
            Map<String, Object> sseData = buildCompactToolEventData(
                    "tool.error",
                    toolName,
                    message,
                    0,
                    errorPayload,
                    null
            );
            sendOnlyCompact(context, "tool.error", NodeKind.TOOL, nodeName, message, 0, sseData);
        } finally {
            this.sseConnectionManager.release(context == null ? null : context.getSseConnectionKey());
        }
    }

    public void publishAsyncToolInterrupted(AgentContext context,
                                            String eventId,
                                            String nodeName,
                                            String toolName,
                                            Map<String, Object> payload) {
        String summary = "Interrupted " + safeText(toolName, "Tool");
        try {
            CompleteEventState state = removeToolState(context, nodeName, toolName);
            Map<String, Object> completeData = buildCompleteToolData(false, summary, payload, state);
            completeData.put("error", null);
            completeData.put("async", Boolean.TRUE);
            fillContextFields(completeData, context);
            this.eventService.updateEventResult(eventId, summary, 3, completeData);

            Map<String, Object> dbData = buildEventData(
                    "tool.end",
                    NodeKind.TOOL,
                    nodeName,
                    null,
                    toolName,
                    null,
                    summary,
                    3,
                    payload
            );
            recordEventTrail(context, dbData);
            Map<String, Object> sseData = buildCompactToolEventData(
                    "tool.end",
                    toolName,
                    summary,
                    3,
                    payload,
                    null
            );
            sendOnlyCompact(context, "tool.end", NodeKind.TOOL, nodeName, summary, 3, sseData);
        } finally {
            this.sseConnectionManager.release(context == null ? null : context.getSseConnectionKey());
        }
    }

    /**
     * 发送 tool.error，并暂存完整 Tool 事件的错误信息。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param toolName 工具名
     * @param error 错误对象
     * @param payload 扩展数据
     */
    public void publishToolError(AgentContext context,
                                 String nodeName,
                                 String toolName,
                                 Throwable error,
                                 Map<String, Object> payload) {
        AgentErrorSupport.ResolvedError resolved = AgentErrorSupport.resolve(
                error,
                AgentErrorSupport.toolExecutionCode(toolName)
        );
        String message = resolved.code().defaultMessage();
        Map<String, Object> errorPayload = AgentErrorSupport.toPayload(resolved);
        Map<String, Object> dbData = buildEventData(
                "tool.error",
                NodeKind.TOOL,
                nodeName,
                null,
                toolName,
                null,
                message,
                0,
                mergePayload(payload, errorPayload)
        );
        Map<String, Object> sseData = buildCompactToolEventData(
                "tool.error",
                toolName,
                message,
                0,
                mergePayload(payload, errorPayload),
                null
        );
        recordEventTrail(context, dbData);
        CompleteEventState state = getToolState(context, nodeName, toolName);
        state.setError(new LinkedHashMap<>(errorPayload));
        if (!isInternalTaskToolEvent(NodeKind.TOOL, dbData)) {
            sendOnlyCompact(context, "tool.error", NodeKind.TOOL, nodeName,
                    message, 0, sseData);
        }
    }

    /**
     * 发送 tool.end，并将本次 Tool 执行作为一条完整事件写入事件表。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param toolName 工具名
     * @param success 是否成功
     * @param content 结果摘要
     * @param payload 扩展数据
     */
    public void publishToolEnd(AgentContext context,
                               String nodeName,
                               String toolName,
                               boolean success,
                               String content,
                               Map<String, Object> payload) {
        publishToolEnd(context, nodeName, toolName, success ? 1 : 0, content, payload);
    }

    public void publishToolEnd(AgentContext context,
                               String nodeName,
                               String toolName,
                               int status,
                               String content,
                               Map<String, Object> payload) {
        String summary = safeText(summarize(content), "Completed " + safeText(toolName, "Tool"));
        Map<String, Object> dbData = buildEventData(
                "tool.end",
                NodeKind.TOOL,
                nodeName,
                null,
                toolName,
                null,
                summary,
                status,
                payload
        );
        Map<String, Object> sseData = buildCompactToolEventData(
                "tool.end",
                toolName,
                summary,
                status,
                payload,
                content
        );
        recordEventTrail(context, dbData);
        CompleteEventState state = removeToolState(context, nodeName, toolName);
        if (isInternalTaskToolEvent(NodeKind.TOOL, dbData)) {
            return;
        }
        Map<String, Object> completeData = buildCompleteToolData(status == 1, content, payload, state);
        if (status == 3) {
            completeData.put("error", null);
        }
        fillContextFields(completeData, context);
        this.eventService.saveEvent(
                state.eventId(),
                eventMessageId(context),
                context.getSessionId(),
                context.getAgentSessionId(),
                "tool",
                safeText(toolName, nodeName),
                nodeName,
                NodeKind.TOOL.name().toLowerCase(),
                summarize(summary),
                status,
                completeData
        );
        sendOnlyCompact(context, "tool.end", NodeKind.TOOL, nodeName, summary, status, sseData);
    }

    /**
     * 构建事件数据。
     *
     * @param eventName 事件名
     * @param nodeKind 节点类型
     * @param nodeName 节点名
     * @param promptCode 模板编码
     * @param toolName 工具名
     * @param agentName Agent 名称
     * @param content 主体内容
     * @param status 状态值
     * @param payload 扩展载荷
     * @return JSON Map
     */
    private Map<String, Object> buildEventData(String eventName,
                                               NodeKind nodeKind,
                                               String nodeName,
                                               String promptCode,
                                               String toolName,
                                               String agentName,
                                               String content,
                                               Object status,
                                               Map<String, Object> payload) {
        Map<String, Object> data = new LinkedHashMap<>();
        putString(data, "event", eventName);
        putString(data, "nodeType", nodeKind == null ? null : nodeKind.name().toLowerCase());
        putString(data, "nodeName", nodeName);
        putString(data, "promptCode", promptCode);
        putString(data, "toolName", toolName);
        putString(data, "agentName", agentName);
        putString(data, "summary", summarize(content));
        if (status != null) {
            data.put("status", status);
        }
        if (payload != null && !payload.isEmpty()) {
            putString(data, "action", stringValue(payload.get("action")));
            putString(data, "reply", stringValue(payload.get("reply")));
            putString(data, "targetSubAgent", stringValue(payload.get("targetSubAgent")));
            putString(data, "errorCode", stringValue(payload.get("errorCode")));
            putString(data, "errorMessage", stringValue(payload.get("errorMessage")));
        }
        return data;
    }

    /**
     * 构建子 Agent 自定义事件数据。
     *
     * @param eventName 事件名
     * @param type 事件类型
     * @param nodeName 节点名
     * @param content 主体内容
     * @param status 状态值
     * @param payload 扩展载荷
     * @return JSON Map
     */
    private Map<String, Object> buildCustomEventData(String eventName,
                                                     String type,
                                                     String nodeName,
                                                     String content,
                                                     Object status,
                                                     Map<String, Object> payload) {
        Map<String, Object> data = new LinkedHashMap<>();
        putString(data, "event", eventName);
        putString(data, "nodeType", type);
        putString(data, "nodeName", nodeName);
        putString(data, "summary", summarize(content));
        if (status != null) {
            data.put("status", status);
        }
        if (payload != null && !payload.isEmpty()) {
            data.putAll(payload);
        }
        return data;
    }

    private Map<String, Object> buildOptionPromptSseData(String question,
                                                         List<Map<String, String>> options) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("question", question);
        data.put("options", options == null ? List.of() : options);
        return data;
    }

    private void publishInteractiveEnd(AgentContext context,
                                       String eventName,
                                       NodeKind nodeKind,
                                       String nodeName,
                                       String question,
                                       List<Map<String, String>> options,
                                       Map<String, Object> resultData) {
        Map<String, Object> payload = buildOptionEndSseData(question, resultData);
        String content = buildSelectionSummary(payload);
        Map<String, Object> dbData = buildEventData(
                eventName,
                nodeKind,
                nodeName,
                null,
                null,
                null,
                content,
                1,
                resultData
        );
        recordEventTrail(context, dbData);
        persistInteractiveEnd(
                context,
                nodeKind,
                nodeName,
                question,
                options,
                resultData,
                content
        );
        sendOptionPromptRaw(context, eventName, payload);
    }

    private Map<String, Object> buildOptionEndSseData(String question,
                                                      Map<String, Object> resultData) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("question", question);
        data.put("selectedOption", normalizedSelection(
                resultData == null ? null : resultData.get("selectedOption"),
                stringValue(resultData == null ? null : resultData.get("optionValue"))
        ));
        putString(data, "action", stringValue(resultData == null ? null : resultData.get("action")));
        putString(data, "reply", stringValue(resultData == null ? null : resultData.get("reply")));
        return data;
    }

    private void persistInteractiveStart(AgentContext context,
                                         NodeKind nodeKind,
                                         String nodeName,
                                         String question,
                                         List<Map<String, String>> options) {
        if (context == null || nodeKind == null) {
            return;
        }
        String nodeType = nodeKind.name().toLowerCase();
        TsAgentChatMessageEventEntity pending = this.eventService.findLatestPendingInteractiveEvent(
                context.getSessionId(),
                context.getAgentCode(),
                nodeName,
                nodeType
        );
        if (pending != null) {
            return;
        }
        Map<String, Object> completeData = buildInteractiveStartData(question, options);
        fillContextFields(completeData, context);
        this.eventService.saveEvent(
                UUIDGenerator.generate(),
                eventMessageId(context),
                context.getSessionId(),
                context.getAgentSessionId(),
                nodeType,
                nodeName,
                nodeName,
                nodeType,
                summarize(question),
                2,
                completeData
        );
    }

    private void persistInteractiveEnd(AgentContext context,
                                       NodeKind nodeKind,
                                       String nodeName,
                                       String question,
                                       List<Map<String, String>> options,
                                       Map<String, Object> resultData,
                                       String content) {
        if (context == null || nodeKind == null) {
            return;
        }
        String nodeType = nodeKind.name().toLowerCase();
        TsAgentChatMessageEventEntity pending = this.eventService.findLatestPendingInteractiveEvent(
                context.getSessionId(),
                context.getAgentCode(),
                nodeName,
                nodeType
        );
        Map<String, Object> completeData = buildInteractiveEndData(
                context,
                pending,
                question,
                options,
                resultData
        );
        fillContextFields(completeData, context);
        if (pending != null) {
            this.eventService.updateEventResult(
                    pending.getId(),
                    summarize(content),
                    1,
                    completeData
            );
            return;
        }
        this.eventService.saveEvent(
                UUIDGenerator.generate(),
                eventMessageId(context),
                context.getSessionId(),
                context.getAgentSessionId(),
                nodeType,
                nodeName,
                nodeName,
                nodeType,
                summarize(content),
                1,
                completeData
        );
    }

    private void persistInteractiveError(AgentContext context,
                                         NodeKind nodeKind,
                                         String nodeName,
                                         String question,
                                         List<Map<String, String>> options,
                                         Throwable error) {
        if (context == null || nodeKind == null) {
            return;
        }
        String nodeType = nodeKind.name().toLowerCase();
        TsAgentChatMessageEventEntity pending = this.eventService.findLatestPendingInteractiveEvent(
                context.getSessionId(),
                context.getAgentCode(),
                nodeName,
                nodeType
        );
        Map<String, Object> completeData = new LinkedHashMap<>();
        completeData.put("input", pending == null
                ? buildInteractiveInput(question, options)
                : readInteractiveInput(pending));
        completeData.put("output", null);
        completeData.put("error", buildErrorData(
                error == null ? "INTERACTIVE_NODE_ERROR" : error.getClass().getSimpleName(),
                error == null ? "Interactive node failed" : error.getMessage()
        ));
        completeData.put("metrics", buildInteractiveMetrics(pending));
        fillContextFields(completeData, context);
        String content = "Interaction failed";
        if (pending != null) {
            this.eventService.updateEventResult(pending.getId(), summarize(content), 0, completeData);
        }
    }

    private Map<String, Object> buildInteractiveStartData(String question,
                                                          List<Map<String, String>> options) {
        Map<String, Object> completeData = new LinkedHashMap<>();
        completeData.put("input", buildInteractiveInput(question, options));
        completeData.put("output", null);
        completeData.put("error", null);
        completeData.put("metrics", new LinkedHashMap<>());
        return completeData;
    }

    private Map<String, Object> buildInteractiveEndData(AgentContext context,
                                                        TsAgentChatMessageEventEntity pending,
                                                        String question,
                                                        List<Map<String, String>> options,
                                                        Map<String, Object> resultData) {
        String optionValue = stringValue(resultData == null ? null : resultData.get("optionValue"));
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("selection", normalizedSelection(
                resultData == null ? null : resultData.get("selectedOption"),
                optionValue
        ));
        putString(output, "value", optionValue);
        putString(output, "action", stringValue(resultData == null ? null : resultData.get("action")));
        putString(output, "reply", stringValue(resultData == null ? null : resultData.get("reply")));
        putString(output, "selectionMessageId", context == null ? null : context.getMessageId());
        putString(output, "selectionRunId", context == null ? null : context.getRunId());

        Map<String, Object> completeData = new LinkedHashMap<>();
        completeData.put("input", pending == null
                ? buildInteractiveInput(question, options)
                : readInteractiveInput(pending));
        completeData.put("output", output);
        completeData.put("error", null);
        completeData.put("metrics", buildInteractiveMetrics(pending));
        return completeData;
    }

    private Map<String, Object> buildInteractiveInput(String question,
                                                      List<Map<String, String>> options) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("question", question);
        input.put("options", options == null ? List.of() : options);
        return input;
    }

    private Map<String, Object> readInteractiveInput(TsAgentChatMessageEventEntity pending) {
        if (pending == null || pending.getJson() == null || pending.getJson().isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            JSONObject root = JSONObject.parseObject(pending.getJson());
            Object rawInput = root == null ? null : root.get("input");
            if (rawInput instanceof Map<?, ?> rawMap) {
                Map<String, Object> input = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() != null) {
                        input.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                return input;
            }
        } catch (Exception ex) {
            log.warn("读取交互事件输入失败，eventId={}", pending.getId(), ex);
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> buildInteractiveMetrics(TsAgentChatMessageEventEntity pending) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        if (pending != null) {
            metrics.put("interactionEventId", pending.getId());
            long startedAt = pending.getCreatedAt() == null
                    ? System.currentTimeMillis()
                    : pending.getCreatedAt().getTime();
            metrics.put("durationMs", elapsedMillis(startedAt));
        } else {
            metrics.put("durationMs", 0L);
        }
        return metrics;
    }

    private Map<String, String> normalizedSelection(Object rawSelection, String optionValue) {
        Map<String, String> selection = new LinkedHashMap<>();
        if (rawSelection instanceof Map<?, ?> rawMap) {
            String label = firstOptionValue(rawMap, "label", "text", "name");
            String value = firstOptionValue(rawMap, "optionValue", "value", "action");
            if (label != null) {
                selection.put("label", label);
            }
            if (value != null) {
                selection.put("value", value);
                selection.put("optionValue", value);
            }
        }
        if (!selection.containsKey("optionValue") && optionValue != null) {
            selection.put("optionValue", optionValue);
        }
        if (!selection.containsKey("value") && optionValue != null) {
            selection.put("value", optionValue);
        }
        if (!selection.containsKey("label") && optionValue != null) {
            selection.put("label", optionValue);
        }
        return selection;
    }

    private String buildSelectionSummary(Map<String, Object> payload) {
        Object rawSelection = payload == null ? null : payload.get("selectedOption");
        if (rawSelection instanceof Map<?, ?> rawMap) {
            String label = firstOptionValue(rawMap, "label", "optionValue", "value");
            if (label != null) {
                return "Selected: " + label;
            }
        }
        return "Selection completed";
    }

    /**
     * 构建 Tool 输入。
     *
     * @param payload 起始载荷
     * @return 固定结构输入
     */
    private Map<String, Object> buildToolInput(Map<String, Object> payload) {
        Map<String, Object> input = new LinkedHashMap<>();
        Object arguments = firstValue(payload, "toolArguments", "arguments", "args");
        input.put("arguments", arguments == null ? new LinkedHashMap<>() : arguments);
        return input;
    }

    /**
     * 构建完整 Tool 数据。
     *
     * @param success 是否成功
     * @param content 结果摘要
     * @param payload Tool 结果载荷
     * @param state 起始状态
     * @return 固定结构完整数据
     */
    private Map<String, Object> buildCompleteToolData(boolean success,
                                                      String content,
                                                      Map<String, Object> payload,
                                                      CompleteEventState state) {
        Map<String, Object> complete = new LinkedHashMap<>();
        Map<String, Object> input = new LinkedHashMap<>(state.input());
        Object arguments = firstValue(payload, "toolArguments", "arguments", "args");
        if (arguments != null) {
            input.put("arguments", arguments);
        }
        complete.put("input", input);

        if (success) {
            Map<String, Object> output = new LinkedHashMap<>();
            if (isImageToolPayload(payload)) {
                appendImageFields(output, payload);
                output.put("summary", content);
            } else {
                Object result = firstValue(payload, "toolData", "resultJson", "structuredResult", "result", "toolPayload");
                output.put("result", result == null ? content : result);
                output.put("summary", content);
            }
            complete.put("output", output);
        } else {
            complete.put("output", null);
        }

        Map<String, Object> error = state.error();
        if (!success && error == null) {
            String errorCode = stringValue(firstValue(payload, "errorCode"));
            String errorMessage = stringValue(firstValue(payload, "errorMessage", "error"));
            error = buildErrorData(safeText(errorCode, "TOOL_EXECUTION_ERROR"),
                    safeText(errorMessage, safeText(content, "Tool execution failed")));
        }
        complete.put("error", error);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("toolCallId", state.eventId());
        metrics.put("durationMs", elapsedMillis(state.startedAt()));
        complete.put("metrics", metrics);
        return complete;
    }

    private Map<String, Object> buildPendingToolData(CompleteEventState state) {
        Map<String, Object> complete = new LinkedHashMap<>();
        complete.put("input", new LinkedHashMap<>(state.input()));
        complete.put("output", null);
        complete.put("error", null);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("toolCallId", state.eventId());
        metrics.put("startedAt", state.startedAt());
        complete.put("metrics", metrics);
        return complete;
    }

    /**
     * 构建不包含 Prompt 和消息上下文的完整 LLM 执行数据。
     */
    private Map<String, Object> buildCompleteLlmData(int status,
                                                     LlmEventState state,
                                                     String content) {
        Map<String, Object> input = new LinkedHashMap<>();
        putString(input, "modelId", state.modelId());
        putString(input, "provider", state.provider());
        putString(input, "modelName", state.modelName());
        putString(input, "promptCode", state.promptCode());

        Map<String, Object> complete = new LinkedHashMap<>();
        complete.put("input", input);
        if (status == 1 || status == 3) {
            Map<String, Object> output = new LinkedHashMap<>();
            putString(output, "finishReason", state.finishReason());
            putString(output, "content", content);
            if (status == 3) {
                output.put("interrupted", Boolean.TRUE);
            }
            complete.put("output", output);
        } else {
            complete.put("output", null);
        }
        complete.put("error", status == 3 ? null : state.error());

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("durationMs", elapsedMillis(state.startedAt()));
        putNumber(metrics, "inputTokens", state.inputTokens());
        putNumber(metrics, "outputTokens", state.outputTokens());
        putNumber(metrics, "totalTokens", state.totalTokens());
        complete.put("metrics", metrics);
        return complete;
    }

    /**
     * Tool 开始前保存已经输出的 LLM 文本段，保留文字与 Tool 的真实顺序。
     */
    private void flushLlmSegmentBeforeTool(AgentContext context, String nodeName) {
        Object rawStates = context == null ? null : context.getAttribute(ATTR_LLM_EVENT_STATES);
        if (!(rawStates instanceof Map<?, ?> rawMap)) {
            return;
        }
        Object rawState = rawMap.get(buildLlmStateKey(nodeName));
        if (!(rawState instanceof LlmEventState state)) {
            return;
        }
        String bufferKey = buildLlmBufferKey(context, nodeName);
        String content = readBuffer(bufferKey);
        if (content == null || content.isBlank()) {
            return;
        }
        clearBuffer(bufferKey);
        appendPersistedLlmContent(context, nodeName, content);
        if (persistLlmSegment(context, nodeName, UUIDGenerator.generate(), state, content, 1)) {
            state.markSegmentPersisted();
        }
    }

    @SuppressWarnings("unchecked")
    private void appendPersistedLlmContent(AgentContext context, String nodeName, String content) {
        if (context == null || content == null || content.isBlank()) {
            return;
        }
        Object rawContents = context.getAttribute(ATTR_LLM_PERSISTED_CONTENT);
        Map<String, String> contents;
        if (rawContents instanceof Map<?, ?>) {
            contents = (Map<String, String>) rawContents;
        } else {
            contents = new ConcurrentHashMap<>();
            context.putAttribute(ATTR_LLM_PERSISTED_CONTENT, contents);
        }
        contents.merge(buildLlmStateKey(nodeName), content, this::joinLlmContent);
    }

    @SuppressWarnings("unchecked")
    private String removePersistedLlmContent(AgentContext context, String nodeName) {
        if (context == null) {
            return null;
        }
        Object rawContents = context.getAttribute(ATTR_LLM_PERSISTED_CONTENT);
        if (!(rawContents instanceof Map<?, ?>)) {
            return null;
        }
        return ((Map<String, String>) rawContents).remove(buildLlmStateKey(nodeName));
    }

    private String joinLlmContent(String first, String second) {
        if (first == null || first.isBlank()) {
            return second == null ? "" : second;
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + System.lineSeparator() + second;
    }

    /**
     * 保存一段完整 LLM 输出及其执行指标。
     */
    private boolean persistLlmSegment(AgentContext context,
                                      String nodeName,
                                      String eventId,
                                      LlmEventState state,
                                      String content,
                                      int status) {
        Map<String, Object> completeData = buildCompleteLlmData(status, state, content);
        fillContextFields(completeData, context);
        String eventContent = content;
        if (eventContent == null || eventContent.isBlank()) {
            eventContent = switch (status) {
                case 1 -> "Model call completed";
                case 3 -> "Model call interrupted";
                default -> "Model call failed";
            };
        }
        try {
            this.eventService.saveEvent(
                    eventId,
                    eventMessageId(context),
                    context.getSessionId(),
                    context.getAgentSessionId(),
                    "llm",
                    safeText(state.modelName(), safeText(state.modelId(), nodeName)),
                    nodeName,
                    NodeKind.LLM.name().toLowerCase(),
                    eventContent,
                    status,
                    completeData
            );
            return true;
        } catch (Exception ex) {
            log.warn("保存 LLM 段落事件失败，nodeName={}, modelId={}", nodeName, state.modelId(), ex);
            return false;
        }
    }

    /**
     * 获取 LLM 状态集合。
     */
    @SuppressWarnings("unchecked")
    private Map<String, LlmEventState> getLlmStates(AgentContext context) {
        Object rawStates = context.getAttribute(ATTR_LLM_EVENT_STATES);
        if (rawStates instanceof Map<?, ?>) {
            return (Map<String, LlmEventState>) rawStates;
        }
        Map<String, LlmEventState> states = new ConcurrentHashMap<>();
        context.putAttribute(ATTR_LLM_EVENT_STATES, states);
        return states;
    }

    /**
     * 获取指定 LLM 状态，不存在时创建。
     */
    private LlmEventState getLlmState(AgentContext context, String nodeName, String promptCode) {
        String key = buildLlmStateKey(nodeName);
        Map<String, LlmEventState> states = getLlmStates(context);
        LlmEventState state = states.get(key);
        if (state != null) {
            return state;
        }
        state = new LlmEventState(UUIDGenerator.generate(), System.currentTimeMillis(), promptCode);
        states.put(key, state);
        return state;
    }

    /**
     * 移除并返回指定 LLM 状态。
     */
    private LlmEventState removeLlmState(AgentContext context, String nodeName, String promptCode) {
        Map<String, LlmEventState> states = getLlmStates(context);
        LlmEventState state = states.remove(buildLlmStateKey(nodeName));
        if (states.isEmpty()) {
            context.removeAttribute(ATTR_LLM_EVENT_STATES);
        }
        return state == null
                ? new LlmEventState(UUIDGenerator.generate(), System.currentTimeMillis(), promptCode)
                : state;
    }

    private String buildLlmStateKey(String nodeName) {
        return safeText(nodeName, "llm-node");
    }

    /**
     * 获取 Tool 状态集合。
     *
     * @param context 运行上下文
     * @return Tool 状态集合
     */
    @SuppressWarnings("unchecked")
    private Map<String, CompleteEventState> getToolStates(AgentContext context) {
        Object rawStates = context.getAttribute(ATTR_TOOL_EVENT_STATES);
        if (rawStates instanceof Map<?, ?>) {
            return (Map<String, CompleteEventState>) rawStates;
        }
        Map<String, CompleteEventState> states = new ConcurrentHashMap<>();
        context.putAttribute(ATTR_TOOL_EVENT_STATES, states);
        return states;
    }

    /**
     * 获取指定 Tool 状态，不存在时创建。
     *
     * @param context 运行上下文
     * @param nodeName 节点名称
     * @param toolName 工具名称
     * @return Tool 状态
     */
    private CompleteEventState getToolState(AgentContext context, String nodeName, String toolName) {
        String key = buildToolStateKey(nodeName, toolName);
        Map<String, CompleteEventState> states = getToolStates(context);
        CompleteEventState state = states.get(key);
        if (state != null) {
            return state;
        }
        state = new CompleteEventState(
                UUIDGenerator.generate(),
                System.currentTimeMillis(),
                buildToolInput(null)
        );
        states.put(key, state);
        return state;
    }

    /**
     * 移除并返回指定 Tool 状态。
     *
     * @param context 运行上下文
     * @param nodeName 节点名称
     * @param toolName 工具名称
     * @return Tool 状态
     */
    private CompleteEventState removeToolState(AgentContext context, String nodeName, String toolName) {
        Map<String, CompleteEventState> states = getToolStates(context);
        CompleteEventState state = states.remove(buildToolStateKey(nodeName, toolName));
        if (states.isEmpty()) {
            context.removeAttribute(ATTR_TOOL_EVENT_STATES);
        }
        if (state != null) {
            return state;
        }
        return new CompleteEventState(
                UUIDGenerator.generate(),
                System.currentTimeMillis(),
                buildToolInput(null)
        );
    }

    /**
     * 构造 Tool 状态键。
     *
     * @param nodeName 节点名称
     * @param toolName 工具名称
     * @return 状态键
     */
    private String buildToolStateKey(String nodeName, String toolName) {
        return safeText(nodeName, "tool-node") + "::" + safeText(toolName, "tool");
    }

    /**
     * 构建统一错误结构。
     *
     * @param code 错误码
     * @param message 错误信息
     * @return 错误结构
     */
    private Map<String, Object> buildErrorData(String code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("errorCode", code);
        error.put("errorCategory", "SYSTEM");
        error.put("retryable", Boolean.FALSE);
        error.put("errorArgs", new LinkedHashMap<>());
        error.put("errorMessage", message);
        error.put("code", code);
        error.put("message", message);
        return error;
    }

    /**
     * 按优先级读取载荷字段。
     *
     * @param payload 载荷
     * @param keys 字段列表
     * @return 首个非空值
     */
    private Object firstValue(Map<String, Object> payload, String... keys) {
        if (payload == null || payload.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key != null && payload.get(key) != null) {
                return payload.get(key);
            }
        }
        return null;
    }

    /**
     * 计算执行耗时。
     *
     * @param startedAt 开始毫秒时间戳
     * @return 非负耗时
     */
    private long elapsedMillis(long startedAt) {
        return Math.max(0L, System.currentTimeMillis() - startedAt);
    }

    /**
     * 构造 llm buffer key。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @return 缓冲键
     */
    public String buildLlmBufferKey(AgentContext context, String nodeName) {
        if (context.getRunId() != null && !context.getRunId().isBlank()) {
            return "agent:run:" + context.getRunId() + ":llm:" + nodeName;
        }
        return "agent:message:" + context.getMessageId() + ":llm:" + nodeName;
    }

    /**
     * 读取 llm buffer 全文。
     *
     * @param bufferKey 缓冲键
     * @return 缓冲全文
     */
    public String readBuffer(String bufferKey) {
        StringBuffer buffer = this.llmBuffers.get(bufferKey);
        return buffer == null ? "" : buffer.toString();
    }

    /**
     * 清理 llm buffer。
     *
     * @param bufferKey 缓冲键
     */
    public void clearBuffer(String bufferKey) {
        this.llmBuffers.remove(bufferKey);
    }

    /**
     * 仅发送 SSE 事件。
     *
     * @param context 运行上下文
     * @param eventName 事件名
     * @param nodeKind 节点类型
     * @param nodeName 节点名
     * @param content 主体内容
     * @param data 扩展数据
     */
    private void sendOnly(AgentContext context,
                          String eventName,
                          NodeKind nodeKind,
                          String nodeName,
                          String content,
                          Object data) {
        SsePayload payload = new SsePayload();
        payload.setEvent(eventName);
        payload.setType(nodeKind == null ? null : nodeKind.name().toLowerCase());
        payload.setName(nodeName);
        payload.setContent(content);
        payload.setData(data);
        this.sseConnectionManager.send(context.getSseConnectionKey(), eventName, payload);
    }

    /**
     * 仅发送精简版 SSE 事件。
     */
    private void sendOnlyCompact(AgentContext context,
                                 String eventName,
                                 NodeKind nodeKind,
                                 String nodeName,
                                 String content,
                                 Integer status,
                                 Object data) {
        SsePayload payload = new SsePayload();
        payload.setEvent(eventName);
        payload.setType(nodeKind == null ? null : nodeKind.name().toLowerCase());
        payload.setName(nodeName);
        payload.setContent(content);
        payload.setStatus(status);
        if (data != null) {
            applyCompactSseData(payload, nodeKind, data);
        }
        this.sseConnectionManager.send(context.getSseConnectionKey(), eventName, payload);
    }

    /**
     * 仅发送自定义类型的精简 SSE 事件。
     *
     * @param context 运行上下文
     * @param eventName 事件名
     * @param type 事件类型
     * @param nodeName 节点名称
     * @param content 展示内容
     * @param status 状态
     */
    private void sendOnlyCustomCompact(AgentContext context,
                                       String eventName,
                                       String type,
                                       String nodeName,
                                       String content,
                                       Integer status) {
        sendOnlyCustomCompact(context, eventName, type, nodeName, content, status, null);
    }

    private void sendOnlyCustomCompact(AgentContext context,
                                       String eventName,
                                       String type,
                                       String nodeName,
                                       String content,
                                       Integer status,
                                       Object data) {
        SsePayload payload = new SsePayload();
        payload.setEvent(eventName);
        payload.setType(type);
        payload.setName(nodeName);
        payload.setContent(content);
        payload.setStatus(status);
        if (data != null) {
            payload.setData(data);
        }
        this.sseConnectionManager.send(context.getSseConnectionKey(), eventName, payload);
    }

    private void sendOptionPromptRaw(AgentContext context,
                                     String eventName,
                                     Map<String, Object> payload) {
        if (context == null) {
            return;
        }
        this.sseConnectionManager.sendRaw(context.getSseConnectionKey(), eventName, payload);
    }

    @SuppressWarnings("unchecked")
    private void applyCompactSseData(SsePayload payload, NodeKind nodeKind, Object sseData) {
        if (payload == null) {
            return;
        }
        if (nodeKind == NodeKind.TOOL && sseData instanceof Map<?, ?> rawMap) {
            Map<String, Object> data = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null) {
                    data.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            payload.setToolName(stringValue(data.get("toolName")));
            payload.setEventId(stringValue(data.get("eventId")));
            payload.setTaskId(stringValue(data.get("taskId")));
            payload.setAsync(booleanValue(data.get("async")));
            payload.setContentType(stringValue(data.get("contentType")));
            payload.setResourceType(stringValue(data.get("resourceType")));
            payload.setImageUrl(stringValue(data.get("imageUrl")));
            payload.setPromptCode(stringValue(data.get("promptCode")));
            payload.setPromptVersion(stringValue(data.get("promptVersion")));
            payload.setResult(data.get("result"));
            payload.setError(stringValue(data.get("error")));
            payload.setQuestion(stringValue(data.get("question")));
            payload.setInteractionId(stringValue(data.get("interactionId")));
            payload.setOptions(optionListValue(data.get("options")));
            return;
        }
        payload.setData(sseData);
    }

    private boolean isInternalTaskToolEvent(NodeKind nodeKind, Map<String, Object> dbData) {
        if (nodeKind != NodeKind.TOOL || dbData == null) {
            return false;
        }
        return INTERNAL_TASK_TOOL.equalsIgnoreCase(oConvertUtils.getString(dbData.get("toolName")));
    }

    /**
     * 构建工具事件的精简 SSE 数据。
     *
     * @param eventName 事件名
     * @param nodeName 节点名
     * @param toolName 工具名
     * @param content 展示内容
     * @param status 状态值
     * @param payload 原始载荷
     * @param rawContent 原始结果文本
     * @return 精简数据
     */
    private Map<String, Object> buildCompactToolEventData(String eventName,
                                                          String toolName,
                                                          String content,
                                                          Integer status,
                                                          Map<String, Object> payload,
                                                          String rawContent) {
        Map<String, Object> data = new LinkedHashMap<>();
        putString(data, "toolName", toolName);
        putString(data, "eventId", stringValue(payload == null ? null : payload.get("eventId")));
        putString(data, "taskId", stringValue(payload == null ? null : payload.get("taskId")));
        if (payload != null && payload.get("async") != null) {
            data.put("async", booleanValue(payload.get("async")));
        }
        if ("tool.start".equals(eventName)) {
            String contentType = stringValue(payload == null ? null : payload.get("contentType"));
            putString(data, "contentType", oConvertUtils.isNotEmpty(contentType) ? contentType : "progress");
        } else if ("tool.error".equals(eventName)) {
            putString(data, "contentType", "error");
            String error = buildToolResultPreview(payload, rawContent);
            if (!sameText(error, content)) {
                putString(data, "error", error);
            }
        } else if (status != null) {
            data.put("status", status);
            appendToolInteraction(data, payload);
            String contentType = data.containsKey("options")
                    ? "options"
                    : resolveToolContentType(payload, rawContent);
            putString(data, "contentType", contentType);
            if ("image".equalsIgnoreCase(contentType)) {
                appendImageFields(data, payload);
            } else {
                String preview = buildToolResultPreview(payload, rawContent);
                if (!sameText(preview, content)) {
                    putString(data, "result", preview);
                }
            }
        }
        return data;
    }

    private boolean isImageToolPayload(Map<String, Object> payload) {
        return "image".equalsIgnoreCase(stringValue(payload == null ? null : payload.get("contentType")));
    }

    private void appendImageFields(Map<String, Object> target, Map<String, Object> payload) {
        if (target == null || payload == null) {
            return;
        }
        putString(target, "contentType", stringValue(payload.get("contentType")));
        putString(target, "resourceType", stringValue(payload.get("resourceType")));
        putString(target, "imageUrl", stringValue(payload.get("imageUrl")));
        putString(target, "promptCode", stringValue(payload.get("promptCode")));
        putString(target, "promptVersion", stringValue(payload.get("promptVersion")));
    }

    private void appendToolInteraction(Map<String, Object> target, Map<String, Object> payload) {
        if (target == null || payload == null || payload.isEmpty()) {
            return;
        }
        Object toolData = payload.get("toolData");
        if (!(toolData instanceof Map<?, ?> rawMap)) {
            return;
        }
        putString(target, "question", stringValue(rawMap.get("question")));
        putString(target, "interactionId", stringValue(rawMap.get("interactionId")));
        List<Map<String, String>> options = optionListValue(rawMap.get("options"));
        if (options != null && !options.isEmpty()) {
            target.put("options", options);
        }
    }

    private List<Map<String, String>> optionListValue(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return null;
        }
        List<Map<String, String>> values = new ArrayList<>();
        for (Object item : iterable) {
            Map<String, String> option = normalizeOption(item);
            if (option != null) {
                values.add(option);
            }
        }
        return values.isEmpty() ? null : values;
    }

    private Map<String, String> normalizeOption(Object item) {
        if (item instanceof Map<?, ?> rawMap) {
            String label = firstOptionValue(rawMap, "label", "text", "name");
            String value = firstOptionValue(rawMap, "value", "optionValue", "action");
            if (label == null) {
                label = value;
            }
            if (value == null) {
                value = label;
            }
            if (label == null) {
                return null;
            }
            Map<String, String> option = new LinkedHashMap<>();
            option.put("label", label);
            option.put("value", value);
            return option;
        }
        String text = stringValue(item);
        if (text == null) {
            return null;
        }
        Map<String, String> option = new LinkedHashMap<>();
        option.put("label", text);
        option.put("value", text);
        return option;
    }

    private String firstOptionValue(Map<?, ?> source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = stringValue(source.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 构建 Agent 控制流事件的精简数据。
     */
    private Map<String, Object> buildCompactAgentEventData(AgentContext context,
                                                           String agentName,
                                                           String error,
                                                           String status,
                                                           AgentResult result) {
        Map<String, Object> data = new LinkedHashMap<>();
        putString(data, "agentName", agentName);
        putString(data, "status", status);
        putString(data, "runId", context == null ? null : context.getRunId());
        if (context != null && context.getSessionId() != null) {
            data.put("sessionId", context.getSessionId());
        }
        if (error != null) {
            putString(data, "error", error);
        }
        if (result != null && result.getData() != null) {
            copyIfPresent(data, result.getData(), "errorCode");
            copyIfPresent(data, result.getData(), "errorCategory");
            copyIfPresent(data, result.getData(), "retryable");
            copyIfPresent(data, result.getData(), "errorArgs");
            copyIfPresent(data, result.getData(), "message");
        }
        return data;
    }

    private void copyIfPresent(Map<String, Object> target,
                               Map<String, Object> source,
                               String key) {
        if (target == null || source == null || key == null || source.get(key) == null) {
            return;
        }
        target.put(key, source.get(key));
    }

    /**
     * 判断工具结果的内容类型。
     *
     * @param payload 原始载荷
     * @param rawContent 原始结果文本
     * @return 内容类型
     */
    private String resolveToolContentType(Map<String, Object> payload, String rawContent) {
        if (payload == null || payload.isEmpty()) {
            if (looksLikeJson(rawContent)) {
                return "json";
            }
            return "text";
        }
        String explicitContentType = stringValue(payload.get("contentType"));
        if (explicitContentType != null) {
            return explicitContentType;
        }
        if (hasAnyKey(payload, "imageUrl", "imageUrls", "image_urls", "snapshotKey")) {
            return "image";
        }
        if (hasAnyKey(payload, "resultJson", "structuredResult")) {
            return "json";
        }
        Object result = payload.get("result");
        if (result != null && !(result instanceof String)) {
            return "json";
        }
        if (result instanceof String text && looksLikeJson(text)) {
            return "json";
        }
        if (looksLikeJson(rawContent)) {
            return "json";
        }
        return "text";
    }

    /**
     * 构建工具结果预览文本。
     *
     * @param payload 原始载荷
     * @param rawContent 原始结果文本
     * @return 预览文本
     */
    private String buildToolResultPreview(Map<String, Object> payload, String rawContent) {
        String preview = previewValue(payload == null ? null : payload.get("formattedResult"));
        if (preview == null) {
            preview = previewValue(payload == null ? null : payload.get("resultJson"));
        }
        if (preview == null) {
            preview = previewValue(payload == null ? null : payload.get("result"));
        }
        if (preview == null) {
            preview = previewValue(payload == null ? null : payload.get("structuredResult"));
        }
        if (preview == null) {
            preview = previewValue(payload == null ? null : payload.get("content"));
        }
        if (preview == null) {
            preview = rawContent;
        }
        if (preview == null && payload != null && !payload.isEmpty()) {
            preview = JSONObject.toJSONString(payload);
        }
        if (preview == null) {
            return null;
        }
        return truncateText(preview, 400);
    }

    /**
     * 生成 agent.end 的展示文本。
     */
    private String buildAgentEndContent(AgentResult result) {
        if (result == null || result.getStatus() == null) {
            return "Execution finished";
        }
        return switch (result.getStatus()) {
            case SUCCESS -> "Completed";
            case FAILED -> "Failed";
            case INTERRUPTED -> "Interrupted";
            case WAITING_USER -> "Waiting for user input";
            case HANDOFF -> "Handed off to the main agent";
        };
    }

    /**
     * 生成 subagent.end 的展示文本。
     */
    private String buildSubAgentEndContent(AgentResult result) {
        if (result == null || result.getStatus() == null) {
            return "Sub-agent execution finished";
        }
        String base = switch (result.getStatus()) {
            case SUCCESS -> "Sub-agent completed";
            case FAILED -> "Sub-agent failed";
            case INTERRUPTED -> "Sub-agent interrupted";
            case WAITING_USER -> "Sub-agent is waiting for user input";
            case HANDOFF -> "Sub-agent handed off to the main agent";
        };
        return base;
    }

    /**
     * 转换 AgentResult 为事件状态码。
     */
    private int eventStatus(AgentResult result) {
        if (result == null || result.getStatus() == null) {
            return 0;
        }
        if (result.getStatus() == AgentResult.Status.FAILED) {
            return 0;
        }
        return result.getStatus() == AgentResult.Status.INTERRUPTED ? 3 : 1;
    }

    /**
     * 判断是否存在任意键。
     */
    private boolean hasAnyKey(Map<String, Object> payload, String... keys) {
        if (payload == null || payload.isEmpty() || keys == null) {
            return false;
        }
        for (String key : keys) {
            if (key != null && payload.containsKey(key) && payload.get(key) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文本是否像 JSON。
     */
    private boolean looksLikeJson(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    /**
     * 截断文本。
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    /**
     * 将预览值转为适合展示的文本。
     */
    private String previewValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return truncateText(text, 400);
        }
        try {
            return truncateText(JSONObject.toJSONString(value), 400);
        } catch (Exception ex) {
            String text = String.valueOf(value);
            return text == null ? null : truncateText(text, 400);
        }
    }

    /**
     * 追加 llm 增量文本到进程内缓冲。
     *
     * @param bufferKey 缓冲键
     * @param delta 增量文本
     */
    private void appendBuffer(String bufferKey, String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        this.llmBuffers.computeIfAbsent(bufferKey, key -> new StringBuffer()).append(delta);
    }

    /**
     * 从 payload 中提取可展示文本。
     *
     * @param payload 扩展数据
     * @param keys 优先级字段
     * @return 文本
     */
    private String extractText(Map<String, Object> payload, String... keys) {
        if (payload == null || payload.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            String text = stringValue(payload.get(key));
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    /**
     * 合并扩展数据。
     *
     * @param source 原始扩展数据
     * @param extra 额外扩展数据
     * @return 合并结果
     */
    private Map<String, Object> mergePayload(Map<String, Object> source, Map<String, Object> extra) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (source != null) {
            merged.putAll(source);
        }
        if (extra != null) {
            merged.putAll(extra);
        }
        return merged;
    }

    /**
     * 安全字符串。
     *
     * @param value 值
     * @param fallback 默认值
     * @return 字符串
     */
    private String safeText(String value, String fallback) {
        String text = stringValue(value);
        return text == null ? fallback : text;
    }

    /**
     * 规范字符串值。
     *
     * @param value 输入值
     * @return 规范化结果
     */
    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = stringValue(value);
        return text == null ? null : Boolean.valueOf(text);
    }

    /**
     * 判断两段文本规范化后是否一致。
     *
     * @param left 左文本
     * @param right 右文本
     * @return 是否一致
     */
    private boolean sameText(String left, String right) {
        String safeLeft = stringValue(left);
        String safeRight = stringValue(right);
        if (safeLeft == null) {
            return safeRight == null;
        }
        return safeLeft.equals(safeRight);
    }

    /**
     * 写入字符串字段。
     *
     * @param target 目标 Map
     * @param key 键
     * @param value 值
     */
    private void putString(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private void putNumber(Map<String, Object> target, String key, Number value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        target.put(key, value);
    }

    /**
     * 补充当前运行上下文字段。
     *
     * @param data 事件数据
     * @param context 运行上下文
     */
    private void fillContextFields(Map<String, Object> data, AgentContext context) {
        if (data == null || context == null) {
            return;
        }
        putString(data, "messageId", eventMessageId(context));
        if (context.getSessionId() != null) {
            data.put("sessionId", context.getSessionId());
        }
        if (context.getAgentSessionId() != null) {
            data.put("agentSessionId", context.getAgentSessionId());
        }
        putString(data, "runId", context.getRunId());
        putString(data, "traceId", context.getTraceId());
        putString(data, "parentRunId", context.getParentRunId());
        putString(data, "turnId", context.getTurnId());
        putString(data, "senderType", context.getSenderType());
        putString(data, "agentCode", context.getAgentCode());
    }

    /**
     * 获取事件所属助手消息ID，兼容未设置独立事件消息ID的调用场景。
     */
    private String eventMessageId(AgentContext context) {
        if (context == null) {
            return null;
        }
        return safeText(context.getEventMessageId(), context.getMessageId());
    }

    /**
     * 将事件追加到上下文轨迹中。
     *
     * @param context 运行上下文
     * @param eventData 事件数据
     */
    private void recordEventTrail(AgentContext context, Map<String, Object> eventData) {
        if (context == null || eventData == null || eventData.isEmpty()) {
            return;
        }
        context.appendEvent(eventData);
    }

    /**
     * 生成展示摘要。
     *
     * @param value 原始文本
     * @return 摘要文本
     */
    private String summarize(String value) {
        String text = stringValue(value);
        if (text == null) {
            return null;
        }
        if (text.length() <= SUMMARY_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, SUMMARY_MAX_LENGTH) + "...";
    }

    /**
     * 一次完整 Task/Tool 执行的暂存状态。
     */
    private static final class CompleteEventState {
        private final String eventId;
        private final long startedAt;
        private final Map<String, Object> input;
        private Map<String, Object> error;

        private CompleteEventState(String eventId,
                                   long startedAt,
                                   Map<String, Object> input) {
            this.eventId = eventId;
            this.startedAt = startedAt;
            this.input = input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
        }

        private String eventId() {
            return this.eventId;
        }

        private long startedAt() {
            return this.startedAt;
        }

        private Map<String, Object> input() {
            return this.input;
        }

        private Map<String, Object> error() {
            return this.error;
        }

        private void setError(Map<String, Object> error) {
            this.error = error == null ? null : new LinkedHashMap<>(error);
        }
    }

    /**
     * LLM 事件暂存模型、执行结果和 Token 指标。
     */
    private static final class LlmEventState {
        private final String eventId;
        private final long startedAt;
        private final String promptCode;
        private String modelId;
        private String provider;
        private String modelName;
        private String finishReason;
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer totalTokens;
        private Map<String, Object> error;
        private boolean persistedSegment;

        private LlmEventState(String eventId, long startedAt, String promptCode) {
            this.eventId = eventId;
            this.startedAt = startedAt;
            this.promptCode = promptCode;
        }

        private void updateModel(String modelId, String provider, String modelName) {
            if (modelId != null && !modelId.isBlank()) {
                this.modelId = modelId;
            }
            if (provider != null && !provider.isBlank()) {
                this.provider = provider;
            }
            if (modelName != null && !modelName.isBlank()) {
                this.modelName = modelName;
            }
        }

        private void updateResult(String finishReason,
                                  Integer inputTokens,
                                  Integer outputTokens,
                                  Integer totalTokens) {
            if (finishReason != null && !finishReason.isBlank()) {
                this.finishReason = finishReason;
            }
            if (inputTokens != null) {
                this.inputTokens = inputTokens;
            }
            if (outputTokens != null) {
                this.outputTokens = outputTokens;
            }
            if (totalTokens != null) {
                this.totalTokens = totalTokens;
            }
        }

        private boolean hasPersistedSegment() {
            return this.persistedSegment;
        }

        private void markSegmentPersisted() {
            this.persistedSegment = true;
        }

        private String eventId() {
            return this.eventId;
        }

        private long startedAt() {
            return this.startedAt;
        }

        private String promptCode() {
            return this.promptCode;
        }

        private String modelId() {
            return this.modelId;
        }

        private String provider() {
            return this.provider;
        }

        private String modelName() {
            return this.modelName;
        }

        private String finishReason() {
            return this.finishReason;
        }

        private Integer inputTokens() {
            return this.inputTokens;
        }

        private Integer outputTokens() {
            return this.outputTokens;
        }

        private Integer totalTokens() {
            return this.totalTokens;
        }

        private Map<String, Object> error() {
            return this.error;
        }

        private void setError(Map<String, Object> error) {
            this.error = error == null ? null : new LinkedHashMap<>(error);
        }
    }
}
