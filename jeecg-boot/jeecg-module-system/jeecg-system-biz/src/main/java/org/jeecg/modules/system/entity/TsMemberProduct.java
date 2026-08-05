package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 会员套餐。
 */
@Data
@Accessors(chain = true)
@TableName("member_product")
public class TsMemberProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 会员等级 ID。 */
    @TableField("plan_id")
    private Long planId;
    /** 周期：WEEK、MONTH、QUARTER、YEAR。 */
    @TableField("cycle_type")
    private String cycleType;
    /** 售价。 */
    @TableField("price")
    private BigDecimal price;
    /** 原价。 */
    @TableField("original_price")
    private BigDecimal originalPrice;
    /** 优惠说明。 */
    @TableField("discount_text")
    private String discountText;
    /** 是否推荐：0否，1是。 */
    @TableField("is_recommend")
    private Integer recommend;
    /** 状态：0停用，1启用。 */
    @TableField("status")
    private Integer status;
    /** 创建时间。 */
    @TableField("created_at")
    private Date createdAt;
}
