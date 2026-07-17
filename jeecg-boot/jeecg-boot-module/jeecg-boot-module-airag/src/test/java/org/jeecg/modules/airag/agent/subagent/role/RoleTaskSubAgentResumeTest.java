package org.jeecg.modules.airag.agent.subagent.role;

import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.interaction.AgentInteractionEventPublisher;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateImageNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateVoiceNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class RoleTaskSubAgentResumeTest {
    private NodeRunner nodeRunner;
    private AgentInteractionEventPublisher interactionEventPublisher;
    private RoleCreateDialogNode dialogNode;
    private RoleCreateImageNode imageNode;
    private RoleCreateVoiceNode voiceNode;
    private RoleTaskSubAgent subAgent;

    @BeforeEach
    void setUp() {
        this.nodeRunner = Mockito.mock(NodeRunner.class);
        this.interactionEventPublisher = Mockito.mock(AgentInteractionEventPublisher.class);
        this.dialogNode = Mockito.mock(RoleCreateDialogNode.class);
        this.imageNode = Mockito.mock(RoleCreateImageNode.class);
        this.voiceNode = Mockito.mock(RoleCreateVoiceNode.class);
        Mockito.when(this.dialogNode.nodeName()).thenReturn("role_create_dialog");
        Mockito.when(this.imageNode.nodeName()).thenReturn("role_create_image");
        Mockito.when(this.voiceNode.nodeName()).thenReturn("role_create_voice");
        this.subAgent = new RoleTaskSubAgent(
                this.nodeRunner,
                this.interactionEventPublisher,
                this.dialogNode,
                this.imageNode,
                this.voiceNode
        );
    }

    @Test
    void shouldResumeAtVoiceWithoutRepeatingEarlierNodes() {
        AgentContext context = new AgentContext();
        context.setActiveStage("voice");
        context.setResumeNodeName("role_create_voice");
        context.putAttribute("roleCoreResultJson", "{\"name\":\"林夏\"}");
        context.putAttribute("roleImageResultJson", "{\"url\":\"image\"}");
        NodeResult voiceResult = NodeResult.success("声音已生成");
        Mockito.when(this.nodeRunner.run(context, this.voiceNode)).thenReturn(voiceResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.HANDOFF, result.getStatus());
        Assertions.assertEquals(AgentRegistry.MAIN_AGENT_CODE, result.getHandoffTargetAgentCode());
        Assertions.assertEquals(Boolean.TRUE, result.getData().get("completed"));
        Mockito.verify(this.nodeRunner).run(context, this.voiceNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.dialogNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.imageNode);
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
    void shouldContinueDialogWhenRoleCoreExistsWithoutExplicitConfirmationTool() {
        AgentContext context = new AgentContext();
        context.setActiveStage("confirmation");
        context.putAttribute("roleCoreResultJson", "{\"name\":\"林夏\"}");
        NodeResult dialogResult = NodeResult.success("我们再完善一下她的经历。");
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenReturn(dialogResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertEquals("dialog", result.getData().get("stage"));
        Mockito.verify(this.nodeRunner).run(context, this.dialogNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.imageNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.voiceNode);
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
        Mockito.verifyNoInteractions(this.nodeRunner);
    }

    @Test
    void shouldContinueWithImageAndVoiceWhenConfirmationIsAccepted() {
        AgentContext context = new AgentContext();
        Map<String, Object> interaction = createPendingConfirmation(context);
        context.putAttribute("interactionId", interaction.get("interactionId"));
        context.putAttribute("optionValue", RoleConfirmationTransitions.ACCEPT_AND_CONTINUE);
        NodeResult imageResult = NodeResult.success("形象已生成");
        NodeResult voiceResult = NodeResult.success("声音已生成");
        Mockito.when(this.nodeRunner.run(context, this.imageNode)).thenReturn(imageResult);
        Mockito.when(this.nodeRunner.run(context, this.voiceNode)).thenReturn(voiceResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.HANDOFF, result.getStatus());
        Mockito.verify(this.interactionEventPublisher).publishResolved(
                context,
                interaction,
                RoleConfirmationTransitions.ACCEPT_AND_CONTINUE
        );
        Mockito.verify(this.nodeRunner).run(context, this.imageNode);
        Mockito.verify(this.nodeRunner).run(context, this.voiceNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.dialogNode);
        Assertions.assertNull(context.getAttribute("pendingUserInteraction"));
        Assertions.assertNull(context.getAttribute("optionValue"));
    }

    @Test
    void shouldReturnToDialogWhenRegenerateIsSelected() {
        AgentContext context = new AgentContext();
        Map<String, Object> interaction = createPendingConfirmation(context);
        context.putAttribute("interactionId", interaction.get("interactionId"));
        context.putAttribute("optionValue", RoleConfirmationTransitions.REGENERATE);
        NodeResult dialogResult = NodeResult.success("好的，我重新生成一版角色。");
        Mockito.when(this.nodeRunner.run(context, this.dialogNode)).thenReturn(dialogResult);

        AgentResult result = this.subAgent.execute(context);

        Assertions.assertEquals(AgentResult.Status.WAITING_USER, result.getStatus());
        Assertions.assertEquals("dialog", result.getData().get("stage"));
        Mockito.verify(this.interactionEventPublisher).publishResolved(
                context,
                interaction,
                RoleConfirmationTransitions.REGENERATE
        );
        Mockito.verify(this.nodeRunner).run(context, this.dialogNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.imageNode);
        Mockito.verify(this.nodeRunner, Mockito.never()).run(context, this.voiceNode);
    }

    private Map<String, Object> createPendingConfirmation(AgentContext context) {
        return UserInteractionSupport.createPending(
                context,
                "confirm",
                "role_request_confirmation",
                "role_create_dialog",
                "role_create_dialog",
                "你对这版角色满意吗？",
                "角色摘要",
                "roleCoreResultJson",
                List.of(
                        Map.of("label", "满意，继续生成", "value", RoleConfirmationTransitions.ACCEPT_AND_CONTINUE),
                        Map.of("label", "不满意，重新生成", "value", RoleConfirmationTransitions.REGENERATE)
                )
        );
    }
}
