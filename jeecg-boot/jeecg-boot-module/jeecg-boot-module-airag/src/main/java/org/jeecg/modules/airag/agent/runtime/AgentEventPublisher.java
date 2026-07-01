package org.jeecg.modules.airag.agent.runtime;

import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.sse.SseConnectionManager;
import org.jeecg.modules.airag.agent.sse.SsePayload;
import org.jeecg.modules.airag.agent.service.TsChatMessageEventService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
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
     * Redis 缓存过期时间，单位分钟。
     */
    private static final long BUFFER_EXPIRE_MINUTES = 30L;

    /**
     * 展示摘要最大长度。
     */
    private static final int SUMMARY_MAX_LENGTH = 200;

    /**
     * 事件落库服务。
     */
    private final TsChatMessageEventService eventService;
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
        Map<String, Object> data = buildEventData(
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
        fillContextFields(data, context);
        sendOnly(context, "agent.start", null, agentName, "开始执行 " + safeText(agentName, "Agent"), data);
    }

    /**
     * 发送 agent.end。
     *
     * @param context 运行上下文
     * @param agentName Agent 名称
     * @param result 执行结果
     */
    public void publishAgentEnd(AgentContext context, String agentName, AgentResult result) {
        Map<String, Object> data = buildEventData(
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
        fillContextFields(data, context);
        sendOnly(context, "agent.end", null, agentName, result == null ? null : result.getContent(), data);
    }

    /**
     * 发送子 Agent 开始事件。
     *
     * @param context 运行上下文
     * @param subAgentName 子 Agent 名称
     * @param payload 扩展数据
     */
    public void publishSubAgentStart(AgentContext context, String subAgentName, Map<String, Object> payload) {
        Map<String, Object> data = buildCustomEventData(
                "subagent.start",
                "subagent",
                subAgentName,
                "开始执行 " + safeText(subAgentName, "SubAgent"),
                2,
                payload
        );
        persistAndSendCustom(context, "subagent.start", "subagent", subAgentName,
                "开始执行 " + safeText(subAgentName, "SubAgent"), 2, data);
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
        Map<String, Object> data = buildCustomEventData(
                "subagent.end",
                "subagent",
                subAgentName,
                result == null ? null : result.getContent(),
                result == null || result.getStatus() == null ? 0 : (result.getStatus() == AgentResult.Status.SUCCESS ? 1 : 0),
                mergedPayload
        );
        persistAndSendCustom(context, "subagent.end", "subagent", subAgentName,
                result == null ? null : result.getContent(),
                result == null || result.getStatus() == null ? 0 : (result.getStatus() == AgentResult.Status.SUCCESS ? 1 : 0),
                data);
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
        Map<String, Object> data = buildCustomEventData(
                "subagent.error",
                "subagent",
                subAgentName,
                error == null ? "SubAgent step failed" : error.getMessage(),
                0,
                mergedPayload
        );
        persistAndSendCustom(context, "subagent.error", "subagent", subAgentName,
                error == null ? "SubAgent step failed" : error.getMessage(), 0, data);
    }

    /**
     * 发送并落库 llm.start。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param promptCode 模板编码
     */
    public void publishLlmStart(AgentContext context, String nodeName, String promptCode) {
        Map<String, Object> data = buildEventData(
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
        persistAndSend(context, "llm.start", NodeKind.LLM, nodeName, "开始生成", 2, data);
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
        Map<String, Object> data = buildEventData(
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
        sendOnly(context, "llm.delta", NodeKind.LLM, nodeName, delta, data);
    }

    /**
     * 发送并落库 llm.error。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param promptCode 模板编码
     * @param error 错误对象
     */
    public void publishLlmError(AgentContext context, String nodeName, String promptCode, Throwable error) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (error != null) {
            payload.put("errorCode", error.getClass().getSimpleName());
            payload.put("errorMessage", error.getMessage());
        }
        Map<String, Object> data = buildEventData(
                "llm.error",
                NodeKind.LLM,
                nodeName,
                promptCode,
                null,
                null,
                error == null ? "LLM step failed" : error.getMessage(),
                0,
                payload
        );
        persistAndSend(context, "llm.error", NodeKind.LLM, nodeName, error == null ? "LLM step failed" : error.getMessage(), 0, data);
    }

    /**
     * 发送并落库 llm.end。
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
        clearBuffer(bufferKey);
        Map<String, Object> data = buildEventData(
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
        persistAndSend(context, "llm.end", NodeKind.LLM, nodeName, content, success ? 1 : 0, data);
    }

    /**
     * 发送并落库 tool.start。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param toolName 工具名
     * @param payload 扩展数据
     */
    public void publishToolStart(AgentContext context, String nodeName, String toolName, Map<String, Object> payload) {
        String content = "开始调用 " + safeText(toolName, "Tool");
        Map<String, Object> data = buildEventData(
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
        persistAndSend(context, "tool.start", NodeKind.TOOL, nodeName, content, 2, data);
    }

    /**
     * 发送并落库 tool.error。
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
        Map<String, Object> data = buildEventData(
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
        persistAndSend(context, "tool.error", NodeKind.TOOL, nodeName, error == null ? "Tool step failed" : error.getMessage(), 0, data);
    }

    /**
     * 发送并落库 tool.end。
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
        Map<String, Object> data = buildEventData(
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
        persistAndSend(context, "tool.end", NodeKind.TOOL, nodeName, summary, success ? 1 : 0, data);
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
            putString(data, "routeDecision", stringValue(payload.get("routeDecision")));
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
     * 持久化并发送节点事件。
     *
     * @param context 运行上下文
     * @param eventName 事件名
     * @param nodeKind 节点类型
     * @param nodeName 节点名
     * @param content 主体内容
     * @param status 状态值
     * @param data 扩展数据
     */
    private void persistAndSend(AgentContext context,
                                String eventName,
                                NodeKind nodeKind,
                                String nodeName,
                                String content,
                                Integer status,
                                Map<String, Object> data) {
        if (data != null) {
            data.put("sessionId", context.getSessionId());
            data.put("agentSessionId", context.getAgentSessionId());
            data.put("runId", context.getRunId());
        }
        this.eventService.saveEvent(
                context.getMessageId(),
                context.getSessionId(),
                context.getAgentSessionId(),
                nodeKind.name().toLowerCase(),
                nodeName,
                content,
                status,
                data
        );
        SsePayload payload = new SsePayload();
        payload.setEvent(eventName);
        payload.setType(nodeKind.name().toLowerCase());
        payload.setName(nodeName);
        payload.setContent(content);
        payload.setStatus(status);
        payload.setData(data);
        this.sseConnectionManager.send(context.getSseConnectionKey(), eventName, payload);
        log.debug("[AGENT_EVENT] {}", JSONObject.toJSONString(payload));
    }

    /**
     * 持久化并发送自定义节点事件。
     *
     * @param context 运行上下文
     * @param eventName 事件名
     * @param type 事件类型
     * @param nodeName 节点名
     * @param content 主体内容
     * @param status 状态值
     * @param data 扩展数据
     */
    private void persistAndSendCustom(AgentContext context,
                                      String eventName,
                                      String type,
                                      String nodeName,
                                      String content,
                                      Integer status,
                                      Map<String, Object> data) {
        fillContextFields(data, context);
        this.eventService.saveEvent(
                context.getMessageId(),
                context.getSessionId(),
                context.getAgentSessionId(),
                type,
                nodeName,
                content,
                status,
                data
        );
        SsePayload payload = new SsePayload();
        payload.setEvent(eventName);
        payload.setType(type);
        payload.setName(nodeName);
        payload.setContent(content);
        payload.setStatus(status);
        payload.setData(data);
        this.sseConnectionManager.send(context.getSseConnectionKey(), eventName, payload);
        log.debug("[AGENT_EVENT] {}", JSONObject.toJSONString(payload));
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
}
