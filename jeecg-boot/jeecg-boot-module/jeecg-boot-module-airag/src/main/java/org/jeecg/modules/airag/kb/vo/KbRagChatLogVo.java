package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.modules.airag.kb.entity.KbRagChatLog;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * RAG 问答日志返回对象。
 */
@Data
@Schema(description = "RAG 问答日志返回对象")
public class KbRagChatLogVo {
    /**
     * 主键ID。
     */
    @Schema(description = "主键ID")
    private String id;

    /**
     * 原始问题。
     */
    @Schema(description = "原始问题")
    private String query;

    /**
     * 回答。
     */
    @Schema(description = "回答")
    private String answer;

    /**
     * 内部知识库ID JSON。
     */
    @Schema(description = "内部知识库ID JSON")
    @JsonProperty("kb_ids_json")
    private String kbIdsJson;

    /**
     * 外部知识库ID JSON。
     */
    @Schema(description = "外部知识库ID JSON")
    @JsonProperty("external_kb_ids_json")
    private String externalKbIdsJson;

    /**
     * 回答模式。
     */
    @Schema(description = "回答模式")
    @JsonProperty("answer_mode")
    private String answerMode;

    /**
     * 实际参数 JSON。
     */
    @Schema(description = "实际参数 JSON")
    @JsonProperty("actual_params_json")
    private String actualParamsJson;

    /**
     * 使用 query JSON。
     */
    @Schema(description = "使用 query JSON")
    @JsonProperty("used_queries_json")
    private String usedQueriesJson;

    /**
     * 使用上下文 JSON。
     */
    @Schema(description = "使用上下文 JSON")
    @JsonProperty("used_context_json")
    private String usedContextJson;

    /**
     * 引用 JSON。
     */
    @Schema(description = "引用 JSON")
    @JsonProperty("citations_json")
    private String citationsJson;

    /**
     * 返回条数。
     */
    @Schema(description = "返回条数")
    @JsonProperty("result_count")
    private Integer resultCount;

    /**
     * 使用引用长度。
     */
    @Schema(description = "使用引用长度")
    @JsonProperty("used_reference_length")
    private Integer usedReferenceLength;

    /**
     * 调试 JSON。
     */
    @Schema(description = "调试 JSON")
    @JsonProperty("debug_json")
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
    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("created_at")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("updated_at")
    private Date updatedAt;

    /**
     * 由实体转换。
     *
     * @param entity 实体
     * @return VO
     */
    public static KbRagChatLogVo from(KbRagChatLog entity) {
        if (entity == null) {
            return null;
        }
        KbRagChatLogVo vo = new KbRagChatLogVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
