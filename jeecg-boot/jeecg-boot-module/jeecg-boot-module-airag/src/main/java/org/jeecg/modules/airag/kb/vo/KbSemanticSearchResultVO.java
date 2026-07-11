package org.jeecg.modules.airag.kb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 知识库语义检索结果。
 */
@Data
@Schema(description = "知识库语义检索结果")
public class KbSemanticSearchResultVO {
    /**
     * 查询内容。
     */
    @Schema(description = "查询内容")
    @com.fasterxml.jackson.annotation.JsonProperty("query")
    private String query;

    /**
     * 原始query。
     */
    @Schema(description = "原始query")
    @com.fasterxml.jackson.annotation.JsonProperty("original_query")
    private String originalQuery;

    /**
     * 优化后的query。
     */
    @Schema(description = "优化后的query")
    @com.fasterxml.jackson.annotation.JsonProperty("optimized_queries")
    private List<String> optimizedQueries = new ArrayList<>();

    /**
     * 实际用于检索的query。
     */
    @Schema(description = "实际用于检索的query")
    @com.fasterxml.jackson.annotation.JsonProperty("used_queries")
    private List<String> usedQueries = new ArrayList<>();

    /**
     * 知识库ID列表。
     */
    @Schema(description = "知识库ID列表")
    @com.fasterxml.jackson.annotation.JsonProperty("kb_ids")
    private List<String> kbIds = new ArrayList<>();

    /**
     * 外部知识库ID列表。
     */
    @Schema(description = "外部知识库ID列表")
    @com.fasterxml.jackson.annotation.JsonProperty("external_kb_ids")
    private List<String> externalKbIds = new ArrayList<>();

    /**
     * 实际使用参数。
     */
    @Schema(description = "实际使用参数")
    @com.fasterxml.jackson.annotation.JsonProperty("actual_params")
    private Map<String, Object> actualParams = new LinkedHashMap<>();

    /**
     * 调试信息。
     */
    @Schema(description = "调试信息")
    @com.fasterxml.jackson.annotation.JsonProperty("debug_info")
    private Map<String, Object> debugInfo = new LinkedHashMap<>();

    /**
     * 检索模式。
     */
    @Schema(description = "检索模式")
    @com.fasterxml.jackson.annotation.JsonProperty("search_mode")
    private String searchMode;

    /**
     * 查询优化模式。
     */
    @Schema(description = "查询优化模式")
    @com.fasterxml.jackson.annotation.JsonProperty("query_optimization_mode")
    private String queryOptimizationMode;

    /**
     * 是否启用Query Optimization。
     */
    @Schema(description = "是否启用Query Optimization")
    @com.fasterxml.jackson.annotation.JsonProperty("use_query_optimization")
    private Boolean useQueryOptimization;

    /**
     * 返回数量。
     */
    @Schema(description = "返回数量")
    @com.fasterxml.jackson.annotation.JsonProperty("top_k")
    private Integer topK;

    /**
     * 是否启用Rerank。
     */
    @Schema(description = "是否启用Rerank")
    @com.fasterxml.jackson.annotation.JsonProperty("use_rerank")
    private Boolean useRerank;

    /**
     * Rerank模型。
     */
    @Schema(description = "Rerank模型")
    @com.fasterxml.jackson.annotation.JsonProperty("rerank_model")
    private String rerankModel;

    /**
     * Rerank候选数量。
     */
    @Schema(description = "Rerank候选数量")
    @com.fasterxml.jackson.annotation.JsonProperty("rerank_top_n")
    private Integer rerankTopN;

    /**
     * 最终返回数量。
     */
    @Schema(description = "最终返回数量")
    @com.fasterxml.jackson.annotation.JsonProperty("final_top_k")
    private Integer finalTopK;

    /**
     * 相似度阈值。
     */
    @Schema(description = "相似度阈值")
    @com.fasterxml.jackson.annotation.JsonProperty("similarity_threshold")
    private BigDecimal similarityThreshold;

    /**
     * 全文检索最低分。
     */
    @Schema(description = "全文检索最低分")
    @com.fasterxml.jackson.annotation.JsonProperty("keyword_threshold")
    private BigDecimal keywordThreshold;

    /**
     * 语义权重。
     */
    @Schema(description = "语义权重")
    @com.fasterxml.jackson.annotation.JsonProperty("semantic_weight")
    private BigDecimal semanticWeight;

    /**
     * 关键词权重。
     */
    @Schema(description = "关键词权重")
    @com.fasterxml.jackson.annotation.JsonProperty("keyword_weight")
    private BigDecimal keywordWeight;

    /**
     * 引用长度上限。
     */
    @Schema(description = "引用长度上限")
    @com.fasterxml.jackson.annotation.JsonProperty("reference_limit")
    private Integer referenceLimit;

    /**
     * 最大改写数量。
     */
    @Schema(description = "最大改写数量")
    @com.fasterxml.jackson.annotation.JsonProperty("max_rewrite_queries")
    private Integer maxRewriteQueries;

    /**
     * 是否保留原始query。
     */
    @Schema(description = "是否保留原始query")
    @com.fasterxml.jackson.annotation.JsonProperty("keep_original_query")
    private Boolean keepOriginalQuery;

    /**
     * Rerank阈值。
     */
    @Schema(description = "Rerank阈值")
    @com.fasterxml.jackson.annotation.JsonProperty("rerank_score_threshold")
    private BigDecimal rerankScoreThreshold;

    /**
     * 已使用引用长度。
     */
    @Schema(description = "已使用引用长度")
    @com.fasterxml.jackson.annotation.JsonProperty("used_reference_length")
    private Integer usedReferenceLength;

    /**
     * 返回数量。
     */
    @Schema(description = "返回数量")
    @com.fasterxml.jackson.annotation.JsonProperty("result_count")
    private Integer resultCount;

    /**
     * 检索结果。
     */
    @Schema(description = "检索结果")
    @com.fasterxml.jackson.annotation.JsonProperty("results")
    private List<KbSemanticSearchItemVO> results = new ArrayList<>();
}
