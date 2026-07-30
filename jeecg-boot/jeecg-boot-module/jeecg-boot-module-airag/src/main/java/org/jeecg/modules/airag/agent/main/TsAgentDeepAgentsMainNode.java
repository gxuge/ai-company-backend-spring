package org.jeecg.modules.airag.agent.main;

import com.alibaba.fastjson2.JSON;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.DeepAgentsProperties;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.agent.skill.model.SkillLoadResult;
import org.jeecg.modules.airag.agent.tool.DeepAgentTaskToolService;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.jeecg.modules.airag.common.handler.AIChatParams;

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
    /**
     * task 工具按需获取，避免启动期拉起子 agent 依赖链。
     */
    private final ObjectProvider<DeepAgentTaskToolService> deepAgentTaskToolServiceProvider;

    public TsAgentDeepAgentsMainNode(IAiragPromptTemplateService promptTemplateService,
                                     AgentModelResolver modelResolver,
                                     IAIChatHandler aiChatHandler,
                                     AgentEventPublisher eventPublisher,
                                     ObjectProvider<DeepAgentTaskToolService> deepAgentTaskToolServiceProvider) {
        super(
                "ts_agent_deep_agents_main",
                "DeepAgents Main Agent",
                buildDefinition(),
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
        this.deepAgentTaskToolServiceProvider = deepAgentTaskToolServiceProvider;
    }

    private static LlmNodeDefinition buildDefinition() {
        LlmNodeDefinition definition = new LlmNodeDefinition();
        definition.setName("DeepAgents Main Agent");
        definition.setDescription("Understands the user's goal, reads the skill index, delegates to a sub-agent through task when needed, and produces the final response.");
        definition.setSkillDomain("chat");
        definition.setSkillTopK(5);
        definition.setSkills(java.util.Collections.emptyList());
        definition.setTools(java.util.List.of("task"));
        definition.setPermissions(java.util.List.of("task"));
        definition.setResponseFormat("text");
        definition.setConversationHistoryEnabled(true);
        definition.setUserPromptTemplate("""
                Current user input:
                {{user_input}}

                Conversation summary:
                {{session_summary}}

                Confirmed information:
                {{confirmed_fields_json}}

                Missing information:
                {{missing_fields_json}}

                Sub-agent handoff report:
                {{handoff_report_json}}
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
        variables.putIfAbsent("confirmed_fields_json", JSON.toJSONString(context == null ? null : context.getAttribute("confirmedFieldsJson")));
        variables.putIfAbsent("missing_fields_json", JSON.toJSONString(context == null ? null : context.getAttribute("missingFieldsJson")));
        variables.putIfAbsent("handoff_report_json", JSON.toJSONString(context == null ? null : context.getAttribute("handoffReport")));
        return variables;
    }

    @Override
    protected AIChatParams buildChatParams(AgentContext context, SkillLoadResult skillLoadResult) {
        AIChatParams params = super.buildChatParams(context, skillLoadResult);
        DeepAgentTaskToolService taskToolService = this.deepAgentTaskToolServiceProvider == null
                ? null
                : this.deepAgentTaskToolServiceProvider.getIfAvailable();
        if (taskToolService != null) {
            Map<dev.langchain4j.agent.tool.ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> taskTools =
                    taskToolService.buildToolMap(context);
            if (taskTools != null && !taskTools.isEmpty()) {
                if (params.getTools() == null) {
                    params.setTools(new LinkedHashMap<>());
                }
                params.getTools().putAll(taskTools);
            }
        }
        return params;
    }

    @Override
    protected boolean shouldPublishPartialResponse() {
        return false;
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
