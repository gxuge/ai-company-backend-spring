package org.jeecg.modules.airag.agent.graph;

/**
 * 节点类型枚举。
 *
 * @author codex
 * @date 2026/6/16
 */
public enum NodeKind {
    /**
     * 大模型节点。
     */
    LLM,
    /**
     * 工具节点。
     */
    TOOL,
    /**
     * 用户确认节点。
     */
    CONFIRM,
    /**
     * 用户候选项选择节点。
     */
    OPTIONS
}
