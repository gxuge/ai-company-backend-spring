package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/** 积分充值支付流水。 */
@Data
@Accessors(chain = true)
@TableName("points_recharge_payment")
public class TsPointsRechargePayment {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 积分充值订单ID。 */
    private Long orderId;
    /** 支付渠道。 */
    private String provider;
    /** 渠道交易ID。 */
    private String transactionId;
    /** 支付意图或支付订单ID。 */
    private String paymentIntentId;
    /** 支付金额。 */
    private BigDecimal amount;
    /** 币种。 */
    private String currency;
    /** 支付状态。 */
    private String status;
    /** 已脱敏渠道响应。 */
    private String rawResponse;
    /** 创建时间。 */
    private Date createdAt;
}
