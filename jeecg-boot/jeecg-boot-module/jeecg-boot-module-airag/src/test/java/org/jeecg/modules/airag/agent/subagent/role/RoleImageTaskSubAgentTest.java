package org.jeecg.modules.airag.agent.subagent.role;

import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateImageNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RoleImageTaskSubAgentTest {

    @Test
    void shouldRunOnlyImageNodeAndHandoffAfterCompletion() {
        NodeRunner nodeRunner = Mockito.mock(NodeRunner.class);
        RoleCreateImageNode imageNode = Mockito.mock(RoleCreateImageNode.class);
        RoleImageTaskSubAgent subAgent = new RoleImageTaskSubAgent(nodeRunner, imageNode);
        AgentContext context = new AgentContext();
        context.putAttribute("roleImageResultJson", "{\"imageUrl\":\"image\"}");
        NodeResult nodeResult = NodeResult.success("角色形象已生成");
        Mockito.when(nodeRunner.run(context, imageNode)).thenReturn(nodeResult);

        AgentResult result = subAgent.execute(context);

        Assertions.assertEquals(RoleImageTaskSubAgent.SUB_AGENT_NAME, subAgent.subAgentName());
        Assertions.assertEquals(AgentResult.Status.HANDOFF, result.getStatus());
        Assertions.assertEquals(AgentRegistry.MAIN_AGENT_CODE, result.getHandoffTargetAgentCode());
        Assertions.assertEquals(Boolean.TRUE, result.getData().get("completed"));
        Mockito.verify(nodeRunner).run(context, imageNode);
        Mockito.verifyNoMoreInteractions(nodeRunner);
    }
}
