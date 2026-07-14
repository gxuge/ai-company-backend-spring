package org.jeecg.modules.airag.agent.subagent.story;

import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateBackgroundNode;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryFlowGateNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StoryTaskSubAgentResumeTest {
    private NodeRunner nodeRunner;
    private StoryCreateDialogNode dialogNode;
    private StoryFlowGateNode gateNode;
    private StoryCreateBackgroundNode backgroundNode;
    private StoryTaskSubAgent subAgent;

    @BeforeEach
    void setUp() {
        this.nodeRunner = Mockito.mock(NodeRunner.class);
        this.dialogNode = Mockito.mock(StoryCreateDialogNode.class);
        this.gateNode = Mockito.mock(StoryFlowGateNode.class);
        this.backgroundNode = Mockito.mock(StoryCreateBackgroundNode.class);
        Mockito.when(this.dialogNode.nodeName()).thenReturn("story_create_dialog");
        Mockito.when(this.gateNode.nodeName()).thenReturn("story_flow_gate");
        Mockito.when(this.backgroundNode.nodeName()).thenReturn("story_create_background");
        this.subAgent = new StoryTaskSubAgent(
                this.nodeRunner,
                this.dialogNode,
                this.gateNode,
                this.backgroundNode
        );
    }

    @Test
    void shouldResumeAtBackgroundAndHandoffAfterCompletion() {
        AgentContext context = new AgentContext();
        context.setActiveStage("background");
        context.setResumeNodeName("story_create_background");
        context.putAttribute("storyCoreResultJson", "{\"title\":\"夜航\"}");
        NodeResult backgroundResult = NodeResult.success("故事背景已生成");
        Mockito.when(this.nodeRunner.run(context, this.backgroundNode)).thenReturn(backgroundResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.HANDOFF, result.getStatus());
        Assertions.assertEquals(AgentRegistry.MAIN_AGENT_CODE, result.getHandoffTargetAgentCode());
        Assertions.assertEquals(Boolean.TRUE, result.getData().get("completed"));
        Mockito.verify(this.nodeRunner).run(context, this.backgroundNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.dialogNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.gateNode);
    }
}
