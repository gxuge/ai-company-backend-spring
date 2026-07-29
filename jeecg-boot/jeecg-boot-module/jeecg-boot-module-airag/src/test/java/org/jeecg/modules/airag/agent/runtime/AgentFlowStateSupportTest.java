package org.jeecg.modules.airag.agent.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class AgentFlowStateSupportTest {

    @Test
    void shouldSnapshotAndRestoreOnlyCurrentAgentWhitelist() {
        AgentContext source = new AgentContext();
        source.putAttribute("taskDescription", "创建一个美女侦探角色");
        source.putAttribute("transferDataJson", "{\"roleName\":\"林夏\"}");
        source.putAttribute("roleCoreResultJson", "{\"name\":\"林夏\"}");
        source.putAttribute("roleImageResultJson", "{\"url\":\"demo\"}");
        source.putAttribute("roleConfirmationDecision", "ACCEPTED");
        source.putAttribute("pendingUserInteraction", Map.of(
                "interactionId", "confirm-1",
                "interactionType", "confirm"
        ));
        source.putAttribute("storyCoreResultJson", "{\"title\":\"不应保存\"}");
        source.putAttribute("arbitraryValue", "不应保存");

        String json = AgentFlowStateSupport.snapshot(source, AgentFlowStateSupport.ROLE_AGENT_CODE);
        AgentContext restored = new AgentContext();
        AgentFlowStateSupport.restore(restored, AgentFlowStateSupport.ROLE_AGENT_CODE, json);

        Assertions.assertEquals("{\"name\":\"林夏\"}", restored.getAttribute("roleCoreResultJson"));
        Assertions.assertEquals("{\"roleName\":\"林夏\"}", restored.getAttribute("transferDataJson"));
        Assertions.assertEquals("{\"url\":\"demo\"}", restored.getAttribute("roleImageResultJson"));
        Assertions.assertEquals("ACCEPTED", restored.getAttribute("roleConfirmationDecision"));
        Assertions.assertEquals("创建一个美女侦探角色", restored.getAttribute("taskDescription"));
        Assertions.assertNotNull(restored.getAttribute("pendingUserInteraction"));
        Assertions.assertNull(restored.getAttribute("storyCoreResultJson"));
        Assertions.assertNull(restored.getAttribute("arbitraryValue"));
    }

    @Test
    void shouldIgnoreMismatchedAgentAndInvalidJson() {
        AgentContext context = new AgentContext();
        String roleJson = """
                {"agentCode":"role_task_agent","attributes":{"roleCoreResultJson":"{}"}}
                """;

        AgentFlowStateSupport.restore(context, AgentFlowStateSupport.STORY_AGENT_CODE, roleJson);
        AgentFlowStateSupport.restore(context, AgentFlowStateSupport.ROLE_AGENT_CODE, "{invalid");

        Assertions.assertNull(context.getAttribute("roleCoreResultJson"));
    }

    @Test
    void shouldSnapshotStoryTransferDataAndPendingInteraction() {
        AgentContext source = new AgentContext();
        source.putAttribute("transferDataJson", "{\"title\":\"夜航\"}");
        source.putAttribute("storyConfirmationDecision", "ACCEPTED");
        source.putAttribute("pendingUserInteraction", Map.of(
                "interactionId", "story-confirm-1",
                "interactionType", "confirm"
        ));

        String json = AgentFlowStateSupport.snapshot(source, AgentFlowStateSupport.STORY_AGENT_CODE);
        AgentContext restored = new AgentContext();
        AgentFlowStateSupport.restore(restored, AgentFlowStateSupport.STORY_AGENT_CODE, json);

        Assertions.assertEquals("{\"title\":\"夜航\"}", restored.getAttribute("transferDataJson"));
        Assertions.assertEquals("ACCEPTED", restored.getAttribute("storyConfirmationDecision"));
        Assertions.assertNotNull(restored.getAttribute("pendingUserInteraction"));
    }

    @Test
    void shouldClearResumePositionAndBusinessStateOnAgentSwitch() {
        AgentContext context = new AgentContext();
        context.setResumeNodeName("role_create_voice");
        context.setActiveStage("voice");
        context.putAttribute("transferDataJson", "{}");
        context.putAttribute("roleCoreResultJson", "{}");
        context.putAttribute("roleConfirmationDecision", "REVISION_REQUESTED");
        context.putAttribute("storyCoreResultJson", "{}");
        context.putAttribute("storyConfirmationDecision", "ACCEPTED");

        AgentFlowStateSupport.clear(context);

        Assertions.assertNull(context.getResumeNodeName());
        Assertions.assertNull(context.getActiveStage());
        Assertions.assertNull(context.getAttribute("transferDataJson"));
        Assertions.assertNull(context.getAttribute("roleCoreResultJson"));
        Assertions.assertNull(context.getAttribute("roleConfirmationDecision"));
        Assertions.assertNull(context.getAttribute("storyCoreResultJson"));
        Assertions.assertNull(context.getAttribute("storyConfirmationDecision"));
    }
}
