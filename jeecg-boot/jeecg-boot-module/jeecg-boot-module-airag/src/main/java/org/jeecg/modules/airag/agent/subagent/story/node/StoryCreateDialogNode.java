package org.jeecg.modules.airag.agent.subagent.story.node;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
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
 * <p>负责围绕故事核心字段进行追问、preset/full 决策和结果确认引导。</p>
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
        definition.setDescription("围绕创建故事收集信息、追问或调用 preset/full 生成核心设定。");
        definition.setSkillDomain("story");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("story_create_dialog"));
        definition.setTools(List.of(
                StoryTaskToolSpec.STORY_FULL_GENERATE_PRESET,
                StoryTaskToolSpec.STORY_FULL_GENERATE,
                StoryTaskToolSpec.STORY_CONFIRMATION_DECISION
        ));
        definition.setPermissions(List.of(
                StoryTaskToolSpec.STORY_FULL_GENERATE_PRESET,
                StoryTaskToolSpec.STORY_FULL_GENERATE,
                StoryTaskToolSpec.STORY_CONFIRMATION_DECISION
        ));
        definition.setResponseFormat("text");
        definition.setSystemPromptTemplate("""
                你是故事创建对话节点。
                你的目标是根据用户输入和上下文，决定是追问一个最关键问题，还是调用 preset/full 工具生成故事核心设定。
                信息很少时优先走 preset；信息较完整时优先走 full；只有一个关键缺口时只问一个问题。
                如果上下文中已有故事核心设定，先判断用户是在确认继续、重新生成、局部修改，还是意图不明确。
                已有故事核心且 story_confirmation_action 为空时，必须调用 story_confirmation_decision 工具输出确认判断，不要普通文本回答，不要调用生成工具。
                如果 story_confirmation_action 为 REGENERATE 或 MODIFY，则按用户最新要求继续重新生成或修改，不再调用确认工具。
                生成后要继续确认用户是否满意，并为后续故事背景节点保留可用的核心信息。
                输出要简短自然，适合继续对话。
                """);
        definition.setUserPromptTemplate("""
                当前用户输入：
                {{user_input}}

                会话摘要：
                {{session_summary}}

                最近对话：
                {{recent_messages_block}}

                已确认字段：
                {{confirmed_fields_json}}

                缺失字段：
                {{missing_fields_json}}

                已有故事核心：
                {{story_core_result_json}}

                当前确认动作：
                {{story_confirmation_action}}
                """);
        definition.getMetadata().put("flow", "create-story");
        definition.getMetadata().put("stage", "dialog");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = StoryTaskPromptSupport.baseVariables(context);
        StoryTaskPromptSupport.appendStoryCoreVariables(variables, context);
        variables.put("story_confirmation_action", oConvertUtils.getString(context == null ? null : context.getAttribute("storyConfirmationAction")));
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
        Object confirmationDecision = context == null ? null : context.getAttribute("storyConfirmationDecision");
        if (confirmationDecision instanceof Map<?, ?> decision) {
            result.put("confirmationDecision", copyStringKeyMap(decision));
            result.put("action", decision.get("action"));
            result.put("reply", decision.get("reply"));
            result.put("options", decision.get("options"));
            result.put("reason", decision.get("reason"));
        }
        return result;
    }

    private Map<ToolSpecification, ToolExecutor> buildStoryToolMap(AgentContext context) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        tools.put(buildStoryFullGeneratePresetSpec(), buildToolExecutor(context, StoryTaskToolSpec.STORY_FULL_GENERATE_PRESET));
        tools.put(buildStoryFullGenerateSpec(), buildToolExecutor(context, StoryTaskToolSpec.STORY_FULL_GENERATE));
        tools.put(buildStoryConfirmationDecisionSpec(), buildToolExecutor(context, StoryTaskToolSpec.STORY_CONFIRMATION_DECISION));
        return tools;
    }

    private ToolSpecification buildStoryFullGeneratePresetSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("userInput", "用户原始输入或本次任务描述")
                .addStringProperty("title", "故事标题，可为空")
                .addStringProperty("storyMode", "故事模式，可为空，normal 或 chapter")
                .addStringProperty("storyIntro", "故事简介，可为空")
                .addStringProperty("storySetting", "故事设定，可为空")
                .addStringProperty("siteSetting", "场景设定，可为空")
                .addStringProperty("plotOutline", "剧情大纲，可为空")
                .build();
        return ToolSpecification.builder()
                .name(StoryTaskToolSpec.STORY_FULL_GENERATE_PRESET)
                .description("信息很少或用户想随机生成故事时，生成一版故事核心设定")
                .parameters(schema)
                .build();
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

    private ToolSpecification buildStoryConfirmationDecisionSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addProperty("action", JsonEnumSchema.builder()
                        .description("确认动作")
                        .enumValues(List.of("ACCEPT_AND_CONTINUE", "REGENERATE", "MODIFY", "ASK_USER"))
                        .build())
                .addStringProperty("reply", "给用户看的简短回复")
                .addProperty("options", JsonArraySchema.builder()
                        .description("给用户展示的两个选择")
                        .items(JsonStringSchema.builder().description("单个选择文案").build())
                        .build())
                .addStringProperty("reason", "简短说明判断依据")
                .required("action", "reply", "options", "reason")
                .build();
        return ToolSpecification.builder()
                .name(StoryTaskToolSpec.STORY_CONFIRMATION_DECISION)
                .description("已有故事核心设定后，由模型判断用户是接受继续、重新生成、局部修改，还是需要展示选择")
                .parameters(schema)
                .build();
    }

    private ToolExecutor buildToolExecutor(AgentContext context, String toolName) {
        return (toolExecutionRequest, memoryId) -> {
            ToolCallRequest request = new ToolCallRequest();
            request.setToolName(toolName);
            request.setArguments(parseArguments(toolExecutionRequest == null ? null : toolExecutionRequest.arguments()));
            ToolCallResult result = this.toolRegistry.execute(context, request);
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

    private Map<String, Object> copyStringKeyMap(Map<?, ?> rawMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (rawMap == null) {
            return map;
        }
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
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
