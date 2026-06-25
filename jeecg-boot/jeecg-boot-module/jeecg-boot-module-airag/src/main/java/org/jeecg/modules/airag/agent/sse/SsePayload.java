package org.jeecg.modules.airag.agent.sse;

import lombok.Data;

/**
 * SSE 事件载荷。
 *
 * @author codex
 * @date 2026/6/16
 */
@Data
public class SsePayload {
    /**
     * 事件名。
     */
    private String event;
    /**
     * 节点类型。
     */
    private String type;
    /**
     * 节点名称。
     */
    private String name;
    /**
     * 主体内容。
     */
    private String content;
    /**
     * 状态值。
     */
    private Integer status;
    /**
     * 扩展数据。
     */
    private Object data;
}
