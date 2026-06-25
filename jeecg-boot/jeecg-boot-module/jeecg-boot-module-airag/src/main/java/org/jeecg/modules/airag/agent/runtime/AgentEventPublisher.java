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
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("agentName", agentName);
        data.put("messageId", context.getMessageId());
        data.put("sessionId", context.getSessionId());
        data.put("agentSessionId", context.getAgentSessionId());
        data.put("runId", context.getRunId());
        sendOnly(context, "agent.start", null, agentName, null, data);
    }

    /**
     * 发送 agent.end。
     *
     * @param context 运行上下文
     * @param agentName Agent 名称
     * @param result 执行结果
     */
    public void publishAgentEnd(AgentContext context, String agentName, AgentResult result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("agentName", agentName);
        data.put("messageId", context.getMessageId());
        data.put("sessionId", context.getSessionId());
        data.put("agentSessionId", context.getAgentSessionId());
        data.put("runId", context.getRunId());
        data.put("status", result.getStatus() == null ? null : result.getStatus().name());
        data.put("payload", result.getData());
        sendOnly(context, "agent.end", null, agentName, result.getContent(), data);
    }

    /**
     * 发送并落库 llm.start。
     *
     * @param context 运行上下文
     * @param nodeName 节点名
     * @param promptCode 模板编码
     */
    public void publishLlmStart(AgentContext context, String nodeName, String promptCode) {
        Map<String, Object> data = buildNodeJson("llm.start", NodeKind.LLM, nodeName, promptCode, null, null);
        persistAndSend(context, "llm.start", NodeKind.LLM, nodeName, "", 2, data);
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
        Map<String, Object> data = buildNodeJson("llm.delta", NodeKind.LLM, nodeName, null, null, null);
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
        Map<String, Object> data = buildNodeJson("llm.error", NodeKind.LLM, nodeName, promptCode, null, null);
        if (error != null) {
            data.put("errorCode", error.getClass().getSimpleName());
            data.put("errorMessage", error.getMessage());
        }
        persistAndSend(context, "llm.error", NodeKind.LLM, nodeName, error == null ? "" : error.getMessage(), 0, data);
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
        String content = readBuffer(buildLlmBufferKey(context, nodeName));
        clearBuffer(buildLlmBufferKey(context, nodeName));
        Map<String, Object> data = buildNodeJson("llm.end", NodeKind.LLM, nodeName, promptCode, null, payload);
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
        Map<String, Object> data = buildNodeJson("tool.start", NodeKind.TOOL, nodeName, null, toolName, payload);
        persistAndSend(context, "tool.start", NodeKind.TOOL, nodeName, "", 2, data);
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
        Map<String, Object> data = buildNodeJson("tool.error", NodeKind.TOOL, nodeName, null, toolName, payload);
        if (error != null) {
            data.put("errorCode", error.getClass().getSimpleName());
            data.put("errorMessage", error.getMessage());
        }
        persistAndSend(context, "tool.error", NodeKind.TOOL, nodeName, error == null ? "" : error.getMessage(), 0, data);
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
        Map<String, Object> data = buildNodeJson("tool.end", NodeKind.TOOL, nodeName, null, toolName, payload);
        persistAndSend(context, "tool.end", NodeKind.TOOL, nodeName, content, success ? 1 : 0, data);
    }

    /**
     * 构建节点事件 JSON。
     *
     * @param eventName 事件名
     * @param nodeKind 节点类型
     * @param nodeName 节点名
     * @param promptCode 模板编码
     * @param toolName 工具名
     * @param payload 扩展载荷
     * @return JSON Map
     */
    public Map<String, Object> buildNodeJson(String eventName,
                                             NodeKind nodeKind,
                                             String nodeName,
                                             String promptCode,
                                             String toolName,
                                             Map<String, Object> payload) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("event", eventName);
        data.put("nodeType", nodeKind == null ? null : nodeKind.name().toLowerCase());
        data.put("nodeName", nodeName);
        data.put("promptCode", promptCode);
        data.put("toolName", toolName);
        data.put("sessionId", null);
        data.put("agentSessionId", null);
        data.put("payload", payload);
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
     * 追加 llm 增量文本到 Redis。
     *
     * @param bufferKey Redis key
     * @param delta 增量文本
     */
    private void appendBuffer(String bufferKey, String delta) {
        String current = readBuffer(bufferKey);
        this.redisTemplate.opsForValue().set(bufferKey, current + (delta == null ? "" : delta), BUFFER_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }
}
