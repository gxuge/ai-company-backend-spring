package org.jeecg.modules.airag.agent.runtime;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 顶层 Agent Run 结果。
 *
 * @author codex
 * @date 2026/7/14
 */
@Getter
public class AgentRunOutcome {
    /**
     * 最终业务结果。
     */
    private final AgentResult result;
    /**
     * 最后实际运行的 Agent 编码。
     */
    private final String lastAgentCode;
    /**
     * 本 Run 的 Agent Step。
     */
    private final List<AgentRunStep> steps;

    public AgentRunOutcome(AgentResult result, String lastAgentCode, List<AgentRunStep> steps) {
        this.result = result;
        this.lastAgentCode = lastAgentCode;
        this.steps = steps == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(steps));
    }
}
