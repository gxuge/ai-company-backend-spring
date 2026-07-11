package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * RAG 使用的上下文项。
 */
@Data
@Schema(description = "RAG 使用的上下文项")
public class KbRagContextVO {
    /**
     * 来源范围。
     */
    @JsonProperty("source_scope")
    @Schema(description = "来源范围")
    private String sourceScope;

    /**
     * 来源ID。
     */
    @JsonProperty("source_id")
    @Schema(description = "来源ID")
    private String sourceId;

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
     * 内容。
     */
    @Schema(description = "内容")
    private String content;

    /**
     * 命中的索引文本。
     */
    @JsonProperty("matched_index_text")
    @Schema(description = "命中的索引文本")
    private String matchedIndexText;

    /**
     * 命中的索引类型。
     */
    @JsonProperty("matched_index_type")
    @Schema(description = "命中的索引类型")
    private String matchedIndexType;

    /**
     * 命中的 query。
     */
    @JsonProperty("matched_query")
    @Schema(description = "命中的 query")
    private String matchedQuery;

    /**
     * 命中字段。
     */
    @JsonProperty("matched_field")
    @Schema(description = "命中字段")
    private String matchedField;

    /**
     * 命中文本。
     */
    @JsonProperty("matched_text")
    @Schema(description = "命中文本")
    private String matchedText;

    /**
     * 语义分数。
     */
    @JsonProperty("semantic_score")
    @Schema(description = "语义分数")
    private BigDecimal semanticScore;

    /**
     * 关键词分数。
     */
    @JsonProperty("keyword_score")
    @Schema(description = "关键词分数")
    private BigDecimal keywordScore;

    /**
     * 最终分数。
     */
    @JsonProperty("final_score")
    @Schema(description = "最终分数")
    private BigDecimal finalScore;

    /**
     * Rerank分数。
     */
    @JsonProperty("rerank_score")
    @Schema(description = "Rerank分数")
    private BigDecimal rerankScore;

    /**
     * 融合分数。
     */
    @JsonProperty("merged_score")
    @Schema(description = "融合分数")
    private BigDecimal mergedScore;

    /**
     * 命中来源类型。
     */
    @JsonProperty("hit_type")
    @Schema(description = "命中来源类型")
    private String hitType;

    /**
     * 引用长度。
     */
    @JsonProperty("reference_length")
    @Schema(description = "引用长度")
    private Integer referenceLength;

    /**
     * 分数。
     */
    @Schema(description = "分数")
    private BigDecimal score;

    /**
     * 来源类型。
     */
    @JsonProperty("source_type")
    @Schema(description = "来源类型")
    private String sourceType;

    /**
     * 标题。
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 来源 URL。
     */
    @JsonProperty("source_url")
    @Schema(description = "来源 URL")
    private String sourceUrl;

    /**
     * 文件类型。
     */
    @JsonProperty("file_type")
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 排序号。
     */
    @JsonProperty("sort_no")
    @Schema(description = "排序号")
    private Integer sortNo;

    /**
     * 元数据。
     */
    @JsonProperty("metadata_json")
    @Schema(description = "元数据")
    private String metadataJson;
}
