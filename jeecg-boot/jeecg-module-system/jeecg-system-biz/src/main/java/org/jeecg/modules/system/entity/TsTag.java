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
 * @Description: 角色与故事固定标签词典
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Data
@TableName("ts_tag")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "角色与故事固定标签词典")
public class TsTag {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 内容类型：role|story */
    @Schema(description = "内容类型：role|story")
    private String scope;

    /** 标签类型ID */
    @TableField("type_id")
    @Schema(description = "标签类型ID")
    private String typeId;

    /** 标签名称 */
    @Schema(description = "标签名称")
    private String name;

    /** 标签描述 */
    @Schema(description = "标签描述")
    private String description;

    /** 是否启用：1启用，0禁用 */
    @Schema(description = "是否启用：1启用，0禁用")
    private Integer enabled;

    /** 词典版本 */
    @Schema(description = "词典版本")
    private Integer version;

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
