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
 * 角色形象生成节点。
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class RoleCreateImageNode extends LlmNode {

    public RoleCreateImageNode(IAiragPromptTemplateService promptTemplateService,
                               AgentModelResolver modelResolver,
                               IAIChatHandler aiChatHandler,
                               AgentEventPublisher eventPublisher) {
        super(
                "role_create_image",
                "角色形象生成",
                buildDefinition(),
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
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
        definition.setSystemPromptTemplate("""
                你是角色形象生成节点。
                只根据已确认的角色核心设定，生成一版适合出图的形象描述。
                重点写外貌、气质、服装、姿态、画面感，不要重复追问核心字段。
                输出要简短清晰，方便后续进入声音生成。
                """);
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
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("stage", "image");
        result.put("roleImageResultJson", org.jeecg.common.util.oConvertUtils.getString(context == null ? null : context.getAttribute("roleImageResultJson")));
        return result;
    }
}
