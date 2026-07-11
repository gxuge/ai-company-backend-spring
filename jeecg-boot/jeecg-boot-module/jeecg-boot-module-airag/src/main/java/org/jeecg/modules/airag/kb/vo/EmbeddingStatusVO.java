package org.jeecg.modules.airag.kb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * embedding进度统计。
 */
@Data
@Schema(description = "embedding进度统计")
public class EmbeddingStatusVO {
    /**
     * 总数。
     */
    @Schema(description = "总数")
    private Integer total;

    /**
     * pending数量。
     */
    @Schema(description = "pending数量")
    private Integer pending;

    /**
     * processing数量。
     */
    @Schema(description = "processing数量")
    private Integer processing;

    /**
     * success数量。
     */
    @Schema(description = "success数量")
    private Integer success;

    /**
     * failed数量。
     */
    @Schema(description = "failed数量")
    private Integer failed;
}
