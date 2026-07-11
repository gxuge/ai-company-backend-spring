package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 知识库语义检索结果项。
 */
@Data
@Schema(description = "知识库语义检索结果项")
public class KbSemanticSearchItemVO {
    /**
     * 知识库ID。
     */
    @Schema(description = "知识库ID")
    private String kbId;

    /**
     * 知识库名称。
     */
    @Schema(description = "知识库名称")
    @JsonProperty("kb_name")
    private String kbName;

    /**
     * 来源范围：internal/external。
     */
    @Schema(description = "来源范围：internal/external")
    @JsonProperty("source_scope")
    private String sourceScope;

    /**
     * 外部知识库ID。
     */
    @Schema(description = "外部知识库ID")
    @JsonProperty("external_kb_id")
    private String externalKbId;

    /**
     * 外部知识库名称。
     */
    @Schema(description = "外部知识库名称")
    @JsonProperty("external_kb_name")
    private String externalKbName;

    /**
     * 文档ID。
     */
    @Schema(description = "文档ID")
    private String documentId;

    /**
     * 文档名称。
     */
    @Schema(description = "文档名称")
    private String documentName;

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
     * 外部结果ID。
     */
    @Schema(description = "外部结果ID")
    @JsonProperty("external_result_id")
    private String externalResultId;

    /**
     * chunk内容。
     */
    @Schema(description = "chunk内容")
    private String content;

    /**
     * 命中的索引文本。
     */
    @Schema(description = "命中的索引文本")
    private String indexText;

    /**
     * 命中的索引文本。
     */
    @Schema(description = "命中的索引文本")
    @JsonProperty("matched_index_text")
    private String matchedIndexText;

    /**
     * 命中的索引类型。
     */
    @Schema(description = "命中的索引类型")
    @JsonProperty("matched_index_type")
    private String matchedIndexType;

    /**
     * 命中的query。
     */
    @Schema(description = "命中的query")
    @JsonProperty("matched_query")
    private String matchedQuery;

    /**
     * 命中字段。
     */
    @Schema(description = "命中字段")
    @JsonProperty("matched_field")
    private String matchedField;

    /**
     * 命中文本片段。
     */
    @Schema(description = "命中文本片段")
    @JsonProperty("matched_text")
    private String matchedText;

    /**
     * 语义分数。
     */
    @Schema(description = "语义分数")
    @JsonProperty("semantic_score")
    private BigDecimal semanticScore;

    /**
     * 关键词分数。
     */
    @Schema(description = "关键词分数")
    @JsonProperty("keyword_score")
    private BigDecimal keywordScore;

    /**
     * 融合分数。
     */
    @Schema(description = "融合分数")
    @JsonProperty("final_score")
    private BigDecimal finalScore;

    /**
     * Rerank分数。
     */
    @Schema(description = "Rerank分数")
    @JsonProperty("rerank_score")
    private BigDecimal rerankScore;

    /**
     * 合并分数。
     */
    @Schema(description = "合并分数")
    @JsonProperty("merged_score")
    private BigDecimal mergedScore;

    /**
     * 命中来源。
     */
    @Schema(description = "命中来源")
    @JsonProperty("hit_type")
    private String hitType;

    /**
     * 引用长度。
     */
    @Schema(description = "引用长度")
    @JsonProperty("reference_length")
    private Integer referenceLength;

    /**
     * 相似度得分。
     */
    @Schema(description = "相似度得分")
    private BigDecimal score;

    /**
     * 来源类型。
     */
    @Schema(description = "来源类型")
    private String sourceType;

    /**
     * 标题。
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 来源URL。
     */
    @Schema(description = "来源URL")
    @JsonProperty("source_url")
    private String sourceUrl;

    /**
     * 文件类型。
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 排序号。
     */
    @Schema(description = "排序号")
    private Integer sortNo;

    /**
     * 元数据JSON。
     */
    @Schema(description = "元数据JSON")
    private String metadataJson;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
