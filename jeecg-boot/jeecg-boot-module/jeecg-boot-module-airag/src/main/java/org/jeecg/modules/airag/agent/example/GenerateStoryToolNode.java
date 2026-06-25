package org.jeecg.modules.airag.agent.example;

import org.jeecg.modules.airag.agent.node.ToolNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * 生成故事工具节点。
 *
 * @author codex
 * @date 2026/6/16
 */
@Component
public class GenerateStoryToolNode extends ToolNode {
    /**
     * 构造函数。
     *
     * @param toolRegistry 工具注册中心
     */
    public GenerateStoryToolNode(ToolRegistry toolRegistry) {
        super("generate_story", "生成故事", "generate_story", toolRegistry);
    }

    @Override
    protected ToolCallRequest buildRequest(AgentContext context) {
        ToolCallRequest request = new ToolCallRequest();
        Object toolArgs = context.getAttribute("toolArgs");
        if (toolArgs != null) {
            request.getArguments().put("toolArgs", toolArgs);
        }
        request.getArguments().put("userInput", context.getUserInput());
        return request;
    }
}
