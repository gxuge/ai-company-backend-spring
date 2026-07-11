package org.jeecg.modules.airag.kb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 问答请求。
 */
@Data
@Schema(description = "RAG 问答请求")
public class KbRagQuestionDTO {
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
     * 原始问题。
     */
    @NotBlank(message = "query不能为空")
    @JsonProperty("query")
    @Schema(description = "原始问题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;

    /**
     * 历史对话。
     */
    @Valid
    @JsonProperty("chat_history")
    @Schema(description = "历史对话")
    private List<KbQueryOptimizationHistoryDTO> chatHistory;

    /**
     * 检索模式。
     */
    @Pattern(regexp = "^(semantic|fulltext|hybrid)?$", message = "search_mode只能是semantic、fulltext、hybrid")
    @JsonProperty("search_mode")
    @Schema(description = "检索模式")
    private String searchMode;

    /**
     * 候选数量。
     */
    @JsonProperty("top_k")
    @Schema(description = "候选数量")
    private Integer topK;

    /**
     * 最终返回数量。
     */
    @JsonProperty("final_top_k")
    @Schema(description = "最终返回数量")
    private Integer finalTopK;

    /**
     * 引用长度上限。
     */
    @JsonProperty("reference_limit")
    @Schema(description = "引用长度上限")
    private Integer referenceLimit;

    /**
     * 是否启用查询优化。
     */
    @JsonProperty("use_query_optimization")
    @Schema(description = "是否启用查询优化")
    private Boolean useQueryOptimization;

    /**
     * 是否启用重排。
     */
    @JsonProperty("use_rerank")
    @Schema(description = "是否启用重排")
    private Boolean useRerank;

    /**
     * 回答模式。
     */
    @Pattern(regexp = "^(strict|balanced|creative)?$", message = "answer_mode只能是strict、balanced、creative")
    @JsonProperty("answer_mode")
    @Schema(description = "回答模式")
    private String answerMode = "balanced";

    /**
     * 是否返回引用来源。
     */
    @JsonProperty("cite_sources")
    @Schema(description = "是否返回引用来源")
    private Boolean citeSources = Boolean.TRUE;

    /**
     * 是否流式返回。
     */
    @JsonProperty("stream")
    @Schema(description = "是否流式返回")
    private Boolean stream = Boolean.FALSE;

    /**
     * 外部/检索过滤条件。
     */
    @JsonProperty("metadata_filter")
    @Schema(description = "外部/检索过滤条件")
    private Map<String, Object> metadataFilter = new LinkedHashMap<>();
}
