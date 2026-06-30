package org.jeecg.modules.airag.agent.example;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CreateStoryAgent 示例工具注册器。
 *
 * @author codex
 * @date 2026/6/16
 */
@Component
public class CreateStoryToolRegistrar {
    /**
     * 工具注册中心。
     */
    private final ToolRegistry toolRegistry;

    /**
     * 构造函数。
     *
     * @param toolRegistry 工具注册中心
     */
    public CreateStoryToolRegistrar(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 注册示例工具。
     */
    @PostConstruct
    public void registerTools() {
        this.toolRegistry.register(buildGenerateStoryTool());
        this.toolRegistry.register(buildGenerateStoryWithDefaultsTool());
    }

    /**
     * 构造生成故事工具。
     *
     * @return 工具定义
     */
    private ToolDefinition buildGenerateStoryTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("generate_story");
        definition.setDisplayName("生成故事");
        definition.setDescription("根据用户已提供的信息组装故事生成请求");
        definition.setExecutor(this::executeGenerateStory);
        return definition;
    }

    /**
     * 构造默认生成故事工具。
     *
     * @return 工具定义
     */
    private ToolDefinition buildGenerateStoryWithDefaultsTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("generate_story_with_defaults");
        definition.setDisplayName("预设生成故事");
        definition.setDescription("在缺少明确信息时直接走故事 preset 生成");
        definition.setExecutor(this::executeGenerateStoryWithDefaults);
        return definition;
    }

    /**
     * 执行生成故事工具。
     *
     * @param context 运行上下文
     * @param request 工具请求
     * @return 工具结果
     */
    private ToolCallResult executeGenerateStory(AgentContext context, ToolCallRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "full-generate");
        result.put("endpoint", "/sys/ts-stories/story-full-generate");
        result.put("toolArgs", request.getArguments().get("toolArgs"));
        result.put("summary", "已根据当前信息组装故事生成请求");
        context.putAttribute("storyGenerateResult", result);
        return ToolCallResult.success("故事生成请求已准备完成", result);
    }

    /**
     * 执行默认生成故事工具。
     *
     * @param context 运行上下文
     * @param request 工具请求
     * @return 工具结果
     */
    private ToolCallResult executeGenerateStoryWithDefaults(AgentContext context, ToolCallRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "full-generate-preset");
        result.put("endpoint", "/sys/ts-stories/story-full-generate-preset");
        result.put("toolArgs", request.getArguments().get("toolArgs"));
        result.put("summary", "信息不足，已切换为故事 preset 生成");
        context.putAttribute("storyGenerateResult", result);
        return ToolCallResult.success("故事 preset 生成请求已准备完成", result);
    }
}
