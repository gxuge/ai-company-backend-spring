package org.jeecg.modules.system.agent.task.story;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryGenerateCompleteToolContract;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 完整故事异步生成工具注册器。
 */
@Component
@RequiredArgsConstructor
public class StoryGenerateCompleteToolRegistrar {
    private static final String ROUTE_STORY_GENERATE_COMPLETE = "STORY_GENERATE_COMPLETE";

    private final ToolRegistry toolRegistry;
    private final StoryGenerateCompleteAsyncService asyncService;

    @PostConstruct
    void registerTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StoryTaskToolSpec.STORY_GENERATE_COMPLETE);
        definition.setDisplayName("完整故事生成");
        definition.setDescription("异步创建新角色、生成故事后续内容并保存完整故事");
        definition.setRouteKey(ROUTE_STORY_GENERATE_COMPLETE);
        definition.setCategory("story_task");
        definition.setInputSchema(StoryGenerateCompleteToolContract.inputSchema());
        definition.setRetryable(false);
        definition.setAsynchronous(true);
        definition.setExecutor(this::submitGeneration);
        this.toolRegistry.register(definition);
    }

    private ToolCallResult submitGeneration(AgentContext context, ToolCallRequest request) {
        Map<String, Object> arguments = request == null || request.getArguments() == null
                ? Map.of()
                : request.getArguments();
        Map<String, Object> transferData = StoryGenerateCompleteToolContract.requireTransferData(arguments);
        return this.asyncService.submit(context, request, transferData);
    }
}
