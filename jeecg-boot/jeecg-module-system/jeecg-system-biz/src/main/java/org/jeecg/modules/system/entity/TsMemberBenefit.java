package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 会员权益定义。
 */
@Data
@Accessors(chain = true)
@TableName("member_benefit")
public class TsMemberBenefit implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 权益编码。 */
    @TableField("code")
    private String code;
    /** 权益名称。 */
    @TableField("name")
    private String name;
    /** 权益说明。 */
    @TableField("description")
    private String description;
    /** 权益图标。 */
    @TableField("icon")
    private String icon;
    /** 权益分类。 */
    @TableField("category")
    private String category;
    /** 排序值。 */
    @TableField("sort")
    private Integer sort;
    /** 创建时间。 */
    @TableField("created_at")
    private Date createdAt;
}
