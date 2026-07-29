package org.jeecg.modules.airag.agent.subagent.role.node;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
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
 * 角色形象生成节点。
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class RoleCreateImageNode extends LlmNode {

    private final ToolRegistry toolRegistry;

    public RoleCreateImageNode(IAiragPromptTemplateService promptTemplateService,
                               AgentModelResolver modelResolver,
                               IAIChatHandler aiChatHandler,
                               AgentEventPublisher eventPublisher,
                               ToolRegistry toolRegistry) {
        super(
                "role_create_image",
                "角色形象生成",
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
        definition.setName("角色形象生成");
        definition.setDescription("基于已确认的角色核心设定，生成适合继续出图的形象描述。");
        definition.setSkillDomain("role");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("role_create_image"));
        definition.setTools(List.of(RoleTaskToolSpec.ROLE_GENERATE_ROLE_IMAGE));
        definition.setPermissions(List.of(RoleTaskToolSpec.ROLE_GENERATE_ROLE_IMAGE));
        definition.setResponseFormat("text");
        definition.setUserPromptTemplate("""
                当前角色核心：
                {{role_core_result_json}}

                角色名称：
                {{role_name}}

                性别：
                {{gender}}

                职业：
                {{occupation}}

                背景故事：
                {{background_story}}

                开场白：
                {{greeting}}
                """);
        definition.getMetadata().put("flow", "create-role");
        definition.getMetadata().put("stage", "image");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = RoleTaskPromptSupport.baseVariables(context);
        RoleTaskPromptSupport.appendRoleImageVariables(variables, context);
        return variables;
    }

    @Override
    protected AIChatParams buildChatParams(AgentContext context, SkillLoadResult skillLoadResult) {
        AIChatParams params = super.buildChatParams(context, skillLoadResult);
        if (params.getTools() == null) {
            params.setTools(new LinkedHashMap<>());
        }
        params.getTools().put(buildRoleImageSpec(), buildToolExecutor(context));
        return params;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("stage", "image");
        result.put("roleImageResultJson", org.jeecg.common.util.oConvertUtils.getString(context == null ? null : context.getAttribute("roleImageResultJson")));
        return result;
    }

    private ToolSpecification buildRoleImageSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("roleId", "已有角色 ID，可为空")
                .addStringProperty("roleName", "角色名称，可为空")
                .addStringProperty("gender", "角色性别，可为空")
                .addStringProperty("occupation", "角色职业或身份，可为空")
                .addStringProperty("backgroundStory", "角色背景、外观要求或本次生成任务描述")
                .addStringProperty("styleName", "期望的画面风格，可为空")
                .addStringProperty("aspectRatio", "期望的画面比例，可为空")
                .addStringProperty("referenceImageUrl", "参考图地址，可为空")
                .build();
        return ToolSpecification.builder()
                .name(RoleTaskToolSpec.ROLE_GENERATE_ROLE_IMAGE)
                .description("根据本次任务描述和已有角色设定生成角色形象")
                .parameters(schema)
                .build();
    }

    private ToolExecutor buildToolExecutor(AgentContext context) {
        return (toolExecutionRequest, memoryId) -> {
            ToolCallRequest request = new ToolCallRequest();
            request.setToolName(RoleTaskToolSpec.ROLE_GENERATE_ROLE_IMAGE);
            request.setArguments(parseArguments(toolExecutionRequest == null ? null : toolExecutionRequest.arguments()));
            ToolCallResult result = executeToolWithSse(context, this.toolRegistry, request);
            return JSON.toJSONString(buildToolResponse(result));
        };
    }

    private Map<String, Object> buildToolResponse(ToolCallResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result == null ? null : result.isSuccess());
        response.put("summary", result == null ? null : result.getSummary());
        response.put("contentType", result == null ? null : result.getContentType());
        response.put("resourceType", result == null ? null : result.getResourceType());
        response.put("imageUrl", result == null ? null : result.getImageUrl());
        response.put("promptCode", result == null ? null : result.getPromptCode());
        response.put("promptVersion", result == null ? null : result.getPromptVersion());
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
