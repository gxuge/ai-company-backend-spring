package org.jeecg.modules.airag.agent.example;

import org.jeecg.modules.airag.agent.node.ToolNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * 保存草稿工具节点。
 *
 * @author codex
 * @date 2026/6/16
 */
@Component
public class SaveDraftToolNode extends ToolNode {
    /**
     * 构造函数。
     *
     * @param toolRegistry 工具注册中心
     */
    public SaveDraftToolNode(ToolRegistry toolRegistry) {
        super("save_draft", "保存草稿", "save_draft", toolRegistry);
    }

    @Override
    protected ToolCallRequest buildRequest(AgentContext context) {
        ToolCallRequest request = new ToolCallRequest();
        request.getArguments().put("storyDraft", context.getAttribute("storyDraft"));
        request.getArguments().put("messageId", context.getMessageId());
        return request;
    }
}
