package org.jeecg.modules.airag.agent.tool;

import org.jeecg.modules.airag.agent.common.SubAgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

class DeepAgentTaskToolServiceTest {

    @Test
    void shouldConvertPendingTaskToHandoffWithoutExecutingSubAgent() {
        SubAgentRegistry subAgentRegistry = Mockito.mock(SubAgentRegistry.class);
        Mockito.when(subAgentRegistry.exists("role_task_agent")).thenReturn(true);
        DeepAgentTaskToolService service = new DeepAgentTaskToolService(subAgentRegistry);
        AgentContext context = new AgentContext();
        context.putAttribute("deepAgentsPendingTaskArgs", Map.of(
                "subAgentName", "role_task_agent",
                "taskDescription", "创建一个侦探角色"
        ));

        AgentResult result = service.consumePendingHandoff(context);

        Assertions.assertEquals(AgentResult.Status.HANDOFF, result.getStatus());
        Assertions.assertEquals("role_task_agent", result.getHandoffTargetAgentCode());
        Assertions.assertEquals("创建一个侦探角色", result.getHandoffInput());
        Assertions.assertEquals("HANDOFF_TO_AGENT", result.getData().get("action"));
        Mockito.verify(subAgentRegistry).exists("role_task_agent");
        Mockito.verifyNoMoreInteractions(subAgentRegistry);
    }
}
