package org.jeecg.modules.system.agent.task.role;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateRoleDto;
import org.jeecg.modules.system.service.ITsRoleGenerateService;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

class RoleTaskToolRegistrarCleanupTest {

    @Test
    void shouldKeepBusinessToolsAndJsonStateWithoutLegacyConfirmationTool() {
        ToolRegistry toolRegistry = new ToolRegistry();
        ITsRoleGenerateService generateService = Mockito.mock(ITsRoleGenerateService.class);
        Mockito.when(generateService.generateRole(Mockito.any(), Mockito.any()))
                .thenReturn(new TsRoleGenerateRoleVo());
        RoleTaskToolRegistrar registrar = new RoleTaskToolRegistrar(toolRegistry, generateService);
        registrar.registerTools();

        Assertions.assertEquals(4, toolRegistry.listDefinitions().size());
        Assertions.assertTrue(toolRegistry.listDefinitions().stream()
                .noneMatch(definition -> "role_confirmation".equals(definition.getName())));

        AgentContext context = new AgentContext();
        context.setUserId("test-user");
        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(RoleTaskToolSpec.ROLE_GENERATE_ROLE);
        request.setArguments(Map.of("user_input", "创建一个都市侦探角色"));
        toolRegistry.execute(context, request);

        ArgumentCaptor<TsRoleGenerateRoleDto> dtoCaptor = ArgumentCaptor.forClass(TsRoleGenerateRoleDto.class);
        Mockito.verify(generateService).generateRole(Mockito.any(), dtoCaptor.capture());
        Assertions.assertEquals("创建一个都市侦探角色", dtoCaptor.getValue().getStorySetting());
        Assertions.assertEquals("创建一个都市侦探角色", dtoCaptor.getValue().getStoryBackground());
        Assertions.assertNotNull(context.getAttribute("roleGenerateRoleResultJson"));
        Assertions.assertNull(context.getAttribute("roleGenerateRoleResult"));
    }
}
