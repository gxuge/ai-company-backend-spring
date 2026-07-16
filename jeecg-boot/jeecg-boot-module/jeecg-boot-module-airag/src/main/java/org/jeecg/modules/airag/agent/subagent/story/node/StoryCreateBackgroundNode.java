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
        definition.setTools(List.of(StoryTaskToolSpec.STORY_GENERATE_SCENE));
        definition.setPermissions(List.of(StoryTaskToolSpec.STORY_GENERATE_SCENE));
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
        params.getTools().put(buildStoryBackgroundSpec(), buildToolExecutor(context));
        return params;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("stage", "background");
        result.put("storySceneResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("storySceneResultJson")));
        result.put("storyBackgroundResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("storyBackgroundResultJson")));
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

    private ToolExecutor buildToolExecutor(AgentContext context) {
        return (toolExecutionRequest, memoryId) -> {
            ToolCallRequest request = new ToolCallRequest();
            request.setToolName(StoryTaskToolSpec.STORY_GENERATE_SCENE);
            request.setArguments(parseArguments(toolExecutionRequest == null ? null : toolExecutionRequest.arguments()));
            ToolCallResult result = executeToolWithSse(context, this.toolRegistry, request);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", result == null ? null : result.isSuccess());
            payload.put("summary", result == null ? null : result.getSummary());
            payload.put("data", result == null ? null : result.getData());
            payload.put("errorMessage", result == null ? null : result.getErrorMessage());
            return JSON.toJSONString(payload);
        };
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
