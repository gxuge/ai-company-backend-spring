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
 * 知识库文档实体。
 */
@Data
@Schema(description = "知识库文档")
@TableName("kb_document")
public class KbDocument implements Serializable {
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
     * 文档名称。
     */
    @Schema(description = "文档名称")
    private String name;

    /**
     * 来源类型：manual/upload/url/import。
     */
    @Schema(description = "来源类型：manual/upload/url/import")
    @TableField("source_type")
    private String sourceType;

    /**
     * 文件类型。
     */
    @Schema(description = "文件类型")
    @TableField("file_type")
    private String fileType;

    /**
     * 文件地址。
     */
    @Schema(description = "文件地址")
    @TableField("file_url")
    private String fileUrl;

    /**
     * 解析状态：pending/processing/success/failed。
     */
    @Schema(description = "解析状态：pending/processing/success/failed")
    @TableField("parse_status")
    private String parseStatus;

    /**
     * 切分状态：pending/processing/success/failed。
     */
    @Schema(description = "切分状态：pending/processing/success/failed")
    @TableField("chunk_status")
    private String chunkStatus;

    /**
     * 向量状态：pending/processing/success/failed。
     */
    @Schema(description = "向量状态：pending/processing/success/failed")
    @TableField("embed_status")
    private String embedStatus;

    /**
     * 元数据JSON。
     */
    @Schema(description = "元数据JSON")
    @TableField("metadata_json")
    private String metadataJson;

    /**
     * 状态：1启用 0禁用。
     */
    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

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
