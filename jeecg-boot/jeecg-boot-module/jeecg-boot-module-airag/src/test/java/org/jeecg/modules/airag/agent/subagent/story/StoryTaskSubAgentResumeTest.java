package org.jeecg.modules.airag.agent.subagent.story;

import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryConfirmationNode;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateBackgroundNode;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateDialogNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StoryTaskSubAgentResumeTest {
    private NodeRunner nodeRunner;
    private StoryCreateDialogNode dialogNode;
    private StoryConfirmationNode confirmationNode;
    private StoryCreateBackgroundNode backgroundNode;
    private StoryTaskSubAgent subAgent;

    @BeforeEach
    void setUp() {
        this.nodeRunner = Mockito.mock(NodeRunner.class);
        this.dialogNode = Mockito.mock(StoryCreateDialogNode.class);
        this.confirmationNode = new StoryConfirmationNode();
        this.backgroundNode = Mockito.mock(StoryCreateBackgroundNode.class);
        Mockito.when(this.dialogNode.nodeName()).thenReturn("story_create_dialog");
        Mockito.when(this.backgroundNode.nodeName()).thenReturn("story_create_background");
        this.subAgent = new StoryTaskSubAgent(
                this.nodeRunner,
                this.dialogNode,
                this.confirmationNode,
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
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.confirmationNode);
    }

    @Test
    void shouldUseConfirmationNodeWhenStoryCoreAlreadyExists() {
        AgentContext context = new AgentContext();
        context.setActiveStage("confirmation");
        context.putAttribute("storyCoreResultJson", "{\"title\":\"夜航\"}");
        NodeResult confirmationResult = this.confirmationNode.execute(context);
        Mockito.when(this.nodeRunner.run(context, this.confirmationNode)).thenReturn(confirmationResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertEquals("confirmation", result.getData().get("stage"));
        Assertions.assertEquals("story_confirmation", context.getResumeNodeName());
        Mockito.verify(this.nodeRunner).run(context, this.confirmationNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.dialogNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.backgroundNode);
    }

    @Test
    void shouldContinueWithBackgroundWhenOptionValueIsAccepted() {
        AgentContext context = new AgentContext();
        context.setActiveStage("confirmation");
        context.putAttribute("storyCoreResultJson", "{\"title\":\"夜航\"}");
        context.putAttribute("optionValue", "ACCEPT_AND_CONTINUE");

        NodeResult confirmationResult = this.confirmationNode.execute(context);
        NodeResult backgroundResult = NodeResult.success("故事背景已生成");
        Mockito.when(this.nodeRunner.run(context, this.confirmationNode)).thenReturn(confirmationResult);
        Mockito.when(this.nodeRunner.run(context, this.backgroundNode)).thenReturn(backgroundResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.HANDOFF, result.getStatus());
        Mockito.verify(this.nodeRunner).run(context, this.confirmationNode);
        Mockito.verify(this.nodeRunner).run(context, this.backgroundNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.dialogNode);
        Assertions.assertNull(context.getAttribute("optionValue"));
    }
}
