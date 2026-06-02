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
@TableName("ts_ai_log")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "ts AI调用监控主表")
public class TsAiLog {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("trace_id")
    @Schema(description = "调用链追踪ID")
    private String traceId;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @TableField("biz_type")
    @Schema(description = "业务类型")
    private String bizType;

    @TableField("biz_scene")
    @Schema(description = "业务场景")
    private String bizScene;

    @Schema(description = "接口路径")
    private String endpoint;

    @TableField("http_method")
    @Schema(description = "HTTP方法")
    private String httpMethod;

    @TableField("controller_method")
    @Schema(description = "控制器方法")
    private String controllerMethod;

    @TableField("request_params")
    @Schema(description = "请求参数摘要")
    private String requestParams;

    @Schema(description = "模型供应商")
    private String provider;

    @TableField("model_name")
    @Schema(description = "模型名称")
    private String modelName;

    @TableField("model_id")
    @Schema(description = "模型ID")
    private String modelId;

    @TableField("prompt_code")
    @Schema(description = "首轮提示词模板编码")
    private String promptCode;

    @TableField("prompt_version")
    @Schema(description = "首轮提示词模板版本")
    private String promptVersion;

    @TableField("repair_prompt_code")
    @Schema(description = "修复提示词模板编码")
    private String repairPromptCode;

    @TableField("repair_prompt_version")
    @Schema(description = "修复提示词模板版本")
    private String repairPromptVersion;

    @TableField("has_repair")
    @Schema(description = "是否触发JSON修复")
    private Integer hasRepair;

    @Schema(description = "执行状态")
    private String status;

    @TableField("cost_ms")
    @Schema(description = "总耗时ms")
    private Long costMs;

    @TableField("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;

    @TableField("final_result_json")
    @Schema(description = "最终结果JSON")
    private String finalResultJson;

    @TableField("create_time")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @TableField("update_time")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
}
