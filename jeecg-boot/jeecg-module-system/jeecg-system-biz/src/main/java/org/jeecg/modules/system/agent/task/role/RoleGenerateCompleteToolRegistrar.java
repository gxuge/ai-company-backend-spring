package org.jeecg.modules.system.agent.task.role;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleGenerateCompleteToolContract;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 完整角色异步生成工具注册器。
 */
@Component
@RequiredArgsConstructor
public class RoleGenerateCompleteToolRegistrar {
    private static final String ROUTE_ROLE_GENERATE_COMPLETE = "ROLE_GENERATE_COMPLETE";

    private final ToolRegistry toolRegistry;
    private final RoleGenerateCompleteAsyncService asyncService;

    /**
     * 注册完整角色异步生成工具。
     */
    @PostConstruct
    void registerTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(RoleTaskToolSpec.ROLE_GENERATE_COMPLETE);
        definition.setDisplayName("完整角色生成");
        definition.setDescription("异步生成角色资料，并显式保存形象资产后关联角色");
        definition.setRouteKey(ROUTE_ROLE_GENERATE_COMPLETE);
        definition.setCategory("role_task");
        definition.setInputSchema(RoleGenerateCompleteToolContract.inputSchema());
        definition.setRetryable(false);
        definition.setAsynchronous(true);
        definition.setExecutor(this::submitGeneration);
        this.toolRegistry.register(definition);
    }

    private ToolCallResult submitGeneration(AgentContext context, ToolCallRequest request) {
        Map<String, Object> arguments = request == null || request.getArguments() == null
                ? Map.of()
                : request.getArguments();
        Map<String, Object> transferData = RoleGenerateCompleteToolContract.requireTransferData(arguments);
        return this.asyncService.submit(context, request, transferData);
    }
}
