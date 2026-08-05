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
 * 会员等级权益关联。
 */
@Data
@Accessors(chain = true)
@TableName("member_plan_benefit")
public class TsMemberPlanBenefit implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 会员等级 ID。 */
    @TableField("plan_id")
    private Long planId;
    /** 权益 ID。 */
    @TableField("benefit_id")
    private Long benefitId;
    /** 权益值。 */
    @TableField("value")
    private String value;
    /** 权益单位。 */
    @TableField("unit")
    private String unit;
    /** 限制类型：ENABLE、LIMIT、MONTH。 */
    @TableField("limit_type")
    private String limitType;
    /** 创建时间。 */
    @TableField("created_at")
    private Date createdAt;
}
