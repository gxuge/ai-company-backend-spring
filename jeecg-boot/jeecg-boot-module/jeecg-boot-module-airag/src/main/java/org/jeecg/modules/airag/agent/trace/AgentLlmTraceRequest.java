package org.jeecg.modules.airag.agent.trace;

import lombok.Data;
import org.jeecg.modules.airag.agent.runtime.AgentContext;

import java.util.Map;
import java.util.Date;

/**
 * Agent LLM request trace payload.
 */
@Data
public class AgentLlmTraceRequest {
    private AgentContext context;
    private String invocationId;
    private Date startedAt;
    private String nodeName;
    private String promptCode;
    private String promptVersion;
    private String modelId;
    private String developerPrompt;
    private String userPrompt;
    private String renderedPrompt;
    private String toolSchema;
    private Map<String, Object> requestPayload;
}
