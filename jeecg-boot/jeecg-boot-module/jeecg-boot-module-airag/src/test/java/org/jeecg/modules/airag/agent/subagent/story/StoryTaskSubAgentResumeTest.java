package org.jeecg.modules.airag.agent.subagent.story;

import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateBackgroundNode;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryContinueGenerationToolContract;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class StoryTaskSubAgentResumeTest {
    private NodeRunner nodeRunner;
    private StoryCreateDialogNode dialogNode;
    private StoryCreateBackgroundNode backgroundNode;
    private StoryTaskSubAgent subAgent;

    @BeforeEach
    void setUp() {
        this.nodeRunner = Mockito.mock(NodeRunner.class);
        this.dialogNode = Mockito.mock(StoryCreateDialogNode.class);
        this.backgroundNode = Mockito.mock(StoryCreateBackgroundNode.class);
        Mockito.when(this.dialogNode.nodeName()).thenReturn("story_create_dialog");
        Mockito.when(this.backgroundNode.nodeName()).thenReturn("story_create_background");
        this.subAgent = new StoryTaskSubAgent(
                this.nodeRunner,
                this.dialogNode,
                this.backgroundNode
        );
    }

    @Test
    void shouldResumeAtBackgroundAndHandoffAfterCompletion() {
        AgentContext context = new AgentContext();
        context.setActiveStage("background");
        context.setResumeNodeName("story_create_background");
        context.putAttribute("transferDataJson", "{\"title\":\"夜航\"}");
        NodeResult backgroundResult = NodeResult.success("故事背景已生成");
        Mockito.when(this.nodeRunner.run(context, this.backgroundNode)).thenReturn(backgroundResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.HANDOFF, result.getStatus());
        Assertions.assertEquals(AgentRegistry.MAIN_AGENT_CODE, result.getHandoffTargetAgentCode());
        Assertions.assertEquals(Boolean.TRUE, result.getData().get("completed"));
        Mockito.verify(this.nodeRunner).run(context, this.backgroundNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.dialogNode);
    }

    @Test
    void shouldContinueDialogWithoutExplicitConfirmationTool() {
        AgentContext context = new AgentContext();
        context.setActiveStage("confirmation");
        context.putAttribute("storyCoreResultJson", "{\"title\":\"夜航\"}");
        NodeResult dialogResult = NodeResult.success("我们再完善一下故事冲突。");
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenReturn(dialogResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertEquals("dialog", result.getData().get("stage"));
        Mockito.verify(this.nodeRunner).run(context, this.dialogNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.backgroundNode);
    }

    @Test
    void shouldWaitForUserWhenConfirmationToolCreatedPendingInteraction() {
        AgentContext context = new AgentContext();
        Map<String, Object> interaction = createPendingConfirmation(context);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertEquals("confirmation", result.getData().get("stage"));
        Assertions.assertEquals(interaction.get("interactionId"), result.getData().get("interactionId"));
        Assertions.assertEquals(interaction.get("options"), result.getData().get("options"));
        Assertions.assertEquals("这版故事喜欢吗？", result.getContent());
        Assertions.assertFalse(result.getData().containsKey("summary"));
        Mockito.verifyNoInteractions(this.nodeRunner);
    }

    @Test
    void shouldReturnUserConfirmationReplyToDialogWithoutDirectStageRouting() {
        AgentContext context = new AgentContext();
        createPendingConfirmation(context);
        context.setUserInput("喜欢，继续✨");
        NodeResult dialogResult = NodeResult.success("我会根据你的确认继续处理。");
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenReturn(dialogResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertEquals("dialog", result.getData().get("stage"));
        Mockito.verify(this.nodeRunner).run(context, this.dialogNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.backgroundNode);
        Assertions.assertNull(context.getAttribute("pendingUserInteraction"));
    }

    @Test
    void shouldContinueWithBackgroundOnlyWhenContinueToolWasCalled() {
        AgentContext context = new AgentContext();
        NodeResult dialogResult = NodeResult.success("故事设定已确认。");
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenAnswer(invocation -> {
            StoryContinueGenerationToolContract.markContinueRequested(context);
            return dialogResult;
        });
        NodeResult backgroundResult = NodeResult.success("故事背景已生成");
        Mockito.when(this.nodeRunner.run(context, this.backgroundNode)).thenReturn(backgroundResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.HANDOFF, result.getStatus());
        Mockito.verify(this.nodeRunner).run(context, this.dialogNode);
        Mockito.verify(this.nodeRunner).run(context, this.backgroundNode);
        Assertions.assertFalse(StoryContinueGenerationToolContract.consumeContinueRequested(context));
    }

    private Map<String, Object> createPendingConfirmation(AgentContext context) {
        return UserInteractionSupport.createPending(
                context,
                "confirm",
                "story_request_confirmation",
                "story_create_dialog",
                "story_create_dialog",
                "这版故事喜欢吗？",
                null,
                List.of(
                        Map.of("label", "喜欢，继续✨", "value", "ACCEPT_AND_CONTINUE"),
                        Map.of("label", "再改改吧～", "value", "REGENERATE")
                )
        );
    }
}
