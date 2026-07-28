package org.jeecg.modules.airag.agent.subagent.story.tool;

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
 * 故事确认后继续生成工具注册器。
 *
 * @author codex
 * @date 2026/7/28
 */
@Component
public class StoryContinueGenerationToolRegistrar {
    private static final String ROUTE_STORY_CONTINUE_GENERATION = "STORY_CONTINUE_GENERATION";

    private final ToolRegistry toolRegistry;

    public StoryContinueGenerationToolRegistrar(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 注册故事继续生成工具。
     */
    @PostConstruct
    void registerTools() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StoryTaskToolSpec.STORY_CONTINUE_GENERATION);
        definition.setDisplayName("继续生成故事");
        definition.setDescription("接收最终故事六字段，并显式进入故事背景与场景生成流程");
        definition.setRouteKey(ROUTE_STORY_CONTINUE_GENERATION);
        definition.setCategory("story_task");
        definition.setInputSchema(StoryContinueGenerationToolContract.inputSchema());
        definition.setRetryable(false);
        definition.setExecutor(this::continueGeneration);
        this.toolRegistry.register(definition);
    }

    /**
     * 保存最终故事数据，并标记故事链路继续执行。
     */
    private ToolCallResult continueGeneration(AgentContext context, ToolCallRequest request) {
        Map<String, Object> arguments = request == null || request.getArguments() == null
                ? Map.of()
                : request.getArguments();
        Map<String, Object> transferData = StoryContinueGenerationToolContract.requireTransferData(arguments);
        if (context != null) {
            context.putAttribute(
                    StoryContinueGenerationToolContract.TRANSFER_DATA_JSON,
                    JSON.toJSONString(transferData)
            );
        }
        StoryContinueGenerationToolContract.markContinueRequested(context);
        ToolCallResult result = ToolCallResult.success("故事设定已确认，继续生成背景与场景", transferData);
        result.setPayload(new LinkedHashMap<>(transferData));
        return result;
    }
}
