package org.jeecg.modules.airag.agent.sse;

import lombok.Data;

import java.util.List;
import java.util.Map;

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
    /**
     * 工具名称。
     */
    private String toolName;
    /**
     * 内容类型。
     */
    private String contentType;
    /**
     * 工具结果预览。
     */
    private Object result;
    /**
     * 错误信息。
     */
    private String error;
    /**
     * 交互问题。
     */
    private String question;
    /**
     * 交互选项。
     */
    private List<Map<String, String>> options;
}
