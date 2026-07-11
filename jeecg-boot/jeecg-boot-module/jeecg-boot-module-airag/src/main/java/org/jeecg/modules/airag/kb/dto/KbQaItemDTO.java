package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * QA条目请求。
 */
@Data
@Schema(description = "QA条目请求")
public class KbQaItemDTO {
    /**
     * 行号。
     */
    @JsonProperty("row_no")
    @Schema(description = "行号")
    private Integer rowNo;

    /**
     * 问题。
     */
    @NotBlank(message = "question不能为空")
    @Size(max = 4000, message = "question不能超过4000个字符")
    @JsonProperty("question")
    @Schema(description = "问题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    /**
     * 答案。
     */
    @NotBlank(message = "answer不能为空")
    @JsonProperty("answer")
    @Schema(description = "答案", requiredMode = Schema.RequiredMode.REQUIRED)
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
    @Min(value = 1, message = "sort_no必须大于0")
    @JsonProperty("sort_no")
    @Schema(description = "排序号")
    private Integer sortNo;
}
