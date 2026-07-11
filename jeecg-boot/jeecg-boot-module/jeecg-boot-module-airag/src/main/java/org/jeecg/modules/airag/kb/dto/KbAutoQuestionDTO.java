package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 自动生成索引问题请求。
 */
@Data
@Schema(description = "自动生成索引问题请求")
public class KbAutoQuestionDTO {
    /**
     * chunk内容，chunkId为空时使用。
     */
    @JsonProperty("content")
    @Schema(description = "chunk内容")
    private String content;

    /**
     * 生成数量。
     */
    @Min(value = 1, message = "question_count必须大于0")
    @JsonProperty("question_count")
    @Schema(description = "生成数量")
    private Integer questionCount;

    /**
     * 是否覆盖已有自动问题。
     */
    @JsonProperty("overwrite_auto_question")
    @Schema(description = "是否覆盖已有自动问题")
    private Boolean overwriteAutoQuestion;
}
