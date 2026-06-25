package org.jeecg.modules.airag.agent.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        this.eventPublisher.publishAgentStart(context, agent.agentName());
        AgentResult result;
        try {
            result = agent.execute(context);
            if (result == null) {
                result = AgentResult.failed("Agent未返回结果");
            }
        } catch (Exception ex) {
            log.error("Agent执行失败，agentName={}", agent.agentName(), ex);
            result = AgentResult.failed(ex.getMessage());
        }
        this.eventPublisher.publishAgentEnd(context, agent.agentName(), result);
        return result;
    }
}
