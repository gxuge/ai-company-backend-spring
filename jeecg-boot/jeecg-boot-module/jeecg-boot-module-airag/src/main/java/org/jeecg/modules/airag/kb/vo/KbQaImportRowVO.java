package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * QA导入预览行。
 */
@Data
@Schema(description = "QA导入预览行")
public class KbQaImportRowVO {
    /**
     * 行号。
     */
    @JsonProperty("row_no")
    @Schema(description = "行号")
    private Integer rowNo;

    /**
     * 问题。
     */
    @JsonProperty("question")
    @Schema(description = "问题")
    private String question;

    /**
     * 答案。
     */
    @JsonProperty("answer")
    @Schema(description = "答案")
    private String answer;

    /**
     * 标签。
     */
    @JsonProperty("tags")
    @Schema(description = "标签")
    private String tags;

    /**
     * 元数据JSON。
     */
    @JsonProperty("metadata_json")
    @Schema(description = "元数据JSON")
    private String metadataJson;

    /**
     * 排序号。
     */
    @JsonProperty("sort_no")
    @Schema(description = "排序号")
    private Integer sortNo;

    /**
     * 是否有效。
     */
    @JsonProperty("valid")
    @Schema(description = "是否有效")
    private Boolean valid;

    /**
     * 错误信息。
     */
    @JsonProperty("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;
}
