package org.jeecg.modules.airag.agent.subagent.role.node;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.ToolSpecification;
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
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleConfirmationToolContract;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleContinueGenerationToolContract;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.llm.stream.ImmediateToolExecutor;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色创建对话节点。
 *
 * <p>负责收集并完善角色核心设定，通过 Tool 显式传递最终数据并发起用户确认。</p>
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
        definition.setDescription("围绕创建角色收集信息、追问并整理最终核心设定。");
        definition.setSkillDomain("role");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("role_create_dialog"));
        definition.setTools(List.of(
                RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION,
                RoleTaskToolSpec.ROLE_CONTINUE_GENERATION
        ));
        definition.setPermissions(List.of(
                RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION,
                RoleTaskToolSpec.ROLE_CONTINUE_GENERATION
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
        String transferDataJson = oConvertUtils.getString(
                context == null ? null : context.getAttribute(RoleContinueGenerationToolContract.TRANSFER_DATA_JSON)
        );
        result.put("hasTransferDataState", oConvertUtils.isNotEmpty(transferDataJson));
        result.put(RoleContinueGenerationToolContract.TRANSFER_DATA_JSON, transferDataJson);
        return result;
    }

    private Map<ToolSpecification, ToolExecutor> buildRoleToolMap(AgentContext context) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        tools.put(
                RoleConfirmationToolContract.buildSpecification(),
                ImmediateToolExecutor.wrap(
                        buildToolExecutor(context, RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION)
                )
        );
        tools.put(
                RoleContinueGenerationToolContract.buildSpecification(),
                ImmediateToolExecutor.wrap(
                        buildToolExecutor(context, RoleTaskToolSpec.ROLE_CONTINUE_GENERATION)
                )
        );
        return tools;
    }

    private ToolExecutor buildToolExecutor(AgentContext context, String toolName) {
        return (toolExecutionRequest, memoryId) -> {
            ToolCallRequest request = new ToolCallRequest();
            request.setToolName(toolName);
            request.setArguments(parseArguments(toolExecutionRequest == null ? null : toolExecutionRequest.arguments()));
            ToolCallResult result = executeToolWithSse(context, this.toolRegistry, request);
            if (RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION.equals(toolName)
                    || RoleTaskToolSpec.ROLE_CONTINUE_GENERATION.equals(toolName)) {
                suppressRemainingLlmOutput(context);
            }
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
