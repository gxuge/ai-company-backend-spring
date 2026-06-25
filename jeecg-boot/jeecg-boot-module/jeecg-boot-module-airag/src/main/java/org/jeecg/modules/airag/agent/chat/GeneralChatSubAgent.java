package org.jeecg.modules.airag.agent.chat;

import lombok.RequiredArgsConstructor;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.springframework.stereotype.Component;

/**
 * 默认聊天子 Agent。
 *
 * @author codex
 * @date 2026/6/25
 */
@Component
@RequiredArgsConstructor
public class GeneralChatSubAgent implements SubAgent {
    /**
     * 节点执行器。
     */
    private final NodeRunner nodeRunner;
    /**
     * 默认聊天节点。
     */
    private final GeneralChatReplyNode generalChatReplyNode;

    @Override
    public String subAgentName() {
        return "general_chat";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        var nodeResult = this.nodeRunner.run(context, this.generalChatReplyNode);
        AgentResult result = AgentResult.success(nodeResult.getContent());
        result.getData().putAll(nodeResult.getData());
        return result;
    }
}
