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
 * 知识库检索测试日志。
 */
@Data
@Schema(description = "知识库检索测试日志")
@TableName("kb_retrieval_test_log")
public class KbRetrievalTestLog implements Serializable {
    /**
     * 主键ID。
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /**
     * 知识库ID。
     */
    @Schema(description = "知识库ID")
    @TableField("kb_id")
    private String kbId;

    /**
     * 原始query。
     */
    @Schema(description = "原始query")
    private String query;

    /**
     * 优化query JSON。
     */
    @Schema(description = "优化query JSON")
    @TableField("optimized_queries_json")
    private String optimizedQueriesJson;

    /**
     * 实际使用query JSON。
     */
    @Schema(description = "实际使用query JSON")
    @TableField("used_queries_json")
    private String usedQueriesJson;

    /**
     * 检索模式。
     */
    @Schema(description = "检索模式")
    @TableField("search_mode")
    private String searchMode;

    /**
     * 请求与实际参数JSON。
     */
    @Schema(description = "请求与实际参数JSON")
    @TableField("params_json")
    private String paramsJson;

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
     * 状态：success/failed。
     */
    @Schema(description = "状态：success/failed")
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
