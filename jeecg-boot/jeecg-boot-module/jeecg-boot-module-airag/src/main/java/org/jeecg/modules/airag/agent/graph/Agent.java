package org.jeecg.modules.airag.agent.graph;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;

/**
 * 主 Agent 接口。
 *
 * @author codex
 * @date 2026/6/16
 */
public interface Agent {

    /**
     * 返回 Agent 名称。
     *
     * @return Agent 名称
     */
    String agentName();

    /**
     * 执行 Agent 主流程。
     *
     * @param context 运行上下文
     * @return Agent 执行结果
     */
    AgentResult execute(AgentContext context);
}
