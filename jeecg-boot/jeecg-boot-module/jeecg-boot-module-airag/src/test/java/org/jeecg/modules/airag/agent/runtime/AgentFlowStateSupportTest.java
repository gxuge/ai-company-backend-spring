package org.jeecg.modules.airag.agent.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentFlowStateSupportTest {

    @Test
    void shouldSnapshotAndRestoreOnlyCurrentAgentWhitelist() {
        AgentContext source = new AgentContext();
        source.putAttribute("roleCoreResultJson", "{\"name\":\"林夏\"}");
        source.putAttribute("roleImageResultJson", "{\"url\":\"demo\"}");
        source.putAttribute("storyCoreResultJson", "{\"title\":\"不应保存\"}");
        source.putAttribute("arbitraryValue", "不应保存");

        String json = AgentFlowStateSupport.snapshot(source, AgentFlowStateSupport.ROLE_AGENT_CODE);
        AgentContext restored = new AgentContext();
        AgentFlowStateSupport.restore(restored, AgentFlowStateSupport.ROLE_AGENT_CODE, json);

        Assertions.assertEquals("{\"name\":\"林夏\"}", restored.getAttribute("roleCoreResultJson"));
        Assertions.assertEquals("{\"url\":\"demo\"}", restored.getAttribute("roleImageResultJson"));
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
    void shouldClearResumePositionAndBusinessStateOnAgentSwitch() {
        AgentContext context = new AgentContext();
        context.setResumeNodeName("role_create_voice");
        context.setActiveStage("voice");
        context.putAttribute("roleCoreResultJson", "{}");
        context.putAttribute("storyCoreResultJson", "{}");

        AgentFlowStateSupport.clear(context);

        Assertions.assertNull(context.getResumeNodeName());
        Assertions.assertNull(context.getActiveStage());
        Assertions.assertNull(context.getAttribute("roleCoreResultJson"));
        Assertions.assertNull(context.getAttribute("storyCoreResultJson"));
    }
}
