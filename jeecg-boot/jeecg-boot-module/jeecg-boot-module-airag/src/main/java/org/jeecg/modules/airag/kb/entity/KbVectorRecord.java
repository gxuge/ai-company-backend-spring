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
import java.util.Date;

/**
 * KB向量记录实体。
 */
@Data
@Schema(description = "KB向量记录")
@TableName("kb_vector_record")
public class KbVectorRecord implements Serializable {
    /**
     * 主键ID。
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /**
     * 向量ID。
     */
    @Schema(description = "向量ID")
    @TableField("vector_id")
    private String vectorId;

    /**
     * 知识库ID。
     */
    @Schema(description = "知识库ID")
    @TableField("kb_id")
    private String kbId;

    /**
     * 文档ID。
     */
    @Schema(description = "文档ID")
    @TableField("document_id")
    private String documentId;

    /**
     * chunk ID。
     */
    @Schema(description = "chunk ID")
    @TableField("chunk_id")
    private String chunkId;

    /**
     * chunk_index ID。
     */
    @Schema(description = "chunk_index ID")
    @TableField("chunk_index_id")
    private String chunkIndexId;

    /**
     * embedding模型名称。
     */
    @Schema(description = "embedding模型名称")
    @TableField("embedding_model")
    private String embeddingModel;

    /**
     * 向量维度。
     */
    @Schema(description = "向量维度")
    @TableField("vector_dimension")
    private Integer vectorDimension;

    /**
     * embedding耗时（毫秒）。
     */
    @Schema(description = "embedding耗时（毫秒）")
    @TableField("embedding_duration_ms")
    private Long embeddingDurationMs;

    /**
     * 内容预览。
     */
    @Schema(description = "内容预览")
    @TableField("content_preview")
    private String contentPreview;

    /**
     * 向量JSON。
     */
    @Schema(description = "向量JSON")
    @TableField("vector_json")
    private String vectorJson;

    /**
     * 元数据JSON。
     */
    @Schema(description = "元数据JSON")
    @TableField("metadata_json")
    private String metadataJson;

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
