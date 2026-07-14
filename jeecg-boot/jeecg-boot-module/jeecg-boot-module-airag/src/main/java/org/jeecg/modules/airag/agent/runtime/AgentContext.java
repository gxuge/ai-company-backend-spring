package org.jeecg.modules.airag.agent.runtime;

import lombok.Data;
import org.jeecg.common.util.UUIDGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单次 Agent 执行上下文。
 *
 * @author codex
 * @date 2026/6/16
 */
@Data
public class AgentContext {
    /**
     * 运行实例标识。
     */
    private String runId;
    /**
     * 整轮链路追踪标识。
     */
    private String traceId;
    /**
     * 父运行实例标识。
     */
    private String parentRunId;
    /**
     * 当前对话轮次标识。
     */
    private String turnId;
    /**
     * 当前 Agent 编码。
     */
    private String agentCode;
    /**
     * 发送方类型：main_agent/sub_agent/tool 等。
     */
    private String senderType;
    /**
     * 应用ID。
     */
    private String appId;
    /**
     * 消息ID。
     */
    private String messageId;
    /**
     * Agent 会话ID。
     */
    private Long agentSessionId;
    /**
     * 业务会话ID。
     */
    private Long sessionId;
    /**
     * 用户ID。
     */
    private String userId;
    /**
     * 用户输入。
     */
    private String userInput;
    /**
     * SSE 连接键。
     */
    private String sseConnectionKey;
    /**
     * 最近一次节点输出。
     */
    private String latestContent;
    /**
     * 当前执行节点名称。
     */
    private String currentNodeName;
    /**
     * 当前执行节点类型。
     */
    private String currentNodeType;
    /**
     * 最后一个成功且正文非空的结果节点名称。
     */
    private String resultNodeName;
    /**
     * 最后一个成功且正文非空的结果节点类型。
     */
    private String resultNodeType;
    /**
     * 最近完成的 SubAgent 完整事件ID。
     */
    private String lastCompletedSubAgentEventId;
    /**
     * 跨消息恢复时下一步执行的节点名称。
     */
    private String resumeNodeName;
    /**
     * 跨消息恢复时当前子 Agent 流程阶段。
     */
    private String activeStage;
    /**
     * 共享扩展数据。
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    /**
     * 事件轨迹。
     */
    private final List<Map<String, Object>> eventTrail = Collections.synchronizedList(new ArrayList<>());

    /**
     * 规范化上下文基础字段。
     */
    public void normalize() {
        if (this.runId == null || this.runId.isBlank()) {
            this.runId = UUIDGenerator.generate();
        }
        if (this.traceId == null || this.traceId.isBlank()) {
            this.traceId = this.runId;
        }
    }

    /**
     * 写入上下文字段。
     *
     * @param key 键
     * @param value 值
     */
    public void putAttribute(String key, Object value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        this.attributes.put(key, value);
    }

    /**
     * 读取上下文字段。
     *
     * @param key 键
     * @return 值
     */
    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     * 删除上下文字段。
     *
     * @param key 键
     */
    public void removeAttribute(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        this.attributes.remove(key);
    }

    /**
     * 按类型读取上下文字段。
     *
     * @param key 键
     * @param clazz 目标类型
     * @param <T> 泛型
     * @return 值
     */
    public <T> T getAttribute(String key, Class<T> clazz) {
        Object value = this.attributes.get(key);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    /**
     * 追加一条执行事件。
     *
     * @param event 事件内容
     */
    public void appendEvent(Map<String, Object> event) {
        if (event == null || event.isEmpty()) {
            return;
        }
        this.eventTrail.add(new LinkedHashMap<>(event));
    }

    /**
     * 获取事件轨迹快照。
     *
     * @return 事件列表
     */
    public List<Map<String, Object>> snapshotEvents() {
        synchronized (this.eventTrail) {
            return new ArrayList<>(this.eventTrail);
        }
    }

    /**
     * 清空事件轨迹。
     */
    public void clearEvents() {
        this.eventTrail.clear();
    }

    /**
     * 标记当前正在执行的节点。
     *
     * @param nodeName 节点名称
     * @param nodeType 节点类型
     */
    public void markCurrentNode(String nodeName, String nodeType) {
        this.currentNodeName = normalizeText(nodeName);
        this.currentNodeType = normalizeText(nodeType);
    }

    /**
     * 在节点成功且返回正文时更新最终结果节点。
     *
     * @param nodeName 节点名称
     * @param nodeType 节点类型
     * @param content 节点正文
     * @param success 是否执行成功
     */
    public void markResultNode(String nodeName, String nodeType, String content, boolean success) {
        if (!success || content == null || content.isBlank()) {
            return;
        }
        this.resultNodeName = normalizeText(nodeName);
        this.resultNodeType = normalizeText(nodeType);
    }

    /**
     * 切换活动 Agent 前清空上一 Agent 的节点来源。
     */
    public void resetNodeSource() {
        this.currentNodeName = null;
        this.currentNodeType = null;
        this.resultNodeName = null;
        this.resultNodeType = null;
        this.lastCompletedSubAgentEventId = null;
    }

    /**
     * 复制出一个子上下文，用于 subagent 独立执行。
     *
     * @param userInput 子上下文用户输入
     * @return 子上下文
     */
    public AgentContext fork(String userInput) {
        AgentContext child = new AgentContext();
        child.setRunId(this.runId);
        child.setTraceId(this.traceId);
        child.setParentRunId(this.runId);
        child.setTurnId(this.turnId);
        child.setAgentCode(this.agentCode);
        child.setSenderType(this.senderType);
        child.setAppId(this.appId);
        child.setMessageId(this.messageId);
        child.setAgentSessionId(this.agentSessionId);
        child.setSessionId(this.sessionId);
        child.setUserId(this.userId);
        child.setUserInput(userInput);
        child.setSseConnectionKey(this.sseConnectionKey);
        child.setLatestContent(this.latestContent);
        child.setCurrentNodeName(this.currentNodeName);
        child.setCurrentNodeType(this.currentNodeType);
        child.setResultNodeName(this.resultNodeName);
        child.setResultNodeType(this.resultNodeType);
        child.setLastCompletedSubAgentEventId(this.lastCompletedSubAgentEventId);
        child.setResumeNodeName(this.resumeNodeName);
        child.setActiveStage(this.activeStage);
        child.attributes.putAll(this.attributes);
        return child;
    }

    /**
     * 规范化可空文本。
     *
     * @param value 原始文本
     * @return 去除首尾空白后的文本
     */
    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
