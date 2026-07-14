package org.jeecg.modules.airag.agent.subagent.role.node;

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
import org.jeecg.modules.airag.agent.subagent.role.RoleTaskPromptSupport;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleTaskToolSpec;
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
 * 角色创建对话节点。
 *
 * <p>负责收集信息、判断追问 / preset / full，并把核心设定结果交给后续确认节点。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class RoleCreateDialogNode extends LlmNode {

    private final ToolRegistry toolRegistry;

    public RoleCreateDialogNode(IAiragPromptTemplateService promptTemplateService,
                                AgentModelResolver modelResolver,
                                IAIChatHandler aiChatHandler,
                                AgentEventPublisher eventPublisher,
                                ToolRegistry toolRegistry) {
        super(
                "role_create_dialog",
                "角色创建对话",
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
        definition.setName("角色创建对话");
        definition.setDescription("围绕创建角色收集信息、追问或调用 preset/full 生成核心设定。");
        definition.setSkillDomain("role");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("role_create_dialog"));
        definition.setTools(List.of(
                RoleTaskToolSpec.ROLE_CORE_FILL_PRESET,
                RoleTaskToolSpec.ROLE_GENERATE_ROLE
        ));
        definition.setPermissions(List.of(
                RoleTaskToolSpec.ROLE_CORE_FILL_PRESET,
                RoleTaskToolSpec.ROLE_GENERATE_ROLE
        ));
        definition.setResponseFormat("text");
        definition.setSystemPromptTemplate("""
                你是角色创建对话节点。
                你的目标是根据用户输入和上下文，决定是追问一个最关键问题，还是调用 preset/full 工具生成角色核心设定。
                信息很少时优先走 preset；信息较完整时优先走 full；只有一个关键缺口时只问一个问题。
                角色生成后的用户确认由后续确认节点处理，本节点不判断确认动作。
                输出要简短自然，适合继续对话。
                """);
        definition.setUserPromptTemplate("""
                当前用户输入：
                {{user_input}}

                主 Agent 委托任务：
                {{task_description}}

                最近对话：
                {{recent_messages_block}}
                """);
        definition.getMetadata().put("flow", "create-role");
        definition.getMetadata().put("stage", "dialog");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = RoleTaskPromptSupport.baseVariables(context);
        RoleTaskPromptSupport.appendRoleCoreVariables(variables, context);
        return variables;
    }

    @Override
    protected AIChatParams buildChatParams(AgentContext context, SkillLoadResult skillLoadResult) {
        AIChatParams params = super.buildChatParams(context, skillLoadResult);
        Map<ToolSpecification, ToolExecutor> roleTools = buildRoleToolMap(context);
        if (!roleTools.isEmpty()) {
            if (params.getTools() == null) {
                params.setTools(new LinkedHashMap<>());
            }
            params.getTools().putAll(roleTools);
        }
        return params;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("stage", "dialog");
        result.put("hasRoleCoreState", hasRoleCoreState(context));
        result.put("roleCoreResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("roleCoreResultJson")));
        result.put("roleGenerateRoleResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("roleGenerateRoleResultJson")));
        return result;
    }

    private Map<ToolSpecification, ToolExecutor> buildRoleToolMap(AgentContext context) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        tools.put(buildRoleCoreFillPresetSpec(), buildToolExecutor(context, RoleTaskToolSpec.ROLE_CORE_FILL_PRESET));
        tools.put(buildRoleGenerateRoleSpec(), buildToolExecutor(context, RoleTaskToolSpec.ROLE_GENERATE_ROLE));
        return tools;
    }

    private ToolSpecification buildRoleCoreFillPresetSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("userInput", "用户原始输入或本次任务描述")
                .addStringProperty("roleName", "角色名称，可为空")
                .addStringProperty("gender", "性别，可为空，建议 male/female/random")
                .addStringProperty("occupation", "职业或身份，可为空")
                .addStringProperty("backgroundStory", "角色背景故事或用户给出的设定方向，可为空")
                .addStringProperty("greeting", "角色开场白，可为空")
                .addStringProperty("styleHint", "风格提示，可为空")
                .addStringProperty("keywords", "关键词，可为空")
                .build();
        return ToolSpecification.builder()
                .name(RoleTaskToolSpec.ROLE_CORE_FILL_PRESET)
                .description("信息很少或用户想随机生成角色时，生成一版角色核心设定")
                .parameters(schema)
                .build();
    }

    private ToolSpecification buildRoleGenerateRoleSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("userInput", "用户原始输入或本次任务描述")
                .addStringProperty("storySetting", "角色相关故事设定或关系背景")
                .addStringProperty("storyBackground", "角色背景、用户要求或已有设定")
                .build();
        return ToolSpecification.builder()
                .name(RoleTaskToolSpec.ROLE_GENERATE_ROLE)
                .description("信息较完整或用户明确要求按现有方向生成/修改角色时，生成完整角色设定")
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

    private boolean hasRoleCoreState(AgentContext context) {
        if (context == null) {
            return false;
        }
        return context.getAttribute("roleCoreResultJson") != null
                || context.getAttribute("roleCorePresetResultJson") != null
                || context.getAttribute("roleGenerateRoleResultJson") != null;
    }
}
