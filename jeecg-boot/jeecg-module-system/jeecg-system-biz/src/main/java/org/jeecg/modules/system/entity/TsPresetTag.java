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
 * @Description: 预设与标签关联表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Data
@TableName("ts_preset_tag")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "预设与标签关联表")
public class TsPresetTag {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /** 预设ID */
    @TableField("preset_id")
    @Schema(description = "预设ID")
    private String presetId;

    /** 标签ID */
    @TableField("tag_id")
    @Schema(description = "标签ID")
    private String tagId;

    /** 是否必选：1是，0否 */
    @TableField("is_required")
    @Schema(description = "是否必选：1是，0否")
    private Integer required;

    /** 权重覆盖值 */
    @TableField("weight_override")
    @Schema(description = "权重覆盖值")
    private Integer weightOverride;

    /** 排序值 */
    @TableField("sort_order")
    @Schema(description = "排序值")
    private Integer sortOrder;

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
