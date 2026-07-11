package org.jeecg.modules.airag.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 多知识库检索日志。
 */
@Data
@Schema(description = "多知识库检索日志")
@TableName("kb_federated_retrieval_log")
public class KbFederatedRetrievalLog implements Serializable {
    /**
     * 主键ID。
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /**
     * 查询内容。
     */
    @Schema(description = "查询内容")
    private String query;

    /**
     * 内部知识库ID JSON。
     */
    @Schema(description = "内部知识库ID JSON")
    @TableField("kb_ids_json")
    private String kbIdsJson;

    /**
     * 外部知识库ID JSON。
     */
    @Schema(description = "外部知识库ID JSON")
    @TableField("external_kb_ids_json")
    private String externalKbIdsJson;

    /**
     * 参数JSON。
     */
    @Schema(description = "参数JSON")
    @TableField("actual_params_json")
    private String actualParamsJson;

    /**
     * 返回条数。
     */
    @Schema(description = "返回条数")
    @TableField("result_count")
    private Integer resultCount;

    /**
     * 结果JSON。
     */
    @Schema(description = "结果JSON")
    @TableField("result_json")
    private String resultJson;

    /**
     * 调试JSON。
     */
    @Schema(description = "调试JSON")
    @TableField("debug_json")
    private String debugJson;

    /**
     * 状态。
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 错误信息。
     */
    @Schema(description = "错误信息")
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("created_at")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("updated_at")
    private Date updatedAt;
}
