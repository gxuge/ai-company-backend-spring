package org.jeecg.modules.airag.agent.subagent.story.tool;

import jakarta.annotation.PostConstruct;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 故事流程交互工具注册器。
 *
 * @author codex
 * @date 2026/7/17
 */
@Component
public class StoryInteractionToolRegistrar {
    private static final String ROUTE_STORY_REQUEST_CONFIRMATION = "STORY_REQUEST_CONFIRMATION";

    private final ToolRegistry toolRegistry;

    public StoryInteractionToolRegistrar(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 注册故事确认工具。
     */
    @PostConstruct
    void registerTools() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StoryTaskToolSpec.STORY_REQUEST_CONFIRMATION);
        definition.setDisplayName("请求故事确认");
        definition.setDescription("生成亲切简短的确认问题和两个候选文案，仅用于前端展示和收集用户回复");
        definition.setRouteKey(ROUTE_STORY_REQUEST_CONFIRMATION);
        definition.setCategory("story_task");
        definition.setInputSchema(StoryConfirmationToolContract.inputSchema());
        definition.setRetryable(false);
        definition.setExecutor(this::requestConfirmation);
        this.toolRegistry.register(definition);
    }

    /**
     * 创建待确认交互，展示文案由模型提供。
     */
    private ToolCallResult requestConfirmation(AgentContext context, ToolCallRequest request) {
        Map<String, Object> arguments = request == null || request.getArguments() == null
                ? Map.of()
                : request.getArguments();
        Map<String, String> displayCopy = StoryConfirmationToolContract.requireDisplayCopy(arguments);
        String question = displayCopy.get(StoryConfirmationToolContract.QUESTION);
        List<Map<String, String>> options = List.of(
                Map.of(
                        "label", displayCopy.get(StoryConfirmationToolContract.CONFIRM_LABEL),
                        "value", "ACCEPT_AND_CONTINUE"
                ),
                Map.of(
                        "label", displayCopy.get(StoryConfirmationToolContract.REVISE_LABEL),
                        "value", "REGENERATE"
                )
        );
        String sourceNode = context == null ? null : context.getCurrentNodeName();
        Map<String, Object> interaction = UserInteractionSupport.createPending(
                context,
                "confirm",
                StoryTaskToolSpec.STORY_REQUEST_CONFIRMATION,
                sourceNode,
                "story_create_dialog",
                question,
                null,
                options
        );
        ToolCallResult result = ToolCallResult.success(question, interaction);
        result.setPayload(new LinkedHashMap<>(interaction));
        return result;
    }
}
