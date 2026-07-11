package org.jeecg.modules.airag.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 知识库检索配置实体。
 */
@Data
@Schema(description = "知识库检索配置")
@TableName("kb_search_config")
public class KbSearchConfig implements Serializable {
    /**
     * 主键ID。
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /**
     * 知识库ID。
     */
    @Schema(description = "知识库ID")
    @TableField("kb_id")
    private String kbId;

    /**
     * 检索模式：semantic/hybrid/fulltext。
     */
    @Schema(description = "检索模式：semantic/hybrid/fulltext")
    @TableField("search_mode")
    private String searchMode;

    /**
     * 相似度阈值。
     */
    @Schema(description = "相似度阈值")
    @TableField("similarity_threshold")
    private BigDecimal similarityThreshold;

    /**
     * 参考文本上限。
     */
    @Schema(description = "参考文本上限")
    @TableField("reference_limit")
    private Integer referenceLimit;

    /**
     * 返回条数。
     */
    @Schema(description = "返回条数")
    @TableField("top_k")
    private Integer topK;

    /**
     * 是否启用Rerank：true是false否。
     */
    @Schema(description = "是否启用Rerank：true是false否")
    @TableField("use_rerank")
    private Boolean useRerank;

    /**
     * 是否启用Query Optimization：true是false否。
     */
    @Schema(description = "是否启用Query Optimization：true是false否")
    @TableField("use_query_optimization")
    private Boolean useQueryOptimization;

    /**
     * 扩展配置JSON。
     */
    @Schema(description = "扩展配置JSON")
    @TableField("config_json")
    private String configJson;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("created_at")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("updated_at")
    private Date updatedAt;
}
