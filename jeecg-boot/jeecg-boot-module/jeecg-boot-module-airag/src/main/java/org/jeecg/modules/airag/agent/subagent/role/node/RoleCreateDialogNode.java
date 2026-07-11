package org.jeecg.modules.airag.agent.subagent.role.node;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.agent.subagent.role.RoleTaskPromptSupport;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 角色创建对话节点。
 *
 * <p>负责收集信息、判断追问 / preset / full，并把核心设定结果交给后续门禁节点。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class RoleCreateDialogNode extends LlmNode {

    public RoleCreateDialogNode(IAiragPromptTemplateService promptTemplateService,
                                AgentModelResolver modelResolver,
                                IAIChatHandler aiChatHandler,
                                AgentEventPublisher eventPublisher) {
        super(
                "role_create_dialog",
                "角色创建对话",
                buildDefinition(),
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
    }

    private static LlmNodeDefinition buildDefinition() {
        LlmNodeDefinition definition = new LlmNodeDefinition();
        definition.setName("角色创建对话");
        definition.setDescription("围绕创建角色收集信息、追问或调用 preset/full 生成核心设定。");
        definition.setSkillDomain("role");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("role_create_dialog"));
        definition.setTools(List.of("role_core_fill_preset", "role_generate_role"));
        definition.setPermissions(List.of("role_core_fill_preset", "role_generate_role"));
        definition.setResponseFormat("text");
        definition.setSystemPromptTemplate("""
                你是角色创建对话节点。
                你的目标是根据用户输入和上下文，决定是追问一个最关键问题，还是调用 preset/full 工具生成角色核心设定。
                信息很少时优先走 preset；信息较完整时优先走 full；只有一个关键缺口时只问一个问题。
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

                已有角色核心：
                {{role_core_result_json}}
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
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("stage", "dialog");
        result.put("hasRoleCoreState", hasRoleCoreState(context));
        result.put("roleCoreResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("roleCoreResultJson")));
        result.put("roleGenerateRoleResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("roleGenerateRoleResultJson")));
        return result;
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
