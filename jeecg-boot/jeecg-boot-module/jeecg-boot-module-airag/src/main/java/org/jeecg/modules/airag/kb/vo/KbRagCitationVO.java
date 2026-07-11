package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * RAG 引用信息。
 */
@Data
@Schema(description = "RAG 引用信息")
public class KbRagCitationVO {
    /**
     * 引用ID。
     */
    @JsonProperty("citation_id")
    @Schema(description = "引用ID")
    private String citationId;

    /**
     * 知识库ID。
     */
    @JsonProperty("kb_id")
    @Schema(description = "知识库ID")
    private String kbId;

    /**
     * 知识库名称。
     */
    @JsonProperty("kb_name")
    @Schema(description = "知识库名称")
    private String kbName;

    /**
     * 外部知识库ID。
     */
    @JsonProperty("external_kb_id")
    @Schema(description = "外部知识库ID")
    private String externalKbId;

    /**
     * 外部知识库名称。
     */
    @JsonProperty("external_kb_name")
    @Schema(description = "外部知识库名称")
    private String externalKbName;

    /**
     * 文档ID。
     */
    @JsonProperty("document_id")
    @Schema(description = "文档ID")
    private String documentId;

    /**
     * 文档名称。
     */
    @JsonProperty("document_name")
    @Schema(description = "文档名称")
    private String documentName;

    /**
     * chunk ID。
     */
    @JsonProperty("chunk_id")
    @Schema(description = "chunk ID")
    private String chunkId;

    /**
     * chunk_index ID。
     */
    @JsonProperty("chunk_index_id")
    @Schema(description = "chunk_index ID")
    private String chunkIndexId;

    /**
     * 外部结果ID。
     */
    @JsonProperty("external_result_id")
    @Schema(description = "外部结果ID")
    private String externalResultId;

    /**
     * 来源 URL。
     */
    @JsonProperty("source_url")
    @Schema(description = "来源 URL")
    private String sourceUrl;

    /**
     * 内容预览。
     */
    @JsonProperty("content_preview")
    @Schema(description = "内容预览")
    private String contentPreview;

    /**
     * 分数。
     */
    @Schema(description = "分数")
    private BigDecimal score;

    /**
     * Rerank分数。
     */
    @JsonProperty("rerank_score")
    @Schema(description = "Rerank分数")
    private BigDecimal rerankScore;
}
