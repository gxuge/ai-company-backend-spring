package org.jeecg.modules.airag.kb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单条embedding处理结果。
 */
@Data
@Schema(description = "单条embedding处理结果")
public class EmbeddingItemResultVO {
    /**
     * 结果状态：success/failed/skipped。
     */
    @Schema(description = "结果状态：success/failed/skipped")
    private String status;

    /**
     * 知识库ID。
     */
    @Schema(description = "知识库ID")
    private String kbId;

    /**
     * 文档ID。
     */
    @Schema(description = "文档ID")
    private String documentId;

    /**
     * chunk ID。
     */
    @Schema(description = "chunk ID")
    private String chunkId;

    /**
     * chunk_index ID。
     */
    @Schema(description = "chunk_index ID")
    private String chunkIndexId;

    /**
     * 向量ID。
     */
    @Schema(description = "向量ID")
    private String vectorId;

    /**
     * 模型名称。
     */
    @Schema(description = "模型名称")
    private String modelName;

    /**
     * 向量维度。
     */
    @Schema(description = "向量维度")
    private Integer vectorDimension;

    /**
     * 耗时（毫秒）。
     */
    @Schema(description = "耗时（毫秒）")
    private Long durationMs;

    /**
     * 错误信息。
     */
    @Schema(description = "错误信息")
    private String errorMessage;
}
