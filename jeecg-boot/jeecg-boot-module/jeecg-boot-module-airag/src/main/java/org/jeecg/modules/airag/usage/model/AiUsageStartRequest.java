package org.jeecg.modules.airag.usage.model;

import lombok.Data;

import java.util.Date;

/**
 * Starts one internal AI usage record.
 */
@Data
public class AiUsageStartRequest {

    private String invocationId;
    private String traceId;
    private String parentInvocationId;
    private String userId;
    private Integer tenantId;
    private String sourceType;
    private String sceneCode;
    private String modality;
    private String operationType;
    private String provider;
    private String modelId;
    private String modelName;
    private Long sessionId;
    private Long messageId;
    private String runId;
    private String agentName;
    private String nodeName;
    private String toolName;
    private Date startedAt;
    private String extJson;
}
