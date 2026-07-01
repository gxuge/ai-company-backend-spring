package org.jeecg.modules.airag.agent.runtime;

import lombok.Data;
import org.jeecg.common.util.UUIDGenerator;

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
     * Agent编码。
     */
    private String agentCode;
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
     * 共享扩展数据。
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * 规范化上下文基础字段。
     */
    public void normalize() {
        if (this.runId == null || this.runId.isBlank()) {
            this.runId = UUIDGenerator.generate();
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
}
