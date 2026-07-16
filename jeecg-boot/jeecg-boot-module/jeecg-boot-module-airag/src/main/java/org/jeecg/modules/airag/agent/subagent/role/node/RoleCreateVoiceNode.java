package org.jeecg.modules.airag.agent.subagent.role.node;

import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.agent.subagent.role.RoleTaskPromptSupport;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleTaskToolSpec;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 角色声音生成节点。
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class RoleCreateVoiceNode extends LlmNode {

    public RoleCreateVoiceNode(IAiragPromptTemplateService promptTemplateService,
                               AgentModelResolver modelResolver,
                               IAIChatHandler aiChatHandler,
                               AgentEventPublisher eventPublisher) {
        super(
                "role_create_voice",
                "角色声音生成",
                buildDefinition(),
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
    }

    private static LlmNodeDefinition buildDefinition() {
        LlmNodeDefinition definition = new LlmNodeDefinition();
        definition.setName("角色声音生成");
        definition.setDescription("基于已确认的角色核心与形象，生成适合角色的声音建议。");
        definition.setSkillDomain("role");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("role_create_voice"));
        definition.setTools(List.of(RoleTaskToolSpec.ROLE_GENERATE_ROLE_VOICE));
        definition.setPermissions(List.of(RoleTaskToolSpec.ROLE_GENERATE_ROLE_VOICE));
        definition.setResponseFormat("text");
        definition.setUserPromptTemplate("""
                当前角色核心：
                {{role_core_result_json}}

                当前角色形象：
                {{role_image_result_json}}

                角色名称：
                {{role_name}}

                职业：
                {{occupation}}

                背景故事：
                {{background_story}}

                音色建议：
                {{voice_name}}
                """);
        definition.getMetadata().put("flow", "create-role");
        definition.getMetadata().put("stage", "voice");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = RoleTaskPromptSupport.baseVariables(context);
        RoleTaskPromptSupport.appendRoleVoiceVariables(variables, context);
        return variables;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("stage", "voice");
        result.put("roleVoiceResultJson", org.jeecg.common.util.oConvertUtils.getString(context == null ? null : context.getAttribute("roleVoiceResultJson")));
        return result;
    }
}
