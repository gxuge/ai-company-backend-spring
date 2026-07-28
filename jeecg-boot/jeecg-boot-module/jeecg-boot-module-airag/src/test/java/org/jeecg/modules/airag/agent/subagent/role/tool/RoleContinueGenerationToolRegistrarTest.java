package org.jeecg.modules.airag.agent.subagent.role.tool;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class RoleContinueGenerationToolRegistrarTest {

    @Test
    void shouldSaveFourRequiredFieldsAndRequestContinuation() {
        ToolRegistry toolRegistry = new ToolRegistry();
        RoleContinueGenerationToolRegistrar registrar = new RoleContinueGenerationToolRegistrar(toolRegistry);
        registrar.registerTools();
        AgentContext context = new AgentContext();
        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(RoleTaskToolSpec.ROLE_CONTINUE_GENERATION);
        request.setArguments(Map.of(
                "transferData", Map.of(
                        "roleName", "亚瑟·雷恩哈特",
                        "gender", "男性",
                        "occupation", "皇家骑士",
                        "backgroundStory", "曾是最年轻的骑士团长。",
                        "ignoredField", "不应传递"
                )
        ));

        ToolCallResult result = toolRegistry.execute(context, request);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(Map.of(
                "roleName", "亚瑟·雷恩哈特",
                "gender", "男性",
                "occupation", "皇家骑士",
                "backgroundStory", "曾是最年轻的骑士团长。"
        ), result.getData());
        Assertions.assertEquals(
                "{\"roleName\":\"亚瑟·雷恩哈特\",\"gender\":\"男性\",\"occupation\":\"皇家骑士\",\"backgroundStory\":\"曾是最年轻的骑士团长。\"}",
                context.getAttribute(RoleContinueGenerationToolContract.TRANSFER_DATA_JSON)
        );
        Assertions.assertTrue(RoleContinueGenerationToolContract.consumeContinueRequested(context));
        Assertions.assertFalse(RoleContinueGenerationToolContract.consumeContinueRequested(context));
    }

    @Test
    void shouldRejectWhenAnyRequiredFieldIsBlank() {
        ToolRegistry toolRegistry = new ToolRegistry();
        RoleContinueGenerationToolRegistrar registrar = new RoleContinueGenerationToolRegistrar(toolRegistry);
        registrar.registerTools();
        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(RoleTaskToolSpec.ROLE_CONTINUE_GENERATION);
        request.setArguments(Map.of(
                "transferData", Map.of(
                        "roleName", "亚瑟·雷恩哈特",
                        "gender", "男性",
                        "occupation", " ",
                        "backgroundStory", "曾是最年轻的骑士团长。"
                )
        ));

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> toolRegistry.execute(new AgentContext(), request)
        );

        Assertions.assertEquals("transferData.occupation 不能为空", exception.getMessage());
    }
}
