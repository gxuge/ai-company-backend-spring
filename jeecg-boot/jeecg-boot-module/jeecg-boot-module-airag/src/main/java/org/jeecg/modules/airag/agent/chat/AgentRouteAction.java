package org.jeecg.modules.airag.agent.chat;

/**
 * Agent 路由动作枚举。
 *
 * @author codex
 * @date 2026/6/25
 */
public enum AgentRouteAction {
    /**
     * 调用指定子 Agent。
     */
    CALL_SUB_AGENT,
    /**
     * 调用默认子 Agent。
     */
    CALL_DEFAULT,
    /**
     * 需要追问用户。
     */
    ASK_USER
}
