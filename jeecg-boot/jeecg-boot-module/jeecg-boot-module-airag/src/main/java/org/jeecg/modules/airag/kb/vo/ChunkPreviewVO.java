package org.jeecg.modules.airag.kb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * chunk预览结果。
 */
@Data
@Schema(description = "chunk预览结果")
public class ChunkPreviewVO {
    /**
     * 分段内容。
     */
    @Schema(description = "分段内容")
    private String content;

    /**
     * 排序号。
     */
    @Schema(description = "排序号")
    private Integer sortNo;

    /**
     * token估算值。
     */
    @Schema(description = "token估算值")
    private Integer tokenCount;
}
