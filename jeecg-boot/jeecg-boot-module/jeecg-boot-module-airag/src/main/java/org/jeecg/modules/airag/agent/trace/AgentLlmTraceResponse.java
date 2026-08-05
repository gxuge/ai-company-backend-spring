package org.jeecg.modules.airag.agent.trace;

import lombok.Data;
import org.jeecg.modules.airag.agent.runtime.AgentContext;

import java.util.Map;
import java.util.Date;

/**
 * Agent LLM response trace payload.
 */
@Data
public class AgentLlmTraceResponse {
    private AgentContext context;
    private String invocationId;
    private Date finishedAt;
    private Long durationMs;
    private String nodeName;
    private String promptCode;
    private String promptVersion;
    private String modelId;
    private String responseRaw;
    private String finishReason;
    private String actualModelName;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private boolean success;
    private String errorMessage;
    private Map<String, Object> extraInfo;
}
