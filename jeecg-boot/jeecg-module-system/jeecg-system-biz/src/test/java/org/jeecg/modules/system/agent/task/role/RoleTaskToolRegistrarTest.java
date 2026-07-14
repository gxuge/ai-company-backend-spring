package org.jeecg.modules.system.agent.task.role;

import org.jeecg.modules.airag.agent.subagent.role.tool.RoleTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleTaskToolRegistrarTest {

    @Test
    void roleConfirmationReturnsFrontendOptionsWithValues() {
        ToolRegistry toolRegistry = new ToolRegistry();
        RoleTaskToolRegistrar registrar = new RoleTaskToolRegistrar(toolRegistry, null);
        registrar.registerTools();

        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(RoleTaskToolSpec.ROLE_CONFIRMATION);
        request.setArguments(Map.of());

        ToolCallResult result = toolRegistry.execute(null, request);

        assertTrue(result.isSuccess());
        assertTrue(result.getData() instanceof Map<?, ?>);
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals("WAIT_CONFIRM", data.get("action"));
        assertEquals("你对这版角色满意吗？", data.get("question"));
        assertEquals(
                List.of(
                        Map.of("label", "满意，继续生成", "value", "ACCEPT_AND_CONTINUE"),
                        Map.of("label", "不满意，重新生成", "value", "REGENERATE")
                ),
                data.get("options")
        );
    }

    @Test
    void roleConfirmationUsesOptionValueForDeterministicDecision() {
        ToolRegistry toolRegistry = new ToolRegistry();
        RoleTaskToolRegistrar registrar = new RoleTaskToolRegistrar(toolRegistry, null);
        registrar.registerTools();

        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(RoleTaskToolSpec.ROLE_CONFIRMATION);
        request.setArguments(Map.of("optionValue", "ACCEPT_AND_CONTINUE"));

        ToolCallResult result = toolRegistry.execute(null, request);

        assertTrue(result.isSuccess());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals("ACCEPT_AND_CONTINUE", data.get("action"));
        assertEquals(List.of(), data.get("options"));
    }
}
