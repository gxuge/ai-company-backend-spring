package org.jeecg.modules.airag.agent.subagent.role;

import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateImageNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RoleImageTaskSubAgentTest {

    @Test
    void shouldStayInImageAgentWhileWaitingForMoreDetails() {
        NodeRunner nodeRunner = Mockito.mock(NodeRunner.class);
        RoleCreateImageNode imageNode = Mockito.mock(RoleCreateImageNode.class);
        RoleImageTaskSubAgent subAgent = new RoleImageTaskSubAgent(nodeRunner, imageNode);
        AgentContext context = new AgentContext();
        NodeResult nodeResult = NodeResult.success("请继续描述角色的服装造型");
        Mockito.when(imageNode.nodeName()).thenReturn("role_create_image");
        Mockito.when(nodeRunner.run(context, imageNode)).thenReturn(nodeResult);

        AgentResult result = subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertNull(result.getHandoffTargetAgentCode());
        Assertions.assertEquals("role_create_image", result.getData().get("resumeNodeName"));
        Assertions.assertEquals("image", result.getData().get("activeStage"));
        Assertions.assertEquals("请继续描述角色的服装造型", result.getContent());
    }

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
        Assertions.assertEquals(
                Boolean.TRUE,
                result.getData().get(AgentHandoffSupport.DATA_END_RUN_AFTER_HANDOFF)
        );
        Mockito.verify(nodeRunner).run(context, imageNode);
        Mockito.verifyNoMoreInteractions(nodeRunner);
    }
}
