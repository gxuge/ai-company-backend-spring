package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 自动问题项。
 */
@Data
@Schema(description = "自动问题项")
public class KbAutoQuestionItemVO {
    /**
     * 索引文本。
     */
    @JsonProperty("index_text")
    @Schema(description = "索引文本")
    private String indexText;

    /**
     * 索引类型。
     */
    @JsonProperty("index_type")
    @Schema(description = "索引类型")
    private String indexType;

    /**
     * 排序号。
     */
    @JsonProperty("sort_no")
    @Schema(description = "排序号")
    private Integer sortNo;
}
