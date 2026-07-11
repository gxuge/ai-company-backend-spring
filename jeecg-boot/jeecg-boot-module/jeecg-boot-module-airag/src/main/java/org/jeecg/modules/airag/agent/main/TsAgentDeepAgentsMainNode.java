package org.jeecg.modules.airag.agent.main;

import com.alibaba.fastjson2.JSON;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DeepAgents 主代理节点。
 *
 * <p>负责注入技能索引、子 Agent 列表和 task 工具，
 * 由模型自行决定是否委托任务。</p>
 */
@Component
public class TsAgentDeepAgentsMainNode extends LlmNode {

    public TsAgentDeepAgentsMainNode(IAiragPromptTemplateService promptTemplateService,
                                     AgentModelResolver modelResolver,
                                     IAIChatHandler aiChatHandler,
                                     AgentEventPublisher eventPublisher) {
        super(
                "ts_agent_deep_agents_main",
                "DeepAgents 主代理",
                buildDefinition(),
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
    }

    private static LlmNodeDefinition buildDefinition() {
        LlmNodeDefinition definition = new LlmNodeDefinition();
        definition.setName("DeepAgents 主代理");
        definition.setDescription("负责理解用户目标、读取技能索引、必要时通过 task 委托子 Agent，并输出最终回复。");
        definition.setSkillDomain("chat");
        definition.setSkillTopK(5);
        definition.setSkills(java.util.Collections.emptyList());
        definition.setTools(java.util.List.of("task"));
        definition.setPermissions(java.util.List.of("task"));
        definition.setResponseFormat("text");
        definition.setUserPromptTemplate("""
                当前用户输入：
                {{user_input}}

                会话摘要：
                {{session_summary}}

                最近对话：
                {{recent_messages_block}}

                已确认信息：
                {{confirmed_fields_json}}

                缺失信息：
                {{missing_fields_json}}
                """);
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = new LinkedHashMap<>();
        Map<?, ?> promptVariables = context == null ? null : context.getAttribute("promptVariables", Map.class);
        if (promptVariables != null) {
            for (Map.Entry<?, ?> entry : promptVariables.entrySet()) {
                if (entry.getKey() != null) {
                    variables.put(String.valueOf(entry.getKey()), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                }
            }
        }
        variables.putIfAbsent("user_input", oConvertUtils.getString(context == null ? null : context.getUserInput()));
        variables.putIfAbsent("session_summary", oConvertUtils.getString(context == null ? null : context.getAttribute("sessionSummary")));
        variables.putIfAbsent("recent_messages_block", oConvertUtils.getString(context == null ? null : context.getAttribute("recentMessagesBlock")));
        variables.putIfAbsent("confirmed_fields_json", JSON.toJSONString(context == null ? null : context.getAttribute("confirmedFieldsJson")));
        variables.putIfAbsent("missing_fields_json", JSON.toJSONString(context == null ? null : context.getAttribute("missingFieldsJson")));
        return variables;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("promptCode", getPromptCode());
        result.put("promptVersion", getPromptVersion());
        return result;
    }
}
