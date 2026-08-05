package org.jeecg.modules.airag.agent.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.graph.Agent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 顶层 Agent 运行循环。
 *
 * <p>Handoff 不创建嵌套 Run，而是在同一个 AgentContext 中切换 active Agent。</p>
 *
 * @author codex
 * @date 2026/7/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRunLoopService {
    /**
     * 单次顶层 Run 允许的最大 Agent Step 数。
     */
    public static final int MAX_AGENT_STEPS = 8;

    private final AgentRegistry agentRegistry;
    private final AgentContextPreparer contextPreparer;
    private final AgentRuntimeService agentRuntimeService;

    /**
     * 从指定 Agent 启动顶层运行循环。
     *
     * @param startingAgentCode 起始 Agent 编码
     * @param context 运行上下文
     * @return 顶层 Run 结果
     */
    public AgentRunOutcome run(String startingAgentCode, AgentContext context) {
        AgentContext actualContext = context == null ? new AgentContext() : context;
        actualContext.normalize();
        String currentAgentCode = this.agentRegistry.normalizeStartingAgentCode(startingAgentCode);
        List<AgentRunStep> steps = new ArrayList<>();

        for (int stepIndex = 1; stepIndex <= MAX_AGENT_STEPS; stepIndex++) {
            AgentRunControlService.throwIfStopRequested(actualContext);
            Optional<Agent> agentOptional = this.agentRegistry.find(currentAgentCode);
            if (agentOptional.isEmpty()) {
                AgentResult failed = AgentResult.failed(
                        AgentErrorCode.RUNTIME_AGENT_NOT_FOUND,
                        Map.of("agentCode", currentAgentCode)
                );
                failed.getData().put("missingAgentCode", currentAgentCode);
                return new AgentRunOutcome(failed, currentAgentCode, steps);
            }

            this.contextPreparer.prepare(actualContext, currentAgentCode);
            actualContext.putAttribute("agentStepIndex", stepIndex);
            AgentResult stepResult = this.agentRuntimeService.execute(agentOptional.get(), actualContext);
            steps.add(new AgentRunStep(stepIndex, currentAgentCode, stepResult));
            if (stepResult != null && stepResult.getStatus() == AgentResult.Status.INTERRUPTED) {
                return new AgentRunOutcome(stepResult, currentAgentCode, steps);
            }
            AgentRunControlService.throwIfStopRequested(actualContext);

            if (stepResult == null || AgentResult.Status.HANDOFF != stepResult.getStatus()) {
                return new AgentRunOutcome(stepResult, currentAgentCode, steps);
            }

            String targetAgentCode = this.agentRegistry.normalizeCode(stepResult.getHandoffTargetAgentCode());
            if (!StringUtils.hasText(targetAgentCode) || !this.agentRegistry.exists(targetAgentCode)) {
                AgentResult failed = AgentResult.failed(
                        AgentErrorCode.RUNTIME_HANDOFF_TARGET_NOT_FOUND,
                        Map.of(
                                "sourceAgentCode", currentAgentCode,
                                "targetAgentCode", targetAgentCode == null ? "" : targetAgentCode
                        )
                );
                failed.getData().put("sourceAgentCode", currentAgentCode);
                failed.getData().put("targetAgentCode", targetAgentCode);
                return new AgentRunOutcome(failed, currentAgentCode, steps);
            }

            appendHandoffEvent(actualContext, stepIndex, currentAgentCode, targetAgentCode, stepResult);
            this.contextPreparer.applyHandoff(actualContext, stepResult);
            currentAgentCode = this.agentRegistry.isMainAgentCode(targetAgentCode)
                    ? AgentRegistry.MAIN_AGENT_CODE
                    : targetAgentCode;
            if (AgentHandoffSupport.shouldEndRunAfterHandoff(stepResult)) {
                return new AgentRunOutcome(stepResult, currentAgentCode, steps);
            }
        }

        AgentResult failed = AgentResult.failed(
                AgentErrorCode.RUNTIME_HANDOFF_LIMIT_EXCEEDED,
                Map.of("maxAgentSteps", MAX_AGENT_STEPS)
        );
        String lastExecutedAgentCode = steps.isEmpty()
                ? currentAgentCode
                : steps.get(steps.size() - 1).getAgentCode();
        failed.getData().put("maxAgentSteps", MAX_AGENT_STEPS);
        failed.getData().put("lastAgentCode", lastExecutedAgentCode);
        log.warn("Agent Handoff次数超过限制，runId={}，lastAgentCode={}",
                actualContext.getRunId(), lastExecutedAgentCode);
        return new AgentRunOutcome(failed, lastExecutedAgentCode, steps);
    }

    private void appendHandoffEvent(AgentContext context,
                                    int stepIndex,
                                    String sourceAgentCode,
                                    String targetAgentCode,
                                    AgentResult result) {
        if (context == null) {
            return;
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", "agent.handoff");
        event.put("stepIndex", stepIndex);
        event.put("sourceAgentCode", sourceAgentCode);
        event.put("targetAgentCode", targetAgentCode);
        event.put("handoffInput", result == null ? null : result.getHandoffInput());
        event.put("handoffData", result == null ? null : result.getData());
        context.appendEvent(event);
    }
}
