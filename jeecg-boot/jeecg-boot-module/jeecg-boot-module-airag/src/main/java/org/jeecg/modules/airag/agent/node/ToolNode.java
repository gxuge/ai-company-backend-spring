package org.jeecg.modules.airag.agent.node;

import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;

/**
 * Tool 节点基类。
 *
 * @author codex
 * @date 2026/6/16
 */
public abstract class ToolNode extends BaseAgentNode {
    /**
     * 工具名。
     */
    private final String toolName;
    /**
     * 工具注册中心。
     */
    private final ToolRegistry toolRegistry;

    /**
     * 构造函数。
     *
     * @param nodeName 节点名
     * @param displayName 展示名
     * @param toolName 工具名
     * @param toolRegistry 工具注册中心
     */
    protected ToolNode(String nodeName, String displayName, String toolName, ToolRegistry toolRegistry) {
        super(nodeName, displayName, NodeKind.TOOL);
        this.toolName = toolName;
        this.toolRegistry = toolRegistry;
    }

    /**
     * 返回工具名。
     *
     * @return 工具名
     */
    public String getToolName() {
        return this.toolName;
    }

    @Override
    public NodeResult execute(AgentContext context) {
        ToolCallRequest request = buildRequest(context);
        request.setToolName(this.toolName);
        ToolCallResult toolCallResult = this.toolRegistry.execute(context, request);
        if (!toolCallResult.isSuccess()) {
            return NodeResult.failure(toolCallResult.getErrorMessage());
        }
        NodeResult result = NodeResult.success(toolCallResult.getSummary());
        result.setContent(toolCallResult.getSummary());
        result.setAction(this.toolName);
        result.setSuccess(true);
        result.put("toolName", this.toolName);
        result.put("toolData", toolCallResult.getData());
        result.put("toolPayload", toolCallResult.getPayload());
        return result;
    }

    /**
     * 构造工具请求。
     *
     * @param context 运行上下文
     * @return 工具请求
     */
    protected abstract ToolCallRequest buildRequest(AgentContext context);
}
