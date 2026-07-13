package org.jeecg.modules.airag.agent.trace;

import lombok.Data;
import org.jeecg.modules.airag.agent.runtime.AgentContext;

import java.util.Map;

/**
 * Agent LLM response trace payload.
 */
@Data
public class AgentLlmTraceResponse {
    private AgentContext context;
    private String nodeName;
    private String promptCode;
    private String promptVersion;
    private String modelId;
    private String responseRaw;
    private String finishReason;
    private boolean success;
    private String errorMessage;
    private Map<String, Object> extraInfo;
}
