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
 * 会员等级。
 */
@Data
@Accessors(chain = true)
@TableName("member_plan")
public class TsMemberPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 会员名称。 */
    @TableField("name")
    private String name;
    /** 会员编码，例如 PRO、ULTRA。 */
    @TableField("code")
    private String code;
    /** 会员说明。 */
    @TableField("description")
    private String description;
    /** 主题颜色。 */
    @TableField("theme_color")
    private String themeColor;
    /** 状态：0停用，1启用。 */
    @TableField("status")
    private Integer status;
    /** 排序值。 */
    @TableField("sort")
    private Integer sort;
    /** 创建时间。 */
    @TableField("created_at")
    private Date createdAt;
}
