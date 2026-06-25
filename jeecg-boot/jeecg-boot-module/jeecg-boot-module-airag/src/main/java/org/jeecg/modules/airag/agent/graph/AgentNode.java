package org.jeecg.modules.airag.agent.graph;

import org.jeecg.modules.airag.agent.runtime.AgentContext;

/**
 * Agent 运行时统一节点接口。
 *
 * @author codex
 * @date 2026/6/16
 */
public interface AgentNode {

    /**
     * 返回节点名称。
     *
     * @return 节点名称
     */
    String nodeName();

    /**
     * 返回节点展示名称。
     *
     * @return 节点展示名称
     */
    String displayName();

    /**
     * 返回节点类型。
     *
     * @return 节点类型
     */
    NodeKind kind();

    /**
     * 执行节点逻辑。
     *
     * @param context 运行上下文
     * @return 节点执行结果
     * @throws Exception 执行异常
     */
    NodeResult execute(AgentContext context) throws Exception;
}
