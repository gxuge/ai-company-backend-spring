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
 * 故事背景图片生成节点。
 *
 * <p>基于已确认的故事核心，生成故事场景背景图片。</p>
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
        definition.setName("故事场景背景生成");
        definition.setDescription("通过自然对话收集或补全故事场景的核心地点、环境氛围和关键特征，并生成故事场景背景图片。");
        definition.setSkillDomain("story");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("story_create_background"));
        definition.setTools(List.of(StoryTaskToolSpec.STORY_GENERATE_SCENE_IMAGE));
        definition.setPermissions(List.of(StoryTaskToolSpec.STORY_GENERATE_SCENE_IMAGE));
        definition.setResponseFormat("text");
        definition.setConversationHistoryEnabled(true);
        definition.setUserPromptTemplate("""
                本轮输入：
                {{user_input}}
                """);
        definition.getMetadata().put("flow", "create-story");
        definition.getMetadata().put("stage", "background");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        return StoryTaskPromptSupport.baseVariables(context);
    }

    @Override
    protected AIChatParams buildChatParams(AgentContext context, SkillLoadResult skillLoadResult) {
        AIChatParams params = super.buildChatParams(context, skillLoadResult);
        if (params.getTools() == null) {
            params.setTools(new LinkedHashMap<>());
        }
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
        result.put("storySceneImageResultJson", oConvertUtils.getString(
                context == null ? null : context.getAttribute("storySceneImageResultJson")));
        return result;
    }

    /**
     * 构建故事场景背景图片生成工具规格。
     */
    private ToolSpecification buildStorySceneImageSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty(
                        "siteSetting",
                        "完整的中文故事场景描述，必须包含核心地点、环境氛围和关键特征")
                .addStringProperty("referenceImageUrl", "参考图片地址，可为空")
                .required("siteSetting")
                .build();
        return ToolSpecification.builder()
                .name(StoryTaskToolSpec.STORY_GENERATE_SCENE_IMAGE)
                .description("根据完整故事场景描述生成背景图片，参考图可选")
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
