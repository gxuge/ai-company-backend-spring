package org.jeecg.modules.airag.kb.dto;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.entity.KbSearchConfig;

import java.math.BigDecimal;

/**
 * 保存搜索配置请求。
 */
@Data
@Schema(description = "保存搜索配置请求")
public class KbSearchConfigSaveDto {
    /**
     * 检索模式：semantic/hybrid/fulltext。
     */
    @Schema(description = "检索模式：semantic/hybrid/fulltext")
    @Pattern(regexp = "^(semantic|hybrid|fulltext)$", message = "检索模式只能是semantic、hybrid、fulltext")
    private String searchMode;

    /**
     * 相似度阈值。
     */
    @Schema(description = "相似度阈值")
    @DecimalMin(value = "0.0", inclusive = true, message = "相似度阈值不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "相似度阈值不能大于1")
    private BigDecimal similarityThreshold;

    /**
     * 参考文本上限。
     */
    @Schema(description = "参考文本上限")
    @Min(value = 1, message = "参考文本上限必须大于0")
    private Integer referenceLimit;

    /**
     * 返回条数。
     */
    @Schema(description = "返回条数")
    @Min(value = 1, message = "返回条数必须大于0")
    private Integer topK;

    /**
     * 是否启用Rerank：true是false否。
     */
    @Schema(description = "是否启用Rerank：true是false否")
    private Boolean useRerank;

    /**
     * Rerank模型。
     */
    @Schema(description = "Rerank模型")
    private String rerankModel;

    /**
     * Rerank候选数量。
     */
    @Schema(description = "Rerank候选数量")
    @Min(value = 1, message = "Rerank候选数量必须大于0")
    private Integer rerankTopN;

    /**
     * 最终返回数量。
     */
    @Schema(description = "最终返回数量")
    @Min(value = 1, message = "最终返回数量必须大于0")
    private Integer finalTopK;

    /**
     * Rerank分数阈值。
     */
    @Schema(description = "Rerank分数阈值")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rerank分数阈值不能小于0")
    @DecimalMax(value = "1.0", inclusive = true, message = "Rerank分数阈值不能大于1")
    private BigDecimal rerankScoreThreshold;

    /**
     * 是否启用Query Optimization：true是false否。
     */
    @Schema(description = "是否启用Query Optimization：true是false否")
    private Boolean useQueryOptimization;

    /**
     * 查询优化模式。
     */
    @Schema(description = "查询优化模式")
    @Pattern(regexp = "^(off|rewrite|keywords|expand|hybrid)$", message = "查询优化模式只能是off、rewrite、keywords、expand、hybrid")
    private String queryOptimizationMode;

    /**
     * 最大生成优化query数量。
     */
    @Schema(description = "最大生成优化query数量")
    @Min(value = 1, message = "最大生成优化query数量必须大于0")
    @Max(value = 10, message = "最大生成优化query数量不能超过10")
    private Integer maxRewriteQueries;

    /**
     * 是否保留原始query。
     */
    @Schema(description = "是否保留原始query")
    private Boolean keepOriginalQuery;

    /**
     * 扩展配置JSON。
     */
    @Schema(description = "扩展配置JSON")
    private String configJson;

    /**
     * 转换为实体。
     *
     * @param kbId 知识库ID
     * @return 检索配置实体
     */
    public KbSearchConfig toEntity(String kbId) {
        KbSearchConfig entity = new KbSearchConfig();
        entity.setKbId(kbId);
        entity.setSearchMode(oConvertUtils.isEmpty(searchMode) ? KbConstants.DEFAULT_SEARCH_MODE : searchMode);
        entity.setSimilarityThreshold(similarityThreshold == null ? KbConstants.DEFAULT_SIMILARITY_THRESHOLD : similarityThreshold);
        entity.setReferenceLimit(referenceLimit == null ? KbConstants.DEFAULT_REFERENCE_LIMIT : referenceLimit);
        entity.setTopK(topK == null ? KbConstants.DEFAULT_TOP_K : topK);
        entity.setUseRerank(useRerank == null ? KbConstants.DEFAULT_USE_RERANK : useRerank);
        entity.setUseQueryOptimization(useQueryOptimization == null ? KbConstants.DEFAULT_USE_QUERY_OPTIMIZATION : useQueryOptimization);
        entity.setConfigJson(buildConfigJson(null, true));
        return entity;
    }

    /**
     * 合并扩展配置JSON。
     *
     * @param baseConfigJson 基础配置JSON
     * @param includeDefaults 是否补齐默认值
     * @return 配置JSON
     */
    public String buildConfigJson(String baseConfigJson, boolean includeDefaults) {
        JSONObject json = parseJson(baseConfigJson);
        if (includeDefaults || rerankModel != null) {
            json.put("rerank_model", rerankModel);
        }
        if (includeDefaults || rerankTopN != null) {
            json.put("rerank_top_n", rerankTopN == null ? KbConstants.DEFAULT_RERANK_TOP_N : rerankTopN);
        }
        if (includeDefaults || finalTopK != null) {
            json.put("final_top_k", finalTopK == null ? KbConstants.DEFAULT_TOP_K : finalTopK);
        }
        if (includeDefaults || rerankScoreThreshold != null) {
            json.put("rerank_score_threshold", rerankScoreThreshold);
        }
        if (includeDefaults || queryOptimizationMode != null) {
            json.put("query_optimization_mode", oConvertUtils.isEmpty(queryOptimizationMode) ? KbConstants.DEFAULT_QUERY_OPTIMIZATION_MODE : queryOptimizationMode);
        }
        if (includeDefaults || maxRewriteQueries != null) {
            json.put("max_rewrite_queries", maxRewriteQueries == null ? KbConstants.DEFAULT_MAX_REWRITE_QUERIES : maxRewriteQueries);
        }
        if (includeDefaults || keepOriginalQuery != null) {
            json.put("keep_original_query", keepOriginalQuery == null ? KbConstants.DEFAULT_KEEP_ORIGINAL_QUERY : keepOriginalQuery);
        }
        if (oConvertUtils.isNotEmpty(configJson)) {
            JSONObject customJson = parseJson(configJson);
            customJson.forEach(json::put);
        }
        if (json.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(json);
    }

    /**
     * 解析JSON。
     *
     * @param jsonString JSON字符串
     * @return JSON对象
     */
    private JSONObject parseJson(String jsonString) {
        if (oConvertUtils.isEmpty(jsonString)) {
            return new JSONObject();
        }
        try {
            return JSONObject.parseObject(jsonString);
        } catch (Exception ex) {
            return new JSONObject();
        }
    }
}
