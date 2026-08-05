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
 * 第三方支付流水。
 */
@Data
@Accessors(chain = true)
@TableName("payment_transaction")
public class TsPaymentTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 会员订单 ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 支付渠道：STRIPE、PAYPAL。 */
    @TableField("provider")
    private String provider;
    /** 渠道交易 ID。 */
    @TableField("transaction_id")
    private String transactionId;
    /** 渠道支付意图或支付订单 ID。 */
    @TableField("payment_intent_id")
    private String paymentIntentId;
    /** 支付金额。 */
    @TableField("amount")
    private BigDecimal amount;
    /** 三位币种编码。 */
    @TableField("currency")
    private String currency;
    /** 支付状态：CREATING、PENDING、SUCCEEDED、FAILED、CANCELED。 */
    @TableField("status")
    private String status;
    /** 渠道原始响应，敏感字段会脱敏。 */
    @TableField("raw_response")
    private String rawResponse;
    /** 创建时间。 */
    @TableField("created_at")
    private Date createdAt;
}
