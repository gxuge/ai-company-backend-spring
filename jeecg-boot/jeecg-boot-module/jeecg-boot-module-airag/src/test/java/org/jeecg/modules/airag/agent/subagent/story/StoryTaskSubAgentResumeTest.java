package org.jeecg.modules.airag.agent.subagent.story;

import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryGenerateCompleteToolContract;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class StoryTaskSubAgentResumeTest {
    private NodeRunner nodeRunner;
    private StoryCreateDialogNode dialogNode;
    private StoryTaskSubAgent subAgent;

    @BeforeEach
    void setUp() {
        this.nodeRunner = Mockito.mock(NodeRunner.class);
        this.dialogNode = Mockito.mock(StoryCreateDialogNode.class);
        Mockito.when(this.dialogNode.nodeName()).thenReturn("story_create_dialog");
        this.subAgent = new StoryTaskSubAgent(
                this.nodeRunner,
                this.dialogNode
        );
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
        Assertions.assertNull(context.getAttribute("pendingUserInteraction"));
        Assertions.assertEquals(
                StoryConfirmationTransitions.DECISION_NONE,
                StoryTaskPromptSupport.baseVariables(context).get("story_confirmation_decision")
        );
    }

    @Test
    void shouldInjectAcceptedConfirmationDecisionIntoDialogPrompt() {
        AgentContext context = new AgentContext();
        Map<String, Object> interaction = createPendingConfirmation(context);
        context.setUserInput("喜欢，继续✨");
        context.putAttribute(
                UserInteractionSupport.ATTR_INTERACTION_ID,
                interaction.get("interactionId")
        );
        context.putAttribute(
                UserInteractionSupport.ATTR_OPTION_VALUE,
                StoryConfirmationTransitions.ACCEPT_AND_CONTINUE
        );
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenAnswer(invocation -> {
            Assertions.assertEquals(
                    StoryConfirmationTransitions.DECISION_ACCEPTED,
                    StoryTaskPromptSupport.baseVariables(context).get("story_confirmation_decision")
            );
            return NodeResult.success("好的，开始生成完整故事。");
        });

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertEquals(
                StoryConfirmationTransitions.DECISION_ACCEPTED,
                context.getAttribute(StoryConfirmationTransitions.ATTR_CONFIRMATION_DECISION)
        );
        Assertions.assertFalse(result.getData().containsKey(
                StoryConfirmationTransitions.ATTR_CONFIRMATION_DECISION
        ));
        Assertions.assertNull(context.getAttribute(UserInteractionSupport.ATTR_OPTION_VALUE));
    }

    @Test
    void shouldInjectRevisionRequestedDecisionIntoDialogPrompt() {
        AgentContext context = new AgentContext();
        Map<String, Object> interaction = createPendingConfirmation(context);
        context.setUserInput("再改改吧～");
        context.putAttribute(
                UserInteractionSupport.ATTR_INTERACTION_ID,
                interaction.get("interactionId")
        );
        context.putAttribute(
                UserInteractionSupport.ATTR_OPTION_VALUE,
                StoryConfirmationTransitions.REGENERATE
        );
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenReturn(
                NodeResult.success("当然可以，想调整哪里呢？")
        );

        this.subAgent.execute(context);

        Assertions.assertEquals(
                StoryConfirmationTransitions.DECISION_REVISION_REQUESTED,
                StoryTaskPromptSupport.baseVariables(context).get("story_confirmation_decision")
        );
    }

    @Test
    void shouldCompleteForegroundFlowWhenAsyncGenerationWasAccepted() {
        AgentContext context = new AgentContext();
        StoryConfirmationTransitions.setDecision(
                context,
                StoryConfirmationTransitions.DECISION_ACCEPTED
        );
        NodeResult dialogResult = NodeResult.success("故事设定已确认。");
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenAnswer(invocation -> {
            StoryGenerateCompleteToolContract.markAccepted(
                    context,
                    "task-1",
                    "event-1",
                    "{\"title\":\"夜航\"}"
            );
            return dialogResult;
        });

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.HANDOFF, result.getStatus());
        Assertions.assertEquals(AgentRegistry.MAIN_AGENT_CODE, result.getHandoffTargetAgentCode());
        Assertions.assertEquals("running", result.getData().get("generationStatus"));
        Assertions.assertEquals(Boolean.TRUE, result.getData().get(
                AgentHandoffSupport.DATA_END_RUN_AFTER_HANDOFF
        ));
        Map<?, ?> handoffPayload = (Map<?, ?>) result.getStructuredResult();
        Map<?, ?> generationResult = (Map<?, ?>) handoffPayload.get("result");
        Assertions.assertEquals("task-1", generationResult.get("taskId"));
        Mockito.verify(this.nodeRunner).run(context, this.dialogNode);
        Assertions.assertFalse(StoryGenerateCompleteToolContract.consumeAccepted(context));
        Assertions.assertNull(context.getAttribute(
                StoryConfirmationTransitions.ATTR_CONFIRMATION_DECISION
        ));
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
                        Map.of("label", "喜欢，继续✨", "value", StoryConfirmationTransitions.ACCEPT_AND_CONTINUE),
                        Map.of("label", "再改改吧～", "value", StoryConfirmationTransitions.REGENERATE)
                )
        );
    }
}
