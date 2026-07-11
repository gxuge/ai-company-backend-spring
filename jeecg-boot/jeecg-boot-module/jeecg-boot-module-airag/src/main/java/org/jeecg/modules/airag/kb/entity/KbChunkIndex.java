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
 * 知识库分段索引实体。
 */
@Data
@Schema(description = "知识库分段索引")
@TableName("kb_chunk_index")
public class KbChunkIndex implements Serializable {
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
     * 分段ID。
     */
    @Schema(description = "分段ID")
    @TableField("chunk_id")
    private String chunkId;

    /**
     * 索引文本。
     */
    @Schema(description = "索引文本")
    @TableField("index_text")
    private String indexText;

    /**
     * 索引类型：default/title/question/summary等。
     */
    @Schema(description = "索引类型：default/title/question/summary等")
    @TableField("index_type")
    private String indexType;

    /**
     * 向量状态：pending/processing/success/failed。
     */
    @Schema(description = "向量状态：pending/processing/success/failed")
    @TableField("embedding_status")
    private String embeddingStatus;

    /**
     * 排序号。
     */
    @Schema(description = "排序号")
    @TableField("sort_no")
    private Integer sortNo;

    /**
     * 状态：1启用 0禁用。
     */
    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

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
