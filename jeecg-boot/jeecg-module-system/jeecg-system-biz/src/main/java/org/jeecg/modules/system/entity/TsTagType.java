package org.jeecg.modules.system.entity;

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
 * @Description: 生成标签类型字典表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Data
@TableName("ts_tag_type")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "生成标签类型字典表")
public class TsTagType {

    /** 类型ID */
    @TableId
    @Schema(description = "类型ID")
    private String id;

    /** 类型名称 */
    @Schema(description = "类型名称")
    private String name;

    /** 适用目标：character|story|shared */
    @Schema(description = "适用目标：character|story|shared")
    private String scope;

    /** 类型描述 */
    @Schema(description = "类型描述")
    private String description;

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
