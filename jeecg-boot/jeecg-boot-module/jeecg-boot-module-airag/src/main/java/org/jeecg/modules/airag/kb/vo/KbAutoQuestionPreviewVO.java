package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动索引问题预览结果。
 */
@Data
@Schema(description = "自动索引问题预览结果")
public class KbAutoQuestionPreviewVO {
    /**
     * chunk ID。
     */
    @JsonProperty("chunk_id")
    @Schema(description = "chunk ID")
    private String chunkId;

    /**
     * chunk内容。
     */
    @JsonProperty("content")
    @Schema(description = "chunk内容")
    private String content;

    /**
     * 生成的索引问题列表。
     */
    @JsonProperty("items")
    @Schema(description = "生成的索引问题列表")
    private List<KbAutoQuestionItemVO> items = new ArrayList<>();
}
