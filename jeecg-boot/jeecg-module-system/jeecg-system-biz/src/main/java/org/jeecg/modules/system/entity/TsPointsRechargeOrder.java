package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/** 积分充值订单。 */
@Data
@Accessors(chain = true)
@TableName("points_recharge_order")
public class TsPointsRechargeOrder {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 充值订单号。 */
    private String orderNo;
    /** 用户ID。 */
    private String userId;
    /** 积分商品ID。 */
    private Long productId;
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
    /** 支付渠道。 */
    private String paymentChannel;
    /** 支付状态。 */
    private String status;
    /** 渠道交易ID。 */
    private String transactionId;
    /** 积分入账流水号。 */
    private String pointsTransactionNo;
    /** 支付时间。 */
    private Date payTime;
    /** 回调处理时间。 */
    private Date callbackTime;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
