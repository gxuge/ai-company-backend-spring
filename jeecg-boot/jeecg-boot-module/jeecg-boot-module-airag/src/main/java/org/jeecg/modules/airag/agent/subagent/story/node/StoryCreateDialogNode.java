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
 * 故事创建对话节点。
 *
 * <p>负责围绕故事核心字段进行追问、生成决策和结果确认引导。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class StoryCreateDialogNode extends LlmNode {

    private final ToolRegistry toolRegistry;

    public StoryCreateDialogNode(IAiragPromptTemplateService promptTemplateService,
                                 AgentModelResolver modelResolver,
                                 IAIChatHandler aiChatHandler,
                                 AgentEventPublisher eventPublisher,
                                 ToolRegistry toolRegistry) {
        super(
                "story_create_dialog",
                "故事创建对话",
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
        definition.setName("故事创建对话");
        definition.setDescription("围绕创建故事收集信息、追问或生成核心设定。");
        definition.setSkillDomain("story");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("story_create_dialog"));
        definition.setTools(List.of(StoryTaskToolSpec.STORY_FULL_GENERATE));
        definition.setPermissions(List.of(StoryTaskToolSpec.STORY_FULL_GENERATE));
        definition.setResponseFormat("text");
        definition.setConversationHistoryEnabled(true);
        definition.setUserPromptTemplate("""
                主 Agent 初始委托（仅作为任务背景）：
                {{task_description}}

                本轮用户最新输入（请结合上面的历史对话优先处理）：
                {{user_input}}
                """);
        definition.getMetadata().put("flow", "create-story");
        definition.getMetadata().put("stage", "dialog");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = StoryTaskPromptSupport.baseVariables(context);
        StoryTaskPromptSupport.appendStoryCoreVariables(variables, context);
        return variables;
    }

    @Override
    protected AIChatParams buildChatParams(AgentContext context, SkillLoadResult skillLoadResult) {
        AIChatParams params = super.buildChatParams(context, skillLoadResult);
        Map<ToolSpecification, ToolExecutor> storyTools = buildStoryToolMap(context);
        if (!storyTools.isEmpty()) {
            if (params.getTools() == null) {
                params.setTools(new LinkedHashMap<>());
            }
            params.getTools().putAll(storyTools);
        }
        return params;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("stage", "dialog");
        result.put("hasStoryCoreState", hasStoryCoreState(context));
        result.put("storyCoreResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("storyCoreResultJson")));
        result.put("storyFullGenerateResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("storyFullGenerateResultJson")));
        return result;
    }

    private Map<ToolSpecification, ToolExecutor> buildStoryToolMap(AgentContext context) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        tools.put(buildStoryFullGenerateSpec(), buildToolExecutor(context, StoryTaskToolSpec.STORY_FULL_GENERATE));
        return tools;
    }

    private ToolSpecification buildStoryFullGenerateSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("userInput", "用户原始输入或本次任务描述")
                .addStringProperty("title", "故事标题，可为空")
                .addStringProperty("storyMode", "故事模式，可为空，normal 或 chapter")
                .addStringProperty("storyIntro", "故事简介，可为空")
                .addStringProperty("storySetting", "故事设定，可为空")
                .addStringProperty("siteSetting", "场景设定，可为空")
                .addStringProperty("plotOutline", "剧情大纲，可为空")
                .addStringProperty("extraInfo", "补充故事信息，可为空")
                .build();
        return ToolSpecification.builder()
                .name(StoryTaskToolSpec.STORY_FULL_GENERATE)
                .description("信息较完整或用户明确要求按现有方向生成/修改故事时，生成完整故事设定")
                .parameters(schema)
                .build();
    }

    private ToolExecutor buildToolExecutor(AgentContext context, String toolName) {
        return (toolExecutionRequest, memoryId) -> {
            ToolCallRequest request = new ToolCallRequest();
            request.setToolName(toolName);
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

    private boolean hasStoryCoreState(AgentContext context) {
        if (context == null) {
            return false;
        }
        return context.getAttribute("storyCoreResultJson") != null
                || context.getAttribute("storyCorePresetResultJson") != null
                || context.getAttribute("storyFullGenerateResultJson") != null;
    }
}
