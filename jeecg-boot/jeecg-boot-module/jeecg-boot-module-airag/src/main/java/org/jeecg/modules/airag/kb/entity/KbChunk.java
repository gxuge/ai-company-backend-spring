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
 * 知识库分段实体。
 */
@Data
@Schema(description = "知识库分段")
@TableName("kb_chunk")
public class KbChunk implements Serializable {
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
     * 文档ID。
     */
    @Schema(description = "文档ID")
    @TableField("document_id")
    private String documentId;

    /**
     * 分段内容。
     */
    @Schema(description = "分段内容")
    private String content;

    /**
     * 分段类型：text/table/code/qa等。
     */
    @Schema(description = "分段类型：text/table/code/qa等")
    @TableField("chunk_type")
    private String chunkType;

    /**
     * Token数量。
     */
    @Schema(description = "Token数量")
    @TableField("token_count")
    private Integer tokenCount;

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
