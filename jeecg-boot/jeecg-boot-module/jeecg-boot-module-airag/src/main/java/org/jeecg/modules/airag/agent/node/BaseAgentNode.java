package org.jeecg.modules.airag.agent.node;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jeecg.modules.airag.agent.graph.AgentNode;
import org.jeecg.modules.airag.agent.graph.NodeKind;

/**
 * 节点基类。
 *
 * @author codex
 * @date 2026/6/16
 */
@Getter
@RequiredArgsConstructor
public abstract class BaseAgentNode implements AgentNode {
    /**
     * 节点名。
     */
    private final String nodeName;
    /**
     * 展示名。
     */
    private final String displayName;
    /**
     * 节点类型。
     */
    private final NodeKind kind;

    @Override
    public String nodeName() {
        return this.nodeName;
    }

    @Override
    public String displayName() {
        return this.displayName;
    }

    @Override
    public NodeKind kind() {
        return this.kind;
    }
}
