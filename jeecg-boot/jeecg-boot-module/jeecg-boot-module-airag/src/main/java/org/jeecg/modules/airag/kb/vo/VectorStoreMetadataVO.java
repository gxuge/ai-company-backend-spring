package org.jeecg.modules.airag.kb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 向量库元数据。
 */
@Data
@Schema(description = "向量库元数据")
public class VectorStoreMetadataVO {
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
     * 索引类型。
     */
    @Schema(description = "索引类型")
    private String indexType;

    /**
     * 来源类型。
     */
    @Schema(description = "来源类型")
    private String sourceType;

    /**
     * 文件类型。
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 内容预览。
     */
    @Schema(description = "内容预览")
    private String contentPreview;

    /**
     * embedding模型名称。
     */
    @Schema(description = "embedding模型名称")
    private String embeddingModel;

    /**
     * 向量维度。
     */
    @Schema(description = "向量维度")
    private Integer vectorDimension;

    /**
     * embedding耗时（毫秒）。
     */
    @Schema(description = "embedding耗时（毫秒）")
    private Long embeddingDurationMs;

    /**
     * 是否截断。
     */
    @Schema(description = "是否截断")
    private Boolean truncated;

    /**
     * 扩展元数据JSON。
     */
    @Schema(description = "扩展元数据JSON")
    private String metadataJson;
}
