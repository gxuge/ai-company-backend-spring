package org.jeecg.modules.airag.agent.subagent.role;

import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleGenerateCompleteToolContract;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class RoleTaskSubAgentResumeTest {
    private NodeRunner nodeRunner;
    private RoleCreateDialogNode dialogNode;
    private RoleTaskSubAgent subAgent;

    @BeforeEach
    void setUp() {
        this.nodeRunner = Mockito.mock(NodeRunner.class);
        this.dialogNode = Mockito.mock(RoleCreateDialogNode.class);
        Mockito.when(this.dialogNode.nodeName()).thenReturn("role_create_dialog");
        this.subAgent = new RoleTaskSubAgent(this.nodeRunner, this.dialogNode);
    }

    @Test
    void shouldSaveDialogAsNextResumeNodeWhenWaitingForUser() {
        AgentContext context = new AgentContext();
        NodeResult dialogResult = NodeResult.success("请补充角色职业");
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenReturn(dialogResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertEquals("dialog", result.getData().get("activeStage"));
        Assertions.assertEquals("role_create_dialog", result.getData().get("resumeNodeName"));
        Assertions.assertEquals("role_create_dialog", context.getResumeNodeName());
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
        Assertions.assertEquals("这版喜欢吗？✨", result.getContent());
        Mockito.verifyNoInteractions(this.nodeRunner);
    }

    @Test
    void shouldTreatConfirmationReplyAsOrdinaryDialogInput() {
        AgentContext context = new AgentContext();
        createPendingConfirmation(context);
        context.setUserInput("再改改吧～");
        NodeResult dialogResult = NodeResult.success("好的，我重新生成一版角色。");
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenReturn(dialogResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertEquals("dialog", result.getData().get("stage"));
        Mockito.verify(this.nodeRunner).run(context, this.dialogNode);
        Assertions.assertNull(context.getAttribute("pendingUserInteraction"));
        Assertions.assertEquals(
                RoleConfirmationTransitions.DECISION_NONE,
                RoleTaskPromptSupport.baseVariables(context).get("role_confirmation_decision")
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
                RoleConfirmationTransitions.ACCEPT_AND_CONTINUE
        );
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenAnswer(invocation -> {
            Assertions.assertEquals(
                    RoleConfirmationTransitions.DECISION_ACCEPTED,
                    RoleTaskPromptSupport.baseVariables(context).get("role_confirmation_decision")
            );
            return NodeResult.success("好的，开始生成完整角色。");
        });

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertEquals(
                RoleConfirmationTransitions.DECISION_ACCEPTED,
                context.getAttribute(RoleConfirmationTransitions.ATTR_CONFIRMATION_DECISION)
        );
        Assertions.assertFalse(result.getData().containsKey(
                RoleConfirmationTransitions.ATTR_CONFIRMATION_DECISION
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
                RoleConfirmationTransitions.REGENERATE
        );
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenReturn(
                NodeResult.success("当然可以，想调整哪里呢？")
        );

        this.subAgent.execute(context);

        Assertions.assertEquals(
                RoleConfirmationTransitions.DECISION_REVISION_REQUESTED,
                RoleTaskPromptSupport.baseVariables(context).get("role_confirmation_decision")
        );
    }

    @Test
    void shouldNaturallyHandoffWhenCompleteGenerationIsAccepted() {
        AgentContext context = new AgentContext();
        RoleConfirmationTransitions.setDecision(
                context,
                RoleConfirmationTransitions.DECISION_ACCEPTED
        );
        NodeResult dialogResult = NodeResult.success("");
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenAnswer(invocation -> {
            RoleGenerateCompleteToolContract.markAccepted(
                    context,
                    "task-1",
                    "event-1",
                    "{\"roleName\":\"林夏\"}"
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
        Assertions.assertNull(context.getAttribute(
                RoleGenerateCompleteToolContract.ATTR_GENERATION_ACCEPTED
        ));
        Assertions.assertNull(context.getAttribute(
                RoleConfirmationTransitions.ATTR_CONFIRMATION_DECISION
        ));
    }

    private Map<String, Object> createPendingConfirmation(AgentContext context) {
        return UserInteractionSupport.createPending(
                context,
                "confirm",
                "role_request_confirmation",
                "role_create_dialog",
                "role_create_dialog",
                "这版喜欢吗？✨",
                null,
                List.of(
                        Map.of("label", "喜欢，继续✨", "value", RoleConfirmationTransitions.ACCEPT_AND_CONTINUE),
                        Map.of("label", "再改改吧～", "value", RoleConfirmationTransitions.REGENERATE)
                )
        );
    }
}
