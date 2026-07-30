package org.jeecg.modules.airag.agent.subagent.story;

import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateBackgroundNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StoryBackgroundTaskSubAgentTest {

    @Test
    void shouldRunOnlyBackgroundNodeAndHandoffAfterCompletion() {
        NodeRunner nodeRunner = Mockito.mock(NodeRunner.class);
        StoryCreateBackgroundNode backgroundNode = Mockito.mock(StoryCreateBackgroundNode.class);
        StoryBackgroundTaskSubAgent subAgent = new StoryBackgroundTaskSubAgent(nodeRunner, backgroundNode);
        AgentContext context = new AgentContext();
        context.putAttribute("storyBackgroundResultJson", "{\"sceneSummary\":\"夜色港口\"}");
        NodeResult nodeResult = NodeResult.success("故事背景已生成");
        Mockito.when(nodeRunner.run(context, backgroundNode)).thenReturn(nodeResult);

        AgentResult result = subAgent.execute(context);

        Assertions.assertEquals(StoryBackgroundTaskSubAgent.SUB_AGENT_NAME, subAgent.subAgentName());
        Assertions.assertEquals(AgentResult.Status.HANDOFF, result.getStatus());
        Assertions.assertEquals(AgentRegistry.MAIN_AGENT_CODE, result.getHandoffTargetAgentCode());
        Assertions.assertEquals(Boolean.TRUE, result.getData().get("completed"));
        Assertions.assertEquals(
                Boolean.TRUE,
                result.getData().get(AgentHandoffSupport.DATA_END_RUN_AFTER_HANDOFF)
        );
        Mockito.verify(nodeRunner).run(context, backgroundNode);
        Mockito.verifyNoMoreInteractions(nodeRunner);
    }
}
