package org.jeecg.modules.airag.agent.chat;

/**
 * Agent 路由动作枚举。
 *
 * @author codex
 * @date 2026/6/25
 */
public enum AgentRouteAction {
    /**
     * 调用指定聊天子 Agent。
     */
    CALL_CHAT_AGENT,
    /**
     * 调用指定任务子 Agent。
     */
    CALL_TASK_AGENT,
    /**
     * 调用默认子 Agent。
     */
    CALL_DEFAULT,
    /**
     * 需要追问用户。
     */
    ASK_USER,
    /**
     * 兼容旧版路由动作。
     */
    CALL_SUB_AGENT
}
