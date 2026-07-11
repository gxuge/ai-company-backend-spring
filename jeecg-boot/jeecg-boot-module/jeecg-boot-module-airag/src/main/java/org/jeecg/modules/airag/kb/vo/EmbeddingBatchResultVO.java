package org.jeecg.modules.airag.kb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * embedding批处理结果。
 */
@Data
@Schema(description = "embedding批处理结果")
public class EmbeddingBatchResultVO {
    /**
     * 总数。
     */
    @Schema(description = "总数")
    private Integer totalCount;

    /**
     * 成功数。
     */
    @Schema(description = "成功数")
    private Integer successCount;

    /**
     * 失败数。
     */
    @Schema(description = "失败数")
    private Integer failedCount;

    /**
     * 跳过数。
     */
    @Schema(description = "跳过数")
    private Integer skippedCount;

    /**
     * 明细列表。
     */
    @Schema(description = "明细列表")
    private List<EmbeddingItemResultVO> itemList = new ArrayList<>();
}
