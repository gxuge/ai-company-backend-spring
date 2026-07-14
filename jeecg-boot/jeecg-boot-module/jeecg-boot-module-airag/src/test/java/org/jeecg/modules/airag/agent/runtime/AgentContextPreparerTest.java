package org.jeecg.modules.airag.agent.runtime;

import org.jeecg.modules.airag.agent.graph.DeepAgentDefinitionRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

class AgentContextPreparerTest {

    private AgentContextPreparer contextPreparer;

    @BeforeEach
    void setUp() {
        DeepAgentDefinitionRegistry definitionRegistry = new DeepAgentDefinitionRegistry();
        definitionRegistry.init();
        this.contextPreparer = new AgentContextPreparer(definitionRegistry);
    }

    @Test
    void shouldPrepareSubAgentContextAndThenClearItForMainAgent() {
        AgentContext context = new AgentContext();
        context.setUserInput("创建一个侦探角色");
        context.putAttribute("sessionSubAgentHistoryJson", """
                {"role_task_agent":[{"summary":"已确认角色为女性"}]}
                """);

        this.contextPreparer.prepare(context, "role_task_agent");

        Assertions.assertEquals("role_task_agent", context.getAgentCode());
        Assertions.assertEquals("sub_agent", context.getSenderType());
        Assertions.assertEquals(Boolean.FALSE, context.getAttribute("deepAgentsMainMode"));
        Assertions.assertEquals("role", context.getAttribute("skillDomain"));
        Assertions.assertTrue(String.valueOf(context.getAttribute("subAgentHistoryJson")).contains("已确认角色为女性"));
        Map<?, ?> subVariables = context.getAttribute("promptVariables", Map.class);
        Assertions.assertEquals("创建一个侦探角色", subVariables.get("user_input"));

        context.markCurrentNode("roleCreateDialogNode", "llm");
        context.markResultNode("roleCreateDialogNode", "llm", "角色回复", true);
        context.setLastCompletedSubAgentEventId("event-1");
        this.contextPreparer.prepare(context, AgentRegistry.MAIN_AGENT_CODE);

        Assertions.assertEquals(AgentRegistry.MAIN_AGENT_CODE, context.getAgentCode());
        Assertions.assertEquals("main_agent", context.getSenderType());
        Assertions.assertEquals(Boolean.TRUE, context.getAttribute("deepAgentsMainMode"));
        Assertions.assertNull(context.getAttribute("skillDomain"));
        Assertions.assertNull(context.getAttribute("subAgentDefinition"));
        Assertions.assertNull(context.getCurrentNodeName());
        Assertions.assertNull(context.getResultNodeName());
        Assertions.assertNull(context.getLastCompletedSubAgentEventId());
    }

    @Test
    void shouldApplyHandoffInputAndReportToSameContext() {
        AgentContext context = new AgentContext();
        context.setUserInput("原始请求");
        context.setResumeNodeName("role_create_dialog");
        context.setActiveStage("confirmation");
        context.putAttribute("roleCoreResultJson", "{}");
        AgentResult handoff = AgentResult.handoffTo(AgentRegistry.MAIN_AGENT_CODE, "改为创建故事");
        handoff.getHandoffContext().put("handoffReport", Map.of("reason", "超出角色职责"));

        this.contextPreparer.applyHandoff(context, handoff);

        Assertions.assertEquals("改为创建故事", context.getUserInput());
        Assertions.assertEquals(Map.of("reason", "超出角色职责"), context.getAttribute("handoffReport"));
        Assertions.assertNull(context.getResumeNodeName());
        Assertions.assertNull(context.getActiveStage());
        Assertions.assertNull(context.getAttribute("roleCoreResultJson"));
    }
}
