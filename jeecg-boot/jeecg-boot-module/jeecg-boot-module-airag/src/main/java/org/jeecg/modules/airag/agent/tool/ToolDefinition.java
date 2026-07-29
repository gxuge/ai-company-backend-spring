package org.jeecg.modules.airag.agent.tool;

import lombok.Data;

/**
 * 工具定义。
 *
 * @author codex
 * @date 2026/6/16
 */
@Data
public class ToolDefinition {
    /**
     * 工具名称。
     */
    private String name;
    /**
     * 展示名称。
     */
    private String displayName;
    /**
     * 描述。
     */
    private String description;
    /**
     * 路由键。
     */
    private String routeKey;
    /**
     * 分类。
     */
    private String category;
    /**
     * 输入结构。
     */
    private String inputSchema;
    /**
     * 执行器。
     */
    private ToolExecutor executor;
    /**
     * 超时时间。
     */
    private Long timeoutMs;
    /**
     * 是否可重试。
     */
    private Boolean retryable;
    /**
     * 是否为异步完成工具。
     *
     * <p>异步工具受理后立即返回，但 tool.end/tool.error 由后台任务稍后发布。</p>
     */
    private Boolean asynchronous;
}
