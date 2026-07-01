package org.jeecg.modules.airag.agent.chat;

import lombok.RequiredArgsConstructor;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.Agent;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 会话总入口。
 *
 * @author codex
 * @date 2026/6/25
 */
@Component
@RequiredArgsConstructor
public class TsAgentChatAgent implements Agent {
    /**
     * 节点执行器。
     */
    private final NodeRunner nodeRunner;
    /**
     * 意图路由节点。
     */
    private final IntentRouterNode intentRouterNode;
    /**
     * 子 Agent 注册中心。
     */
    private final SubAgentRegistry subAgentRegistry;
    /**
     * 开场白子 Agent。
     */
    private final WelcomeIntroSubAgent welcomeIntroSubAgent;
    /**
     * 默认聊天子 Agent。
     */
    private final GeneralChatSubAgent generalChatSubAgent;

    @Override
    public String agentName() {
        return "ts_agent_chat";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String forceSubAgentName = oConvertUtils.getString(context.getAttribute("forceSubAgentName"));
        if (welcomeIntroSubAgent.subAgentName().equalsIgnoreCase(forceSubAgentName)) {
            AgentResult result = this.welcomeIntroSubAgent.execute(context);
            if (result == null) {
                return AgentResult.failed("开场白子Agent未返回结果");
            }
            result.getData().put("intentMode", AgentIntentMode.CHAT_MODE.name());
            result.getData().put("targetAgent", this.welcomeIntroSubAgent.subAgentName());
            result.getData().put("resolvedAgent", this.welcomeIntroSubAgent.subAgentName());
            result.getData().put("targetSubAgent", this.welcomeIntroSubAgent.subAgentName());
            Map<String, Object> routeDecision = new LinkedHashMap<>();
            routeDecision.put("intentMode", AgentIntentMode.CHAT_MODE.name());
            routeDecision.put("action", AgentRouteAction.CALL_CHAT_AGENT.name());
            routeDecision.put("targetAgent", this.welcomeIntroSubAgent.subAgentName());
            routeDecision.put("subAgentName", this.welcomeIntroSubAgent.subAgentName());
            routeDecision.put("taskGoal", null);
            routeDecision.put("reason", "新会话首轮自动开场");
            routeDecision.put("confidence", 1.0);
            routeDecision.put("question", null);
            result.getData().put("routeDecision", routeDecision);
            return result;
        }

        NodeResult routeNodeResult = this.nodeRunner.run(context, this.intentRouterNode);
        AgentRouteDecision decision = extractDecision(routeNodeResult);
        context.putAttribute("routeDecision", decision.toMap());
        context.putAttribute("intentMode", decision.getIntentMode() == null ? null : decision.getIntentMode().name());
        context.putAttribute("targetAgent", decision.getTargetAgent());
        context.putAttribute("taskGoal", decision.getTaskGoal());
        context.putAttribute("question", decision.getQuestion());

        if (decision.getAction() == AgentRouteAction.ASK_USER) {
            AgentResult waiting = AgentResult.waitingUser(resolveReply(decision, routeNodeResult));
            waiting.getData().put("intentMode", decision.getIntentMode() == null ? null : decision.getIntentMode().name());
            waiting.getData().put("routeDecision", decision.toMap());
            waiting.getData().put("targetAgent", decision.getTargetAgent());
            waiting.getData().put("taskGoal", decision.getTaskGoal());
            waiting.getData().put("question", decision.getQuestion());
            waiting.getData().put("targetSubAgent", decision.getTargetAgent());
            waiting.getData().put("routeNode", routeNodeResult.getData());
            waiting.getData().put("promptCode", routeNodeResult.getData() == null ? null : routeNodeResult.getData().get("promptCode"));
            waiting.getData().put("promptVersion", routeNodeResult.getData() == null ? null : routeNodeResult.getData().get("promptVersion"));
            return waiting;
        }

        String targetAgent = decision.getTargetAgent();
        if (oConvertUtils.isEmpty(targetAgent)) {
            targetAgent = decision.getSubAgentName();
        }
        String resolvedAgent = targetAgent;
        if (oConvertUtils.isEmpty(resolvedAgent) || !this.subAgentRegistry.exists(resolvedAgent)) {
            resolvedAgent = this.subAgentRegistry.defaultSubAgentName();
        }

        AgentResult result;
        if (this.subAgentRegistry.exists(resolvedAgent)) {
            result = this.subAgentRegistry.find(resolvedAgent)
                    .map(subAgent -> subAgent.execute(context))
                    .orElseGet(() -> this.generalChatSubAgent.execute(context));
        } else {
            result = this.generalChatSubAgent.execute(context);
            resolvedAgent = this.generalChatSubAgent.subAgentName();
        }

        if (result == null) {
            result = AgentResult.failed("子Agent未返回结果");
        }
        result.getData().put("intentMode", decision.getIntentMode() == null ? null : decision.getIntentMode().name());
        result.getData().put("routeDecision", decision.toMap());
        result.getData().put("targetAgent", targetAgent);
        result.getData().put("resolvedAgent", resolvedAgent);
        result.getData().put("targetSubAgent", resolvedAgent);
        result.getData().put("taskGoal", decision.getTaskGoal());
        result.getData().put("question", decision.getQuestion());
        result.getData().put("routeNode", routeNodeResult.getData());
        if (!result.getData().containsKey("promptCode") && routeNodeResult.getData() != null) {
            result.getData().put("promptCode", routeNodeResult.getData().get("promptCode"));
        }
        if (!result.getData().containsKey("promptVersion") && routeNodeResult.getData() != null) {
            result.getData().put("promptVersion", routeNodeResult.getData().get("promptVersion"));
        }
        return result;
    }

    /**
     * 提取路由结果。
     *
     * @param routeNodeResult 节点结果
     * @return 路由决策
     */
    @SuppressWarnings("unchecked")
    private AgentRouteDecision extractDecision(NodeResult routeNodeResult) {
        if (routeNodeResult == null || routeNodeResult.getData() == null) {
            AgentRouteDecision decision = new AgentRouteDecision();
            decision.setTargetAgent(this.subAgentRegistry.defaultSubAgentName());
            decision.setSubAgentName(this.subAgentRegistry.defaultSubAgentName());
            decision.setAction(AgentRouteAction.CALL_DEFAULT);
            return decision;
        }
        Object decisionValue = routeNodeResult.getData().get("routeDecision");
        if (decisionValue instanceof Map<?, ?> map) {
            Map<String, Object> decisionMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    decisionMap.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            AgentRouteDecision decision = AgentRouteDecision.fromMap(decisionMap);
            if (oConvertUtils.isEmpty(decision.getTargetAgent()) && decision.getAction() != AgentRouteAction.CALL_TASK_AGENT) {
                decision.setTargetAgent(this.subAgentRegistry.defaultSubAgentName());
            }
            if (oConvertUtils.isEmpty(decision.getSubAgentName()) && oConvertUtils.isNotEmpty(decision.getTargetAgent())) {
                decision.setSubAgentName(decision.getTargetAgent());
            }
            return decision;
        }
        AgentRouteDecision decision = new AgentRouteDecision();
        decision.setTargetAgent(this.subAgentRegistry.defaultSubAgentName());
        decision.setSubAgentName(this.subAgentRegistry.defaultSubAgentName());
        decision.setAction(AgentRouteAction.CALL_DEFAULT);
        return decision;
    }

    /**
     * 选择追问文本。
     *
     * @param decision 路由决策
     * @param routeNodeResult 节点结果
     * @return 文本
     */
    private String resolveReply(AgentRouteDecision decision, NodeResult routeNodeResult) {
        if (decision != null && oConvertUtils.isNotEmpty(decision.getQuestion())) {
            return decision.getQuestion();
        }
        if (decision != null && oConvertUtils.isNotEmpty(decision.getReply())) {
            return decision.getReply();
        }
        if (routeNodeResult != null && oConvertUtils.isNotEmpty(routeNodeResult.getContent())) {
            return routeNodeResult.getContent();
        }
        return "我先确认一下你的意思，再继续往下走。";
    }
}
