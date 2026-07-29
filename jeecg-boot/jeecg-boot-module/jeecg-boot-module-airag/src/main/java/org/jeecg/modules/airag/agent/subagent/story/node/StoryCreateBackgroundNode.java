package org.jeecg.modules.airag.agent.subagent.story.node;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.agent.skill.model.SkillLoadResult;
import org.jeecg.modules.airag.agent.subagent.story.StoryTaskPromptSupport;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 故事背景生成节点。
 *
 * <p>基于已确认的故事核心，生成适合继续推进的背景 / 场景设定。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class StoryCreateBackgroundNode extends LlmNode {

    private final ToolRegistry toolRegistry;

    public StoryCreateBackgroundNode(IAiragPromptTemplateService promptTemplateService,
                                     AgentModelResolver modelResolver,
                                     IAIChatHandler aiChatHandler,
                                     AgentEventPublisher eventPublisher,
                                     ToolRegistry toolRegistry) {
        super(
                "story_create_background",
                "故事背景生成",
                buildDefinition(),
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
        this.toolRegistry = toolRegistry;
    }

    private static LlmNodeDefinition buildDefinition() {
        LlmNodeDefinition definition = new LlmNodeDefinition();
        definition.setName("故事背景生成");
        definition.setDescription("基于已确认的故事核心设定，生成适合继续推进的背景与场景描述。");
        definition.setSkillDomain("story");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("story_create_background"));
        definition.setTools(List.of(
                StoryTaskToolSpec.STORY_GENERATE_SCENE,
                StoryTaskToolSpec.STORY_GENERATE_SCENE_IMAGE
        ));
        definition.setPermissions(List.of(
                StoryTaskToolSpec.STORY_GENERATE_SCENE,
                StoryTaskToolSpec.STORY_GENERATE_SCENE_IMAGE
        ));
        definition.setResponseFormat("text");
        definition.setUserPromptTemplate("""
                当前故事核心：
                {{story_core_result_json}}

                当前背景设定：
                {{story_background_result_json}}

                标题：
                {{title}}

                故事模式：
                {{story_mode}}

                故事简介：
                {{story_intro}}

                故事设定：
                {{story_setting}}

                场景设定：
                {{site_setting}}

                剧情大纲：
                {{plot_outline}}

                故事背景：
                {{story_background}}
                """);
        definition.getMetadata().put("flow", "create-story");
        definition.getMetadata().put("stage", "background");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = StoryTaskPromptSupport.baseVariables(context);
        StoryTaskPromptSupport.appendStoryBackgroundVariables(variables, context);
        return variables;
    }

    @Override
    protected AIChatParams buildChatParams(AgentContext context, SkillLoadResult skillLoadResult) {
        AIChatParams params = super.buildChatParams(context, skillLoadResult);
        if (params.getTools() == null) {
            params.setTools(new LinkedHashMap<>());
        }
        params.getTools().put(
                buildStoryBackgroundSpec(),
                buildToolExecutor(context, StoryTaskToolSpec.STORY_GENERATE_SCENE));
        params.getTools().put(
                buildStorySceneImageSpec(),
                buildToolExecutor(context, StoryTaskToolSpec.STORY_GENERATE_SCENE_IMAGE));
        return params;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("stage", "background");
        result.put("storySceneResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("storySceneResultJson")));
        result.put("storyBackgroundResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("storyBackgroundResultJson")));
        result.put("storySceneImageResultJson", oConvertUtils.getString(
                context == null ? null : context.getAttribute("storySceneImageResultJson")));
        return result;
    }

    private ToolSpecification buildStoryBackgroundSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("title", "故事标题，可为空")
                .addStringProperty("storyMode", "故事模式，可为空")
                .addStringProperty("storySetting", "故事世界观或整体设定")
                .addStringProperty("storyIntro", "故事简介，可为空")
                .addStringProperty("storyBackground", "故事背景或本次生成任务描述")
                .addStringProperty("sceneSetting", "主要场景设定，可为空")
                .addStringProperty("plotOutline", "剧情大纲，可为空")
                .addStringProperty("styleHint", "期望的叙事或画面风格，可为空")
                .addStringProperty("templateMode", "模板模式，可为空")
                .build();
        return ToolSpecification.builder()
                .name(StoryTaskToolSpec.STORY_GENERATE_SCENE)
                .description("根据本次任务描述和已有故事设定生成故事背景与场景")
                .parameters(schema)
                .build();
    }

    /**
     * 构建故事场景背景图片生成工具规格。
     */
    private ToolSpecification buildStorySceneImageSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("title", "故事标题，可为空")
                .addStringProperty("storySetting", "故事世界观或整体设定")
                .addStringProperty("siteSetting", "故事主要地点或场景设定")
                .addStringProperty("plotOutline", "剧情大纲，可为空")
                .addStringProperty("styleName", "视觉风格名称，可为空")
                .addStringProperty("aspectRatio", "图片宽高比，默认9:16")
                .addStringProperty("referenceImageUrl", "参考图片地址，可为空")
                .build();
        return ToolSpecification.builder()
                .name(StoryTaskToolSpec.STORY_GENERATE_SCENE_IMAGE)
                .description("根据故事与场景设定生成临时背景图片，不保存素材或关联故事")
                .parameters(schema)
                .build();
    }

    /**
     * 构建指定故事工具的执行器。
     */
    private ToolExecutor buildToolExecutor(AgentContext context, String toolName) {
        return (toolExecutionRequest, memoryId) -> {
            ToolCallRequest request = new ToolCallRequest();
            request.setToolName(toolName);
            request.setArguments(parseArguments(toolExecutionRequest == null ? null : toolExecutionRequest.arguments()));
            ToolCallResult result = executeToolWithSse(context, this.toolRegistry, request);
            return JSON.toJSONString(buildToolResponse(result));
        };
    }

    private Map<String, Object> buildToolResponse(ToolCallResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result == null ? null : result.isSuccess());
        response.put("summary", result == null ? null : result.getSummary());
        if (result != null && "image".equalsIgnoreCase(result.getContentType())) {
            response.put("contentType", result.getContentType());
            response.put("resourceType", result.getResourceType());
            response.put("imageUrl", result.getImageUrl());
            response.put("promptCode", result.getPromptCode());
            response.put("promptVersion", result.getPromptVersion());
        } else {
            response.put("data", result == null ? null : result.getData());
        }
        response.put("errorMessage", result == null ? null : result.getErrorMessage());
        return response;
    }

    private Map<String, Object> parseArguments(String arguments) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (arguments == null || arguments.isBlank()) {
            return map;
        }
        try {
            JSONObject json = JSON.parseObject(arguments);
            if (json != null) {
                map.putAll(json);
            }
        } catch (Exception ignored) {
            // ignore invalid tool arguments
        }
        return map;
    }
}
