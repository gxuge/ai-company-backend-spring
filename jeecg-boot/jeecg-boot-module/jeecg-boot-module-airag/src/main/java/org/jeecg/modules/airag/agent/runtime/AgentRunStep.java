package org.jeecg.modules.airag.agent.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 顶层 Run 中的一次 Agent 执行记录。
 *
 * @author codex
 * @date 2026/7/14
 */
@Data
@AllArgsConstructor
public class AgentRunStep {
    /**
     * Step 序号。
     */
    private int stepIndex;
    /**
     * 本 Step 执行的 Agent 编码。
     */
    private String agentCode;
    /**
     * Agent 执行结果。
     */
    private AgentResult result;
}
