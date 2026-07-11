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
 * RAG 问答日志。
 */
@Data
@Schema(description = "RAG 问答日志")
@TableName("kb_rag_chat_log")
public class KbRagChatLog implements Serializable {
    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 原始问题。
     */
    private String query;

    /**
     * 回答。
     */
    @TableField("answer")
    private String answer;

    /**
     * 内部知识库ID JSON。
     */
    @TableField("kb_ids_json")
    private String kbIdsJson;

    /**
     * 外部知识库ID JSON。
     */
    @TableField("external_kb_ids_json")
    private String externalKbIdsJson;

    /**
     * 回答模式。
     */
    @TableField("answer_mode")
    private String answerMode;

    /**
     * 实际参数 JSON。
     */
    @TableField("actual_params_json")
    private String actualParamsJson;

    /**
     * 使用 query JSON。
     */
    @TableField("used_queries_json")
    private String usedQueriesJson;

    /**
     * 使用上下文 JSON。
     */
    @TableField("used_context_json")
    private String usedContextJson;

    /**
     * 引用 JSON。
     */
    @TableField("citations_json")
    private String citationsJson;

    /**
     * 返回条数。
     */
    @TableField("result_count")
    private Integer resultCount;

    /**
     * 使用引用长度。
     */
    @TableField("used_reference_length")
    private Integer usedReferenceLength;

    /**
     * 调试 JSON。
     */
    @TableField("debug_json")
    private String debugJson;

    /**
     * 状态。
     */
    private String status;

    /**
     * 错误信息。
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间。
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("created_at")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("updated_at")
    private Date updatedAt;
}
