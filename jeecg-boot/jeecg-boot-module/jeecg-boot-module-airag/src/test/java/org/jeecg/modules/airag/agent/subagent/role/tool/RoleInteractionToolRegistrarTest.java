package org.jeecg.modules.airag.agent.subagent.role.tool;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class RoleInteractionToolRegistrarTest {

    @Test
    void shouldExposeOnlyCurrentRoleConfirmationOptions() {
        ToolRegistry toolRegistry = new ToolRegistry();
        RoleInteractionToolRegistrar registrar = new RoleInteractionToolRegistrar(toolRegistry);
        registrar.registerTools();
        AgentContext context = new AgentContext();
        context.setCurrentNodeName("role_create_dialog");
        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION);
        request.setArguments(Map.of(
                "question", "这版骑士角色符合你的预期吗？",
                "summary", "王城守卫骑士"
        ));

        ToolCallResult result = toolRegistry.execute(context, request);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getData() instanceof Map<?, ?>);
        Map<?, ?> interaction = (Map<?, ?>) result.getData();
        Assertions.assertEquals("confirm", interaction.get("interactionType"));
        Assertions.assertEquals("这版骑士角色符合你的预期吗？", interaction.get("question"));
        Assertions.assertEquals(List.of(
                Map.of("label", "满意，继续生成", "value", "ACCEPT_AND_CONTINUE"),
                Map.of("label", "不满意，重新生成", "value", "REGENERATE")
        ), interaction.get("options"));
        Assertions.assertFalse(String.valueOf(interaction.get("options")).contains("MODIFY"));
        Assertions.assertEquals(interaction, context.getAttribute("pendingUserInteraction"));
    }
}
