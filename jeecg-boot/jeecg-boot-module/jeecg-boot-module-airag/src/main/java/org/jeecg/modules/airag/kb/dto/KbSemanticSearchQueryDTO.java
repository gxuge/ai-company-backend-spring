package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;
import java.math.BigDecimal;

/**
 * 知识库语义检索请求。
 */
@Data
@Schema(description = "知识库语义检索请求")
public class KbSemanticSearchQueryDTO {
    /**
     * 用户问题。
     */
    @NotBlank(message = "query不能为空")
    @JsonProperty("query")
    @Schema(description = "用户问题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;

    /**
     * 检索模式：semantic/fulltext/hybrid。
     */
    @Pattern(regexp = "^(semantic|fulltext|hybrid)$", message = "search_mode只能是semantic、fulltext、hybrid")
    @JsonProperty("search_mode")
    @Schema(description = "检索模式：semantic/fulltext/hybrid")
    private String searchMode;

    /**
     * 返回数量。
     */
    @Min(value = 1, message = "top_k必须大于0")
    @JsonProperty("top_k")
    @Schema(description = "返回数量")
    private Integer topK;

    /**
     * 参考文本上限。
     */
    @Min(value = 1, message = "reference_limit必须大于0")
    @JsonProperty("reference_limit")
    @Schema(description = "参考文本上限")
    private Integer referenceLimit;

    /**
     * 是否启用Rerank。
     */
    @JsonProperty("use_rerank")
    @Schema(description = "是否启用Rerank")
    private Boolean useRerank;

    /**
     * Rerank模型。
     */
    @JsonProperty("rerank_model")
    @Schema(description = "Rerank模型")
    private String rerankModel;

    /**
     * Rerank候选数量。
     */
    @Min(value = 1, message = "rerank_top_n必须大于0")
    @JsonProperty("rerank_top_n")
    @Schema(description = "Rerank候选数量")
    private Integer rerankTopN;

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
     * 全文检索最低分。
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "keyword_threshold不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "keyword_threshold不能大于1")
    @JsonProperty("keyword_threshold")
    @Schema(description = "全文检索最低分")
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
     * Rerank分数阈值。
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "rerank_score_threshold不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "rerank_score_threshold不能大于1")
    @JsonProperty("rerank_score_threshold")
    @Schema(description = "Rerank分数阈值")
    private BigDecimal rerankScoreThreshold;

    /**
     * 是否启用Query Optimization。
     */
    @JsonProperty("use_query_optimization")
    @Schema(description = "是否启用Query Optimization")
    private Boolean useQueryOptimization;

    /**
     * 查询优化模式。
     */
    @Pattern(regexp = "^(off|rewrite|keywords|expand|hybrid)$", message = "query_optimization_mode只能是off、rewrite、keywords、expand、hybrid")
    @JsonProperty("query_optimization_mode")
    @Schema(description = "查询优化模式")
    private String queryOptimizationMode;

    /**
     * 最大生成的优化query数量。
     */
    @Min(value = 1, message = "max_rewrite_queries必须大于0")
    @Max(value = 10, message = "max_rewrite_queries不能超过10")
    @JsonProperty("max_rewrite_queries")
    @Schema(description = "最大生成的优化query数量")
    private Integer maxRewriteQueries;

    /**
     * 是否保留原始query。
     */
    @JsonProperty("keep_original_query")
    @Schema(description = "是否保留原始query")
    private Boolean keepOriginalQuery;

    /**
     * 最近多轮对话。
     */
    @Valid
    @JsonProperty("chat_history")
    @Schema(description = "最近多轮对话")
    private List<KbQueryOptimizationHistoryDTO> chatHistory;
}
