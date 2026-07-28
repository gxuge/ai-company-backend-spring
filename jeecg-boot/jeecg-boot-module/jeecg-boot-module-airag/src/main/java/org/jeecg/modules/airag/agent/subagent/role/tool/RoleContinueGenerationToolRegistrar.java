package org.jeecg.modules.airag.agent.subagent.role.tool;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.PostConstruct;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色确认后继续生成工具注册器。
 *
 * @author codex
 * @date 2026/7/17
 */
@Component
public class RoleContinueGenerationToolRegistrar {
    private static final String ROUTE_ROLE_CONTINUE_GENERATION = "ROLE_CONTINUE_GENERATION";

    private final ToolRegistry toolRegistry;

    public RoleContinueGenerationToolRegistrar(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 注册角色继续生成工具。
     */
    @PostConstruct
    void registerTools() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(RoleTaskToolSpec.ROLE_CONTINUE_GENERATION);
        definition.setDisplayName("继续生成角色");
        definition.setDescription("接收最终角色四字段，并显式进入角色形象和声音生成流程");
        definition.setRouteKey(ROUTE_ROLE_CONTINUE_GENERATION);
        definition.setCategory("role_task");
        definition.setInputSchema(RoleContinueGenerationToolContract.inputSchema());
        definition.setRetryable(false);
        definition.setExecutor(this::continueGeneration);
        this.toolRegistry.register(definition);
    }

    /**
     * 保存最终角色数据，并标记角色链路继续执行。
     */
    private ToolCallResult continueGeneration(AgentContext context, ToolCallRequest request) {
        Map<String, Object> arguments = request == null || request.getArguments() == null
                ? Map.of()
                : request.getArguments();
        Map<String, Object> transferData = RoleContinueGenerationToolContract.requireTransferData(arguments);
        if (context != null) {
            context.putAttribute(
                    RoleContinueGenerationToolContract.TRANSFER_DATA_JSON,
                    JSON.toJSONString(transferData)
            );
        }
        RoleContinueGenerationToolContract.markContinueRequested(context);
        ToolCallResult result = ToolCallResult.success("角色设定已确认，继续生成形象和声音", transferData);
        result.setPayload(new LinkedHashMap<>(transferData));
        return result;
    }
}
