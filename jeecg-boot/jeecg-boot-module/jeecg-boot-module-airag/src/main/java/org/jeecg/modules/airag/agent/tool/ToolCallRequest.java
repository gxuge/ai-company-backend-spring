package org.jeecg.modules.airag.agent.tool;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具调用请求。
 *
 * @author codex
 * @date 2026/6/16
 */
@Data
public class ToolCallRequest {
    /**
     * 工具名称。
     */
    private String toolName;
    /**
     * 入参集合。
     */
    private Map<String, Object> arguments = new LinkedHashMap<>();
    /**
     * 当前 Tool Event ID。
     */
    private String eventId;
    /**
     * 异步任务ID。
     */
    private String taskId;
}
