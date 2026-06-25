package org.jeecg.modules.airag.agent.graph;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;

/**
 * 子 Agent 接口。
 *
 * @author codex
 * @date 2026/6/16
 */
public interface SubAgent {

    /**
     * 返回子 Agent 名称。
     *
     * @return 子 Agent 名称
     */
    String subAgentName();

    /**
     * 执行子流程。
     *
     * @param context 运行上下文
     * @return 子流程结果
     */
    AgentResult execute(AgentContext context);
}
