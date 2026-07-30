package org.jeecg.modules.airag.agent.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorSupport;
import org.jeecg.modules.airag.agent.graph.Agent;
import org.springframework.stereotype.Service;

/**
 * Agent 执行入口服务。
 *
 * @author codex
 * @date 2026/6/16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRuntimeService {
    /**
     * 子 Agent 发送方类型。
     */
    private static final String SENDER_SUB_AGENT = "sub_agent";

    /**
     * 事件发布器。
     */
    private final AgentEventPublisher eventPublisher;

    /**
     * 启动一个 Agent 执行。
     *
     * @param agent 目标 Agent
     * @param context 运行上下文
     * @return 执行结果
     */
    public AgentResult execute(Agent agent, AgentContext context) {
        context.normalize();
        boolean subAgent = isSubAgentContext(context);
        if (subAgent) {
            this.eventPublisher.publishSubAgentStart(context, agent.agentName(), null);
        } else {
            this.eventPublisher.publishAgentStart(context, agent.agentName());
        }
        AgentResult result;
        try {
            result = agent.execute(context);
            if (result == null) {
                result = AgentResult.failed(AgentErrorCode.RUNTIME_AGENT_EMPTY_RESULT, null);
            }
        } catch (Exception ex) {
            log.error("Agent执行失败，agentName={}", agent.agentName(), ex);
            AgentErrorCode fallback = subAgent
                    ? AgentErrorCode.RUNTIME_SUBAGENT_EXECUTION_FAILED
                    : AgentErrorCode.RUNTIME_AGENT_EXECUTION_FAILED;
            result = AgentResult.failed(fallback, null);
            AgentErrorSupport.attach(result, ex, fallback);
        }
        if (subAgent) {
            this.eventPublisher.publishSubAgentEnd(context, agent.agentName(), result, null);
        } else {
            this.eventPublisher.publishAgentEnd(context, agent.agentName(), result);
        }
        return result;
    }

    private boolean isSubAgentContext(AgentContext context) {
        return context != null && SENDER_SUB_AGENT.equalsIgnoreCase(context.getSenderType());
    }
}
