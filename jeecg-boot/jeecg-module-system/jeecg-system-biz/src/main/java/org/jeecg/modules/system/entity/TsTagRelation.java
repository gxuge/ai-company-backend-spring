package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 标签关系规则表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Data
@TableName("ts_tag_relation")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "标签关系规则表")
public class TsTagRelation {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /** 源标签ID */
    @TableField("source_tag_id")
    @Schema(description = "源标签ID")
    private String sourceTagId;

    /** 目标标签ID */
    @TableField("target_tag_id")
    @Schema(description = "目标标签ID")
    private String targetTagId;

    /** 关系类型：compatible|incompatible|requires|boosts|blocks */
    @TableField("relation_type")
    @Schema(description = "关系类型：compatible|incompatible|requires|boosts|blocks")
    private String relationType;

    /** 权重增量 */
    @TableField("weight_delta")
    @Schema(description = "权重增量")
    private Integer weightDelta;

    /** 关系说明 */
    @Schema(description = "关系说明")
    private String description;

    /** 是否启用：1启用，0禁用 */
    @Schema(description = "是否启用：1启用，0禁用")
    private Integer enabled;

    /** 创建时间 */
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private java.util.Date createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private java.util.Date updatedAt;
}
