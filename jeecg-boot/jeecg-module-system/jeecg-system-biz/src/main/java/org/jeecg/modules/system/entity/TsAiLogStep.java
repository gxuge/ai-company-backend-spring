package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@TableName("ts_ai_log_step")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "ts AI调用监控步骤表")
public class TsAiLogStep {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("log_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主日志ID")
    private Long logId;

    @TableField("trace_id")
    @Schema(description = "调用链追踪ID")
    private String traceId;

    @TableField("step_no")
    @Schema(description = "步骤序号")
    private Integer stepNo;

    @TableField("step_type")
    @Schema(description = "步骤类型")
    private String stepType;

    @TableField("step_name")
    @Schema(description = "步骤名称")
    private String stepName;

    @Schema(description = "步骤状态")
    private String status;

    @TableField("prompt_code")
    @Schema(description = "步骤命中模板编码")
    private String promptCode;

    @TableField("prompt_version")
    @Schema(description = "步骤命中模板版本")
    private String promptVersion;

    @Schema(description = "模型供应商")
    private String provider;

    @TableField("model_name")
    @Schema(description = "模型名称")
    private String modelName;

    @TableField("model_id")
    @Schema(description = "模型ID")
    private String modelId;

    @TableField("developer_prompt")
    @Schema(description = "developer_prompt")
    private String developerPrompt;

    @TableField("user_prompt")
    @Schema(description = "user_prompt")
    private String userPrompt;

    @TableField("tool_schema")
    @Schema(description = "tool_schema")
    private String toolSchema;

    @TableField("rendered_prompt")
    @Schema(description = "渲染后的完整Prompt")
    private String renderedPrompt;

    @TableField("request_payload_json")
    @Schema(description = "真实发送请求摘要JSON")
    private String requestPayloadJson;

    @TableField("response_raw")
    @Schema(description = "模型原始返回")
    private String responseRaw;

    @TableField("response_json")
    @Schema(description = "解析后的JSON")
    private String responseJson;

    @TableField("validation_issues")
    @Schema(description = "校验或修复问题摘要")
    private String validationIssues;

    @TableField("final_output_json")
    @Schema(description = "步骤最终输出")
    private String finalOutputJson;

    @TableField("extra_info_json")
    @Schema(description = "额外扩展信息")
    private String extraInfoJson;

    @TableField("create_time")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;
}
