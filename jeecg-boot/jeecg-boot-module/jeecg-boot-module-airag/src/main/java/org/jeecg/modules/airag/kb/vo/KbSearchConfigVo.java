package org.jeecg.modules.airag.kb.vo;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.entity.KbSearchConfig;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 检索配置返回对象。
 */
@Data
@Schema(description = "检索配置返回对象")
public class KbSearchConfigVo {
    /**
     * 主键ID。
     */
    @Schema(description = "主键ID")
    private String id;

    /**
     * 知识库ID。
     */
    @Schema(description = "知识库ID")
    private String kbId;

    /**
     * 检索模式。
     */
    @Schema(description = "检索模式")
    private String searchMode;

    /**
     * 相似度阈值。
     */
    @Schema(description = "相似度阈值")
    private BigDecimal similarityThreshold;

    /**
     * 参考文本上限。
     */
    @Schema(description = "参考文本上限")
    private Integer referenceLimit;

    /**
     * 返回条数。
     */
    @Schema(description = "返回条数")
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
    private Integer rerankTopN;

    /**
     * 最终返回数量。
     */
    @Schema(description = "最终返回数量")
    private Integer finalTopK;

    /**
     * Rerank分数阈值。
     */
    @Schema(description = "Rerank分数阈值")
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
    private String queryOptimizationMode;

    /**
     * 最大生成优化query数量。
     */
    @Schema(description = "最大生成优化query数量")
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
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /**
     * 由实体转换为返回对象。
     *
     * @param entity 配置实体
     * @return 返回对象
     */
    public static KbSearchConfigVo from(KbSearchConfig entity) {
        if (entity == null) {
            return null;
        }
        KbSearchConfigVo vo = new KbSearchConfigVo();
        BeanUtils.copyProperties(entity, vo);
        JSONObject json = parseConfigJson(entity.getConfigJson());
        vo.rerankModel = json.getString("rerank_model");
        vo.rerankTopN = json.getInteger("rerank_top_n");
        if (vo.rerankTopN == null) {
            vo.rerankTopN = org.jeecg.modules.airag.kb.consts.KbConstants.DEFAULT_RERANK_TOP_N;
        }
        vo.finalTopK = json.getInteger("final_top_k");
        if (vo.finalTopK == null) {
            vo.finalTopK = vo.getTopK() == null ? org.jeecg.modules.airag.kb.consts.KbConstants.DEFAULT_TOP_K : vo.getTopK();
        }
        vo.rerankScoreThreshold = json.getBigDecimal("rerank_score_threshold");
        if (vo.useRerank == null) {
            vo.useRerank = org.jeecg.modules.airag.kb.consts.KbConstants.DEFAULT_USE_RERANK;
        }
        vo.queryOptimizationMode = json.getString("query_optimization_mode");
        if (oConvertUtils.isEmpty(vo.queryOptimizationMode)) {
            vo.queryOptimizationMode = org.jeecg.modules.airag.kb.consts.KbConstants.DEFAULT_QUERY_OPTIMIZATION_MODE;
        }
        vo.maxRewriteQueries = json.getInteger("max_rewrite_queries");
        if (vo.maxRewriteQueries == null || vo.maxRewriteQueries < 1) {
            vo.maxRewriteQueries = org.jeecg.modules.airag.kb.consts.KbConstants.DEFAULT_MAX_REWRITE_QUERIES;
        }
        vo.keepOriginalQuery = json.getBoolean("keep_original_query");
        if (vo.keepOriginalQuery == null) {
            vo.keepOriginalQuery = org.jeecg.modules.airag.kb.consts.KbConstants.DEFAULT_KEEP_ORIGINAL_QUERY;
        }
        if (vo.useQueryOptimization == null) {
            vo.useQueryOptimization = org.jeecg.modules.airag.kb.consts.KbConstants.DEFAULT_USE_QUERY_OPTIMIZATION;
        }
        return vo;
    }

    /**
     * 解析配置JSON。
     *
     * @param configJson 配置JSON
     * @return JSON对象
     */
    private static JSONObject parseConfigJson(String configJson) {
        if (oConvertUtils.isEmpty(configJson)) {
            return new JSONObject();
        }
        try {
            return JSONObject.parseObject(configJson);
        } catch (Exception ex) {
            return new JSONObject();
        }
    }
}
