package org.jeecg.modules.airag.agent.example;

import org.jeecg.common.util.UUIDGenerator;
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
        this.toolRegistry.register(buildSaveDraftTool());
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
        definition.setDescription("根据用户参数生成故事草稿");
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
        definition.setDisplayName("默认参数生成故事");
        definition.setDescription("使用默认参数生成故事草稿");
        definition.setExecutor(this::executeGenerateStoryWithDefaults);
        return definition;
    }

    /**
     * 构造保存草稿工具。
     *
     * @return 工具定义
     */
    private ToolDefinition buildSaveDraftTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("save_draft");
        definition.setDisplayName("保存草稿");
        definition.setDescription("保存故事草稿");
        definition.setExecutor(this::executeSaveDraft);
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
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("storyId", UUIDGenerator.generate());
        draft.put("toolArgs", request.getArguments().get("toolArgs"));
        draft.put("summary", "已根据用户参数生成故事草稿");
        context.putAttribute("storyDraft", draft);
        return ToolCallResult.success("故事草稿生成完成", draft);
    }

    /**
     * 执行默认生成故事工具。
     *
     * @param context 运行上下文
     * @param request 工具请求
     * @return 工具结果
     */
    private ToolCallResult executeGenerateStoryWithDefaults(AgentContext context, ToolCallRequest request) {
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("storyId", UUIDGenerator.generate());
        draft.put("mode", "default");
        draft.put("summary", "已使用默认参数生成故事草稿");
        context.putAttribute("storyDraft", draft);
        return ToolCallResult.success("默认故事草稿生成完成", draft);
    }

    /**
     * 执行保存草稿工具。
     *
     * @param context 运行上下文
     * @param request 工具请求
     * @return 工具结果
     */
    private ToolCallResult executeSaveDraft(AgentContext context, ToolCallRequest request) {
        Map<String, Object> saved = new LinkedHashMap<>();
        saved.put("draftId", UUIDGenerator.generate());
        saved.put("messageId", request.getArguments().get("messageId"));
        saved.put("saved", Boolean.TRUE);
        context.putAttribute("savedDraft", saved);
        return ToolCallResult.success("故事草稿保存完成", saved);
    }
}
