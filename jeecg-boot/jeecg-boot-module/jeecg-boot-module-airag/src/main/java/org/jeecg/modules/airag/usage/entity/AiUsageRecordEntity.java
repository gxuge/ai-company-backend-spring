package org.jeecg.modules.airag.usage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI model and multimodal capability usage record.
 */
@Data
@TableName("ts_ai_usage_record")
public class AiUsageRecordEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("invocation_id")
    private String invocationId;
    @TableField("trace_id")
    private String traceId;
    @TableField("parent_invocation_id")
    private String parentInvocationId;
    @TableField("user_id")
    private String userId;
    @TableField("tenant_id")
    private Integer tenantId;
    @TableField("source_type")
    private String sourceType;
    @TableField("scene_code")
    private String sceneCode;
    private String modality;
    @TableField("operation_type")
    private String operationType;
    private String provider;
    @TableField("model_id")
    private String modelId;
    @TableField("model_name")
    private String modelName;
    @TableField("session_id")
    private Long sessionId;
    @TableField("message_id")
    private Long messageId;
    @TableField("run_id")
    private String runId;
    @TableField("agent_name")
    private String agentName;
    @TableField("node_name")
    private String nodeName;
    @TableField("tool_name")
    private String toolName;
    private String status;
    @TableField("started_at")
    private Date startedAt;
    @TableField("finished_at")
    private Date finishedAt;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("error_code")
    private String errorCode;
    @TableField("error_message")
    private String errorMessage;
    @TableField("usage_raw_json")
    private String usageRawJson;
    @TableField("ext_json")
    private String extJson;
    @TableField("is_deleted")
    private Integer isDeleted;
    @TableField("created_at")
    private Date createdAt;
    @TableField("updated_at")
    private Date updatedAt;
}
