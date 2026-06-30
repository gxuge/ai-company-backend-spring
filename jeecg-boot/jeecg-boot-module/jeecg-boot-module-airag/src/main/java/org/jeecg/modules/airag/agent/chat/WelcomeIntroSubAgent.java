package org.jeecg.modules.airag.agent.chat;

import lombok.RequiredArgsConstructor;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.springframework.stereotype.Component;

/**
 * Agent 开场白子流程。
 *
 * @author codex
 * @date 2026/6/26
 */
@Component
@RequiredArgsConstructor
public class WelcomeIntroSubAgent implements SubAgent {

    /**
     * 节点执行器。
     */
    private final NodeRunner nodeRunner;

    /**
     * 开场白节点。
     */
    private final WelcomeIntroNode welcomeIntroNode;

    @Override
    public String subAgentName() {
        return "welcome_intro";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        var nodeResult = this.nodeRunner.run(context, this.welcomeIntroNode);
        AgentResult result = AgentResult.success(nodeResult.getContent());
        result.getData().putAll(nodeResult.getData());
        return result;
    }
}
