package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 会员开通赠礼。
 */
@Data
@Accessors(chain = true)
@TableName("member_gift")
public class TsMemberGift implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 会员等级 ID。 */
    @TableField("plan_id")
    private Long planId;
    /** 赠礼名称。 */
    @TableField("name")
    private String name;
    /** 赠礼说明。 */
    @TableField("description")
    private String description;
    /** 赠礼图标。 */
    @TableField("icon")
    private String icon;
    /** 排序值。 */
    @TableField("sort")
    private Integer sort;
}
