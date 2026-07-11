package org.jeecg.modules.airag.kb.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 问答返回。
 */
@Data
@Schema(description = "RAG 问答返回")
public class KbRagAnswerVO {
    /**
     * 原始问题。
     */
    @JsonProperty("query")
    @Schema(description = "原始问题")
    private String query;

    /**
     * 原始query。
     */
    @JsonProperty("original_query")
    @Schema(description = "原始query")
    private String originalQuery;

    /**
     * 优化后的query列表。
     */
    @JsonProperty("optimized_queries")
    @Schema(description = "优化后的query列表")
    private List<String> optimizedQueries = new ArrayList<>();

    /**
     * 实际使用的query列表。
     */
    @JsonProperty("used_queries")
    @Schema(description = "实际使用的query列表")
    private List<String> usedQueries = new ArrayList<>();

    /**
     * 知识库ID列表。
     */
    @JsonProperty("kb_ids")
    @Schema(description = "知识库ID列表")
    private List<String> kbIds = new ArrayList<>();

    /**
     * 外部知识库ID列表。
     */
    @JsonProperty("external_kb_ids")
    @Schema(description = "外部知识库ID列表")
    private List<String> externalKbIds = new ArrayList<>();

    /**
     * 回答内容。
     */
    @Schema(description = "回答内容")
    private String answer;

    /**
     * 回答模式。
     */
    @JsonProperty("answer_mode")
    @Schema(description = "回答模式")
    private String answerMode;

    /**
     * 实际参数。
     */
    @JsonProperty("actual_params")
    @Schema(description = "实际参数")
    private Map<String, Object> actualParams = new LinkedHashMap<>();

    /**
     * 使用的上下文。
     */
    @JsonProperty("used_context")
    @Schema(description = "使用的上下文")
    private List<KbRagContextVO> usedContext = new ArrayList<>();

    /**
     * 引用列表。
     */
    @Schema(description = "引用列表")
    private List<KbRagCitationVO> citations = new ArrayList<>();

    /**
     * 检索结果数。
     */
    @JsonProperty("result_count")
    @Schema(description = "检索结果数")
    private Integer resultCount;

    /**
     * 使用的引用长度。
     */
    @JsonProperty("used_reference_length")
    @Schema(description = "使用的引用长度")
    private Integer usedReferenceLength;

    /**
     * 调试信息。
     */
    @JsonProperty("debug_info")
    @Schema(description = "调试信息")
    private Map<String, Object> debugInfo = new LinkedHashMap<>();

    /**
     * 状态。
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 错误信息。
     */
    @JsonProperty("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * LLM模型。
     */
    @JsonProperty("llm_model")
    @Schema(description = "LLM模型")
    private String llmModel;

    /**
     * token使用量。
     */
    @JsonProperty("token_usage")
    @Schema(description = "token使用量")
    private Map<String, Object> tokenUsage = new LinkedHashMap<>();

    /**
     * 是否流式返回。
     */
    @JsonProperty("stream")
    @Schema(description = "是否流式返回")
    private Boolean stream;

    /**
     * 是否返回引用来源。
     */
    @JsonProperty("cite_sources")
    @Schema(description = "是否返回引用来源")
    private Boolean citeSources;

    /**
     * 是否执行 Query Optimization。
     */
    @JsonProperty("use_query_optimization")
    @Schema(description = "是否执行 Query Optimization")
    private Boolean useQueryOptimization;

    /**
     * 是否执行 Rerank。
     */
    @JsonProperty("use_rerank")
    @Schema(description = "是否执行 Rerank")
    private Boolean useRerank;

    /**
     * 检索模式。
     */
    @JsonProperty("search_mode")
    @Schema(description = "检索模式")
    private String searchMode;

    /**
     * 参考长度限制。
     */
    @JsonProperty("reference_limit")
    @Schema(description = "参考长度限制")
    private Integer referenceLimit;

    /**
     * 结果数据。
     */
    @Schema(description = "结果数据")
    private Object data;
}
