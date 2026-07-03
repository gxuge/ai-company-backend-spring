package org.jeecg.modules.airag.agent.chat;

import org.jeecg.common.util.oConvertUtils;
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
 * 意图路由节点。
 *
 * @author codex
 * @date 2026/6/25
 */
@Component
public class IntentRouterNode extends LlmNode {
    /**
     * 子 Agent 注册中心。
     */
    private final SubAgentRegistry subAgentRegistry;

    /**
     * 构造函数。
     *
     * @param promptTemplateService 模板服务
     * @param modelResolver 模型解析器
     * @param aiChatHandler 大模型处理器
     * @param eventPublisher 事件发布器
     * @param subAgentRegistry 子 Agent 注册中心
     */
    public IntentRouterNode(IAiragPromptTemplateService promptTemplateService,
                            AgentModelResolver modelResolver,
                            IAIChatHandler aiChatHandler,
                            AgentEventPublisher eventPublisher,
                            SubAgentRegistry subAgentRegistry) {
        super(
                "intent_router_dual_mode",
                "Agent意图路由",
                "intent_router_dual_mode",
                "v1",
                null,
                null,
                null,
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
        this.subAgentRegistry = subAgentRegistry;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("user_input", oConvertUtils.getString(context.getUserInput()));
        variables.put("session_title", oConvertUtils.getString(context.getAttribute("sessionTitle")));
        variables.put("session_summary", oConvertUtils.getString(context.getAttribute("sessionSummary")));
        variables.put("memory_json", oConvertUtils.getString(context.getAttribute("sessionMemoryJson")));
        variables.put("recent_messages_block", oConvertUtils.getString(context.getAttribute("recentMessagesBlock")));
        variables.put("available_sub_agents", this.subAgentRegistry.describeAvailableSubAgents());
        return variables;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        Map<String, Object> json = parseJsonObject(finalText);
        AgentRouteDecision decision = AgentRouteDecision.fromMap(json);
        if (decision.getAction() == null) {
            decision.setAction(AgentRouteAction.CALL_DEFAULT);
        }
        if (decision.getAction() == AgentRouteAction.CALL_CHAT_AGENT
                && oConvertUtils.isEmpty(decision.getTargetAgent())) {
            decision.setTargetAgent(this.subAgentRegistry.defaultSubAgentName());
        }
        if (oConvertUtils.isEmpty(decision.getSubAgentName()) && oConvertUtils.isNotEmpty(decision.getTargetAgent())) {
            decision.setSubAgentName(decision.getTargetAgent());
        }
        if (decision.getIntentMode() == null) {
            decision.setIntentMode(decision.getAction() == AgentRouteAction.CALL_TASK_AGENT
                    ? AgentIntentMode.TASK_MODE
                    : AgentIntentMode.CHAT_MODE);
        }
        if (decision.getAction() == AgentRouteAction.CALL_DEFAULT) {
            decision.setIntentMode(AgentIntentMode.CHAT_MODE);
        }
        if (decision.getAction() == AgentRouteAction.CALL_CHAT_AGENT
                && oConvertUtils.isEmpty(decision.getTargetAgent())) {
            decision.setTargetAgent(this.subAgentRegistry.defaultSubAgentName());
        }
        NodeResult result = NodeResult.success(oConvertUtils.isEmpty(decision.getReply()) ? finalText : decision.getReply());
        result.setAction(decision.getAction().name());
        result.put("routeDecision", decision.toMap());
        result.put("intentMode", decision.getIntentMode() == null ? null : decision.getIntentMode().name());
        result.put("targetAgent", decision.getTargetAgent());
        result.put("taskGoal", decision.getTaskGoal());
        result.put("promptCode", getPromptCode());
        result.put("promptVersion", getPromptVersion());
        result.put("rawText", finalText);
        return result;
    }

    @Override
    protected boolean shouldPublishPartialResponse() {
        return false;
    }
}
