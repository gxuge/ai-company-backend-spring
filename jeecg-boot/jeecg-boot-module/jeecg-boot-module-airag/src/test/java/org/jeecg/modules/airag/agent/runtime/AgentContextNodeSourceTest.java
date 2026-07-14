package org.jeecg.modules.airag.agent.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentContextNodeSourceTest {

    @Test
    void shouldKeepLastSuccessfulNodeWithContent() {
        AgentContext context = new AgentContext();

        context.markCurrentNode("dialogNode", "llm");
        context.markResultNode("dialogNode", "llm", "请继续补充", true);
        context.markCurrentNode("emptyNode", "llm");
        context.markResultNode("emptyNode", "llm", " ", true);
        context.markCurrentNode("failedGateNode", "tool");
        context.markResultNode("failedGateNode", "tool", "执行失败", false);

        Assertions.assertEquals("failedGateNode", context.getCurrentNodeName());
        Assertions.assertEquals("tool", context.getCurrentNodeType());
        Assertions.assertEquals("dialogNode", context.getResultNodeName());
        Assertions.assertEquals("llm", context.getResultNodeType());
    }

    @Test
    void shouldCopyNodeSourceWhenForkingContext() {
        AgentContext context = new AgentContext();
        context.markCurrentNode("roleDialogNode", "llm");
        context.markResultNode("roleDialogNode", "llm", "角色回复", true);
        context.setLastCompletedSubAgentEventId("event-1");

        AgentContext child = context.fork("继续完善");

        Assertions.assertEquals("roleDialogNode", child.getCurrentNodeName());
        Assertions.assertEquals("roleDialogNode", child.getResultNodeName());
        Assertions.assertEquals("event-1", child.getLastCompletedSubAgentEventId());
    }
}
