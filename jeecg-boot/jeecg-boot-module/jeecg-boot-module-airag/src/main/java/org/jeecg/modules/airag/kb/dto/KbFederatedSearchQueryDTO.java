package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多知识库联邦检索请求。
 */
@Data
@Schema(description = "多知识库联邦检索请求")
public class KbFederatedSearchQueryDTO {
    /**
     * 内部知识库ID列表。
     */
    @JsonProperty("kb_ids")
    @Schema(description = "内部知识库ID列表")
    private List<String> kbIds;

    /**
     * 外部知识库ID列表。
     */
    @JsonProperty("external_kb_ids")
    @Schema(description = "外部知识库ID列表")
    private List<String> externalKbIds;

    /**
     * 查询内容。
     */
    @NotBlank(message = "query不能为空")
    @JsonProperty("query")
    @Schema(description = "查询内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;

    /**
     * 检索模式。
     */
    @Pattern(regexp = "^(semantic|fulltext|hybrid)$", message = "search_mode只能是semantic、fulltext、hybrid")
    @JsonProperty("search_mode")
    @Schema(description = "检索模式")
    private String searchMode;

    /**
     * 每个知识库候选数量。
     */
    @Min(value = 1, message = "top_k必须大于0")
    @JsonProperty("top_k")
    @Schema(description = "每个知识库候选数量")
    private Integer topK;

    /**
     * 最终返回数量。
     */
    @Min(value = 1, message = "final_top_k必须大于0")
    @JsonProperty("final_top_k")
    @Schema(description = "最终返回数量")
    private Integer finalTopK;

    /**
     * 相似度阈值。
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "similarity_threshold不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "similarity_threshold不能大于1")
    @JsonProperty("similarity_threshold")
    @Schema(description = "相似度阈值")
    private BigDecimal similarityThreshold;

    /**
     * 关键词阈值。
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "keyword_threshold不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "keyword_threshold不能大于1")
    @JsonProperty("keyword_threshold")
    @Schema(description = "关键词阈值")
    private BigDecimal keywordThreshold;

    /**
     * 语义权重。
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "semantic_weight不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "semantic_weight不能大于1")
    @JsonProperty("semantic_weight")
    @Schema(description = "语义权重")
    private BigDecimal semanticWeight;

    /**
     * 关键词权重。
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "keyword_weight不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "keyword_weight不能大于1")
    @JsonProperty("keyword_weight")
    @Schema(description = "关键词权重")
    private BigDecimal keywordWeight;

    /**
     * 是否启用Query Optimization。
     */
    @JsonProperty("use_query_optimization")
    @Schema(description = "是否启用Query Optimization")
    private Boolean useQueryOptimization;

    /**
     * Query Optimization模式。
     */
    @Pattern(regexp = "^(off|rewrite|keywords|expand|hybrid)$", message = "query_optimization_mode只能是off、rewrite、keywords、expand、hybrid")
    @JsonProperty("query_optimization_mode")
    @Schema(description = "Query Optimization模式")
    private String queryOptimizationMode;

    /**
     * 最大改写数。
     */
    @Min(value = 1, message = "max_rewrite_queries必须大于0")
    @JsonProperty("max_rewrite_queries")
    @Schema(description = "最大改写数")
    private Integer maxRewriteQueries;

    /**
     * 是否保留原query。
     */
    @JsonProperty("keep_original_query")
    @Schema(description = "是否保留原query")
    private Boolean keepOriginalQuery;

    /**
     * 是否启用Rerank。
     */
    @JsonProperty("use_rerank")
    @Schema(description = "是否启用Rerank")
    private Boolean useRerank;

    /**
     * Rerank候选数量。
     */
    @Min(value = 1, message = "rerank_top_n必须大于0")
    @JsonProperty("rerank_top_n")
    @Schema(description = "Rerank候选数量")
    private Integer rerankTopN;

    /**
     * Rerank分数阈值。
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "rerank_score_threshold不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "rerank_score_threshold不能大于1")
    @JsonProperty("rerank_score_threshold")
    @Schema(description = "Rerank分数阈值")
    private BigDecimal rerankScoreThreshold;

    /**
     * 参考文本上限。
     */
    @Min(value = 1, message = "reference_limit必须大于0")
    @JsonProperty("reference_limit")
    @Schema(description = "参考文本上限")
    private Integer referenceLimit;

    /**
     * 知识库权重。
     */
    @JsonProperty("kb_weights")
    @Schema(description = "知识库权重")
    private Map<String, BigDecimal> kbWeights = new LinkedHashMap<>();

    /**
     * 外部检索过滤条件。
     */
    @JsonProperty("metadata_filter")
    @Schema(description = "外部检索过滤条件")
    private Map<String, Object> metadataFilter = new LinkedHashMap<>();

    /**
     * 历史消息。
     */
    @Valid
    @JsonProperty("chat_history")
    @Schema(description = "历史消息")
    private List<KbQueryOptimizationHistoryDTO> chatHistory;

    /**
     * 外部失败是否严格失败。
     */
    @JsonProperty("strict_external_failure")
    @Schema(description = "外部失败是否严格失败")
    private Boolean strictExternalFailure;
}
