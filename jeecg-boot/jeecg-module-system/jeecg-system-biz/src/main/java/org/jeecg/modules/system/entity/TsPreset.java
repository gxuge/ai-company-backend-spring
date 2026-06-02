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
 * @Description: 生成预设主表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Data
@TableName("ts_preset")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "生成预设主表")
public class TsPreset {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    /** 预设名称 */
    @Schema(description = "预设名称")
    private String name;

    /** 预设描述 */
    @Schema(description = "预设描述")
    private String description;

    /** 目标类型：character|story|both */
    @TableField("target_type")
    @Schema(description = "目标类型：character|story|both")
    private String targetType;

    /** 是否启用：1启用，0禁用 */
    @Schema(description = "是否启用：1启用，0禁用")
    private Integer enabled;

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
