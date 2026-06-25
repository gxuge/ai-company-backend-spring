package org.jeecg.modules.airag.agent.tool;

import org.jeecg.modules.airag.agent.runtime.AgentContext;

/**
 * 工具执行器函数接口。
 *
 * @author codex
 * @date 2026/6/16
 */
@FunctionalInterface
public interface ToolExecutor {

    /**
     * 执行工具逻辑。
     *
     * @param context 运行上下文
     * @param request 工具请求
     * @return 工具结果
     */
    ToolCallResult execute(AgentContext context, ToolCallRequest request);
}
