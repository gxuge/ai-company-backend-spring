package org.jeecg.modules.airag.agent.example;

import org.jeecg.modules.airag.agent.node.ToolNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * 使用默认参数生成故事工具节点。
 *
 * @author codex
 * @date 2026/6/16
 */
@Component
public class GenerateStoryWithDefaultsToolNode extends ToolNode {
    /**
     * 构造函数。
     *
     * @param toolRegistry 工具注册中心
     */
    public GenerateStoryWithDefaultsToolNode(ToolRegistry toolRegistry) {
        super("generate_story_with_defaults", "默认参数生成故事", "generate_story_with_defaults", toolRegistry);
    }

    @Override
    protected ToolCallRequest buildRequest(AgentContext context) {
        ToolCallRequest request = new ToolCallRequest();
        request.getArguments().put("userInput", context.getUserInput());
        request.getArguments().put("toolArgs", context.getAttribute("toolArgs"));
        return request;
    }
}
