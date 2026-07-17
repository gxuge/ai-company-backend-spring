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
 * <p>负责收集信息、生成角色核心设定，并通过 Tool 显式发起用户确认。</p>
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
        definition.setDescription("围绕创建角色收集信息、追问或生成核心设定。");
        definition.setSkillDomain("role");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("role_create_dialog"));
        definition.setTools(List.of(
                RoleTaskToolSpec.ROLE_CORE_FILL,
                RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION
        ));
        definition.setPermissions(List.of(
                RoleTaskToolSpec.ROLE_CORE_FILL,
                RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION
        ));
        definition.setResponseFormat("text");
        definition.setConversationHistoryEnabled(true);
        definition.setUserPromptTemplate("""
                主 Agent 初始委托（仅作为任务背景）：
                {{task_description}}

                本轮用户最新输入（请结合上面的历史对话优先处理）：
                {{user_input}}
                """);
        definition.getMetadata().put("flow", "create-role");
        definition.getMetadata().put("stage", "dialog");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        return RoleTaskPromptSupport.baseVariables(context);
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
        tools.put(buildRoleCoreFillSpec(), buildToolExecutor(context, RoleTaskToolSpec.ROLE_CORE_FILL));
        tools.put(
                buildRoleRequestConfirmationSpec(),
                buildToolExecutor(context, RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION)
        );
        return tools;
    }

    private ToolSpecification buildRoleCoreFillSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("userInput", "用户原始输入或本次任务描述")
                .addStringProperty("roleName", "角色名称，可为空")
                .addStringProperty("gender", "角色性别，可为空")
                .addStringProperty("occupation", "角色职业，可为空")
                .addStringProperty("backgroundStory", "角色背景故事，可为空")
                .addStringProperty("greeting", "角色开场白，可为空")
                .addStringProperty("styleHint", "角色风格提示，可为空")
                .addStringProperty("keywords", "角色关键词，可为空")
                .addStringProperty("extraInfo", "不属于核心字段的其它有效角色信息，可为空")
                .build();
        return ToolSpecification.builder()
                .name(RoleTaskToolSpec.ROLE_CORE_FILL)
                .description("根据用户已提供的信息生成或修改角色核心设定，不生成形象或声音")
                .parameters(schema)
                .build();
    }

    private ToolSpecification buildRoleRequestConfirmationSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("question", "结合当前角色内容生成的简短确认问题")
                .addStringProperty("summary", "本次角色设定的简短摘要，可为空")
                .required("question")
                .build();
        return ToolSpecification.builder()
                .name(RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION)
                .description("角色核心设定已生成且适合交给用户确认时，发起确认并暂停当前角色流程")
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

    private boolean hasRoleCoreState(AgentContext context) {
        if (context == null) {
            return false;
        }
        return context.getAttribute("roleCoreResultJson") != null
                || context.getAttribute("roleCorePresetResultJson") != null
                || context.getAttribute("roleGenerateRoleResultJson") != null;
    }
}
