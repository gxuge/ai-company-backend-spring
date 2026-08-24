package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/** 积分充值商品。 */
@Data
@Accessors(chain = true)
@TableName("points_recharge_product")
public class TsPointsRechargeProduct {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 商品名称。 */
    private String name;
    /** 购买积分。 */
    private Long points;
    /** 赠送积分。 */
    private Long giftPoints;
    /** 原价。 */
    private BigDecimal originalAmount;
    /** 实付金额。 */
    private BigDecimal actualAmount;
    /** 币种。 */
    private String currency;
    /** 状态：0停用，1启用。 */
    private Integer status;
    /** 排序。 */
    private Integer sort;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
