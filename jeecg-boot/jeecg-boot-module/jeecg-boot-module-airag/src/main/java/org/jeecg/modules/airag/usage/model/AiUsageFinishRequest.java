package org.jeecg.modules.airag.usage.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Completes or fails one internal AI usage record.
 */
@Data
public class AiUsageFinishRequest {

    private String invocationId;
    private String status;
    private String provider;
    private String modelId;
    private String modelName;
    private Date finishedAt;
    private Long durationMs;
    private String errorCode;
    private String errorMessage;
    private String usageRawJson;
    private String extJson;
    private List<AiUsageMetricValue> metrics = new ArrayList<>();
}
