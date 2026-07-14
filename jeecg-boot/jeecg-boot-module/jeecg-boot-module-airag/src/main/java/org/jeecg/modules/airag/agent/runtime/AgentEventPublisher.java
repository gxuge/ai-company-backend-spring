package org.jeecg.modules.airag.agent.runtime;

import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.sse.SseConnectionManager;
import org.jeecg.modules.airag.agent.sse.SsePayload;
import org.jeecg.modules.airag.agent.service.TsAgentChatMessageEventService;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
     * Redis 缓存过期时间，单位分钟。
     */
    private static final long BUFFER_EXPIRE_MINUTES = 30L;

    /**
     * 展示摘要最大长度。
     */
    private static final int SUMMARY_MAX_LENGTH = 200;

    /**
     * 当前 SubAgent 完整事件暂存键。
     */
    private static final String ATTR_SUBAGENT_EVENT_STATE = AgentEventPublisher.class.getName() + ".subagentEventState";

    /**
     * 当前 Tool 完整事件暂存集合键。
     */
    private static final String ATTR_TOOL_EVENT_STATES = AgentEventPublisher.class.getName() + ".toolEventStates";

    /**
     * 事件落库服务。
     */
    private final TsAgentChatMessageEventService eventService;
    /**
     * SSE 管理器。
     */
    private final SseConnectionManager sseConnectionManager;
    /**
     * Redis 客户端。
     */
    private final RedisTemplate redisTemplate;

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
                "开始执行 " + safeText(agentName, "Agent"),
                null,
                null
        );
        fillContextFields(dbData, context);
        Map<String, Object> sseData = buildCompactAgentEventData(context, agentName, null, "running", null);
        recordEventTrail(context, dbData);
        sendOnlyCompact(context, "agent.start", null, agentName, "开始执行 " + safeText(agentName, "Agent"), 2, sseData);
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
        CompleteEventState state = new CompleteEventState(
                UUIDGenerator.generate(),
                System.currentTimeMillis(),
                buildSubAgentInput(context, payload),
                null
        );
        context.putAttribute(ATTR_SUBAGENT_EVENT_STATE, state);
        Map<String, Object> dbData = buildCustomEventData(
                "subagent.start",
                "subagent",
                subAgentName,
                "开始执行 " + safeText(subAgentName, "SubAgent"),
                2,
                payload
        );
        recordEventTrail(context, dbData);
        sendOnlyCustomCompact(context, "subagent.start", "subagent", subAgentName,
                "开始执行 " + safeText(subAgentName, "SubAgent"), 2);
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
        Map<String, Object> mergedPayload = new LinkedHashMap<>();
        if (result != null && result.getData() != null) {
            mergedPayload.putAll(result.getData());
        }
        if (payload != null && !payload.isEmpty()) {
            mergedPayload.putAll(payload);
        }
        Map<String, Object> dbData = buildCustomEventData(
                "subagent.end",
                "subagent",
                subAgentName,
                result == null ? null : result.getContent(),
                result == null || result.getStatus() == null ? 0 : eventStatus(result),
                mergedPayload
        );
        recordEventTrail(context, dbData);
        CompleteEventState state = getSubAgentState(context);
        context.removeAttribute(ATTR_SUBAGENT_EVENT_STATE);
        Map<String, Object> completeData = buildCompleteSubAgentData(context, result, payload, state);
        fillContextFields(completeData, context);
        String persistedName = safeText(context == null ? null : context.getAgentCode(), subAgentName);
        this.eventService.saveEvent(
                state.eventId(),
                context.getMessageId(),
                context.getSessionId(),
                context.getAgentSessionId(),
                "subagent",
                persistedName,
                resolveSourceNodeName(context, result),
                resolveSourceNodeType(context, result),
                summarize(result == null ? null : result.getContent()),
                result == null || result.getStatus() == null ? 0 : eventStatus(result),
                completeData
        );
        context.setLastCompletedSubAgentEventId(state.eventId());
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
        Map<String, Object> errorPayload = new LinkedHashMap<>();
        if (error != null) {
            errorPayload.put("errorCode", error.getClass().getSimpleName());
            errorPayload.put("errorMessage", error.getMessage());
        }
        Map<String, Object> mergedPayload = mergePayload(payload, errorPayload);
        Map<String, Object> dbData = buildCustomEventData(
                "subagent.error",
                "subagent",
                subAgentName,
                error == null ? "SubAgent step failed" : error.getMessage(),
                0,
                mergedPayload
        );
        recordEventTrail(context, dbData);
        CompleteEventState state = getSubAgentState(context);
        state.setError(buildErrorData(
                error == null ? "SUBAGENT_EXECUTION_ERROR" : error.getClass().getSimpleName(),
                error == null ? "SubAgent step failed" : error.getMessage()
        ));
        sendOnlyCustomCompact(context, "subagent.error", "subagent", subAgentName,
                error == null ? "SubAgent step failed" : error.getMessage(), 0);
    }

    /**
     * 发送 llm.start，不写入事件表。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param promptCode 模板编码
     */
    public void publishLlmStart(AgentContext context, String nodeName, String promptCode) {
        Map<String, Object> dbData = buildEventData(
                "llm.start",
                NodeKind.LLM,
                nodeName,
                promptCode,
                null,
                null,
                "开始生成",
                2,
                null
        );
        recordEventTrail(context, dbData);
        sendOnlyCompact(context, "llm.start", NodeKind.LLM, nodeName, "开始生成", 2, null);
    }

    /**
     * 发送 llm.delta，并写入 Redis buffer。
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
     * 发送 llm.error，不写入事件表。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param promptCode 模板编码
     * @param error 错误对象
     */
    public void publishLlmError(AgentContext context, String nodeName, String promptCode, Throwable error) {
        String errorText = error == null ? "LLM step failed" : error.getMessage();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (error != null) {
            payload.put("errorCode", error.getClass().getSimpleName());
            payload.put("errorMessage", error.getMessage());
        }
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
        sendOnlyCompact(context, "llm.error", NodeKind.LLM, nodeName, errorText, 0, null);
    }

    /**
     * 发送 llm.end，不写入事件表。
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
        String bufferKey = buildLlmBufferKey(context, nodeName);
        String content = readBuffer(bufferKey);
        if (content == null || content.isBlank()) {
            content = extractText(payload, "reply", "content", "rawText", "text", "summary");
        }
        if (content == null || content.isBlank()) {
            content = context.getLatestContent();
        }
        clearBuffer(bufferKey);
        Map<String, Object> dbData = buildEventData(
                "llm.end",
                NodeKind.LLM,
                nodeName,
                promptCode,
                null,
                null,
                content,
                success ? 1 : 0,
                payload
        );
        recordEventTrail(context, dbData);
        sendOnlyCompact(context, "llm.end", NodeKind.LLM, nodeName, content, success ? 1 : 0, null);
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
        CompleteEventState subAgentState = context.getAttribute(ATTR_SUBAGENT_EVENT_STATE, CompleteEventState.class);
        CompleteEventState toolState = new CompleteEventState(
                UUIDGenerator.generate(),
                System.currentTimeMillis(),
                buildToolInput(payload),
                subAgentState == null ? null : subAgentState.eventId()
        );
        getToolStates(context).put(buildToolStateKey(nodeName, toolName), toolState);
        String content = "开始调用 " + safeText(toolName, "Tool");
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
        Map<String, Object> errorPayload = new LinkedHashMap<>();
        if (error != null) {
            errorPayload.put("errorCode", error.getClass().getSimpleName());
            errorPayload.put("errorMessage", error.getMessage());
        }
        Map<String, Object> dbData = buildEventData(
                "tool.error",
                NodeKind.TOOL,
                nodeName,
                null,
                toolName,
                null,
                error == null ? "Tool step failed" : error.getMessage(),
                0,
                mergePayload(payload, errorPayload)
        );
        Map<String, Object> sseData = buildCompactToolEventData(
                "tool.error",
                toolName,
                error == null ? "Tool step failed" : error.getMessage(),
                0,
                mergePayload(payload, errorPayload),
                null
        );
        recordEventTrail(context, dbData);
        CompleteEventState state = getToolState(context, nodeName, toolName);
        state.setError(buildErrorData(
                error == null ? "TOOL_EXECUTION_ERROR" : error.getClass().getSimpleName(),
                error == null ? "Tool step failed" : error.getMessage()
        ));
        if (!isInternalTaskToolEvent(NodeKind.TOOL, dbData)) {
            sendOnlyCompact(context, "tool.error", NodeKind.TOOL, nodeName,
                    error == null ? "Tool step failed" : error.getMessage(), 0, sseData);
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
        String summary = safeText(summarize(content), "调用完成 " + safeText(toolName, "Tool"));
        Map<String, Object> dbData = buildEventData(
                "tool.end",
                NodeKind.TOOL,
                nodeName,
                null,
                toolName,
                null,
                summary,
                success ? 1 : 0,
                payload
        );
        Map<String, Object> sseData = buildCompactToolEventData(
                "tool.end",
                toolName,
                summary,
                success ? 1 : 0,
                payload,
                content
        );
        recordEventTrail(context, dbData);
        CompleteEventState state = removeToolState(context, nodeName, toolName);
        if (isInternalTaskToolEvent(NodeKind.TOOL, dbData)) {
            return;
        }
        Map<String, Object> completeData = buildCompleteToolData(success, content, payload, state);
        fillContextFields(completeData, context);
        if (state.parentEventId() != null) {
            completeData.put("parentEventId", state.parentEventId());
        }
        this.eventService.saveEvent(
                state.eventId(),
                context.getMessageId(),
                context.getSessionId(),
                context.getAgentSessionId(),
                "tool",
                safeText(toolName, nodeName),
                nodeName,
                NodeKind.TOOL.name().toLowerCase(),
                summarize(summary),
                success ? 1 : 0,
                completeData
        );
        sendOnlyCompact(context, "tool.end", NodeKind.TOOL, nodeName, summary, success ? 1 : 0, sseData);
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

    /**
     * 构建 SubAgent Task 输入。
     *
     * @param context 运行上下文
     * @param payload Handoff 扩展上下文
     * @return 固定结构输入
     */
    private Map<String, Object> buildSubAgentInput(AgentContext context, Map<String, Object> payload) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("userInput", context == null ? null : context.getUserInput());
        Object taskDescription = context == null ? null : context.getAttribute("taskDescription");
        input.put("taskDescription", taskDescription == null && context != null ? context.getUserInput() : taskDescription);
        input.put("handoffContext", payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload));
        return input;
    }

    /**
     * 构建完整 SubAgent Task 数据。
     *
     * @param context 运行上下文
     * @param result Agent 最终结果
     * @param payload 结束扩展数据
     * @param state 起始状态
     * @return 固定结构完整数据
     */
    private Map<String, Object> buildCompleteSubAgentData(AgentContext context,
                                                          AgentResult result,
                                                          Map<String, Object> payload,
                                                          CompleteEventState state) {
        Map<String, Object> complete = new LinkedHashMap<>();
        complete.put("input", new LinkedHashMap<>(state.input()));

        boolean failed = result == null || result.getStatus() == null || result.getStatus() == AgentResult.Status.FAILED;
        if (failed) {
            complete.put("output", null);
        } else {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("content", result.getContent());
            Object structuredResult = result.getStructuredResult();
            if (structuredResult == null && result.getData() != null && !result.getData().isEmpty()) {
                structuredResult = new LinkedHashMap<>(result.getData());
            }
            if (structuredResult == null && payload != null && !payload.isEmpty()) {
                structuredResult = new LinkedHashMap<>(payload);
            }
            output.put("structuredResult", structuredResult);
            complete.put("output", output);
        }

        Map<String, Object> error = state.error();
        if (failed && error == null) {
            String errorCode = result == null || result.getData() == null
                    ? null
                    : stringValue(result.getData().get("errorCode"));
            String errorMessage = result == null ? null : stringValue(result.getError());
            if (errorMessage == null && result != null) {
                errorMessage = stringValue(result.getContent());
            }
            error = buildErrorData(safeText(errorCode, "SUBAGENT_EXECUTION_ERROR"),
                    safeText(errorMessage, "SubAgent execution failed"));
        }
        complete.put("error", error);

        Map<String, Object> metrics = new LinkedHashMap<>();
        Object stepIndex = context == null ? null : context.getAttribute("agentStepIndex");
        if (stepIndex != null) {
            metrics.put("stepIndex", stepIndex);
        }
        metrics.put("durationMs", elapsedMillis(state.startedAt()));
        complete.put("metrics", metrics);
        return complete;
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
            Object result = firstValue(payload, "toolData", "resultJson", "structuredResult", "result", "toolPayload");
            output.put("result", result == null ? content : result);
            output.put("summary", content);
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

    /**
     * 获取当前 SubAgent Task 状态。
     *
     * @param context 运行上下文
     * @return Task 状态
     */
    private CompleteEventState getSubAgentState(AgentContext context) {
        CompleteEventState state = context.getAttribute(ATTR_SUBAGENT_EVENT_STATE, CompleteEventState.class);
        if (state != null) {
            return state;
        }
        state = new CompleteEventState(
                UUIDGenerator.generate(),
                System.currentTimeMillis(),
                buildSubAgentInput(context, null),
                null
        );
        context.putAttribute(ATTR_SUBAGENT_EVENT_STATE, state);
        return state;
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
        CompleteEventState subAgentState = context.getAttribute(ATTR_SUBAGENT_EVENT_STATE, CompleteEventState.class);
        state = new CompleteEventState(
                UUIDGenerator.generate(),
                System.currentTimeMillis(),
                buildToolInput(null),
                subAgentState == null ? null : subAgentState.eventId()
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
        CompleteEventState subAgentState = context.getAttribute(ATTR_SUBAGENT_EVENT_STATE, CompleteEventState.class);
        return new CompleteEventState(
                UUIDGenerator.generate(),
                System.currentTimeMillis(),
                buildToolInput(null),
                subAgentState == null ? null : subAgentState.eventId()
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
     * @return Redis key
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
     * @param bufferKey Redis key
     * @return 缓冲全文
     */
    public String readBuffer(String bufferKey) {
        Object value = this.redisTemplate.opsForValue().get(bufferKey);
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 清理 llm buffer。
     *
     * @param bufferKey Redis key
     */
    public void clearBuffer(String bufferKey) {
        this.redisTemplate.delete(bufferKey);
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
        if (nodeKind == NodeKind.TOOL) {
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
        SsePayload payload = new SsePayload();
        payload.setEvent(eventName);
        payload.setType(type);
        payload.setName(nodeName);
        payload.setContent(content);
        payload.setStatus(status);
        this.sseConnectionManager.send(context.getSseConnectionKey(), eventName, payload);
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
            payload.setContentType(stringValue(data.get("contentType")));
            payload.setResult(data.get("result"));
            payload.setError(stringValue(data.get("error")));
            payload.setQuestion(stringValue(data.get("question")));
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
        if ("tool.start".equals(eventName)) {
            putString(data, "contentType", "progress");
        } else if ("tool.error".equals(eventName)) {
            putString(data, "contentType", "error");
            String error = buildToolResultPreview(payload, rawContent);
            if (!sameText(error, content)) {
                putString(data, "error", error);
            }
        } else if (status != null) {
            data.put("status", status);
            appendToolInteraction(data, payload);
            putString(data, "contentType", data.containsKey("options")
                    ? "options"
                    : resolveToolContentType(payload, rawContent));
            String preview = buildToolResultPreview(payload, rawContent);
            if (!sameText(preview, content)) {
                putString(data, "result", preview);
            }
        }
        return data;
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
        if (error != null) {
            putString(data, "error", error);
        }
        return data;
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
            return "执行结束";
        }
        return switch (result.getStatus()) {
            case SUCCESS -> "执行完成";
            case FAILED -> "执行失败";
            case WAITING_USER -> "等待用户继续输入";
            case HANDOFF -> "已交还主Agent重新派活";
        };
    }

    /**
     * 生成 subagent.end 的展示文本。
     */
    private String buildSubAgentEndContent(AgentResult result) {
        if (result == null || result.getStatus() == null) {
            return "子Agent执行结束";
        }
        String base = switch (result.getStatus()) {
            case SUCCESS -> "子Agent执行完成";
            case FAILED -> "子Agent执行失败";
            case WAITING_USER -> "子Agent等待用户继续输入";
            case HANDOFF -> "子Agent已交还主Agent";
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
        return result.getStatus() == AgentResult.Status.FAILED ? 0 : 1;
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
     * 追加 llm 增量文本到 Redis。
     *
     * @param bufferKey Redis key
     * @param delta 增量文本
     */
    private void appendBuffer(String bufferKey, String delta) {
        String current = readBuffer(bufferKey);
        this.redisTemplate.opsForValue().set(bufferKey, current + (delta == null ? "" : delta), BUFFER_EXPIRE_MINUTES, TimeUnit.MINUTES);
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
        putString(data, "messageId", context.getMessageId());
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
     * 获取最终产出节点名称，失败场景回退到当前节点。
     *
     * @param context 运行上下文
     * @param result Agent 执行结果
     * @return 节点名称
     */
    private String resolveSourceNodeName(AgentContext context, AgentResult result) {
        if (context == null) {
            return null;
        }
        if (result == null
                || result.getStatus() == null
                || result.getStatus() == AgentResult.Status.FAILED) {
            return safeText(context.getCurrentNodeName(), context.getResultNodeName());
        }
        return safeText(context.getResultNodeName(), context.getCurrentNodeName());
    }

    /**
     * 获取最终产出节点类型，失败场景回退到当前节点类型。
     *
     * @param context 运行上下文
     * @param result Agent 执行结果
     * @return 节点类型
     */
    private String resolveSourceNodeType(AgentContext context, AgentResult result) {
        if (context == null) {
            return null;
        }
        if (result == null
                || result.getStatus() == null
                || result.getStatus() == AgentResult.Status.FAILED) {
            return safeText(context.getCurrentNodeType(), context.getResultNodeType());
        }
        return safeText(context.getResultNodeType(), context.getCurrentNodeType());
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
        private final String parentEventId;
        private Map<String, Object> error;

        private CompleteEventState(String eventId,
                                   long startedAt,
                                   Map<String, Object> input,
                                   String parentEventId) {
            this.eventId = eventId;
            this.startedAt = startedAt;
            this.input = input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
            this.parentEventId = parentEventId;
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

        private String parentEventId() {
            return this.parentEventId;
        }

        private Map<String, Object> error() {
            return this.error;
        }

        private void setError(Map<String, Object> error) {
            this.error = error == null ? null : new LinkedHashMap<>(error);
        }
    }
}
