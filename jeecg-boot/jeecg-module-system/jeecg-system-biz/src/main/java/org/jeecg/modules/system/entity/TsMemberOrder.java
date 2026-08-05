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
 * 会员订单。
 */
@Data
@Accessors(chain = true)
@TableName("member_order")
public class TsMemberOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** JEECG 用户 ID。 */
    @TableField("user_id")
    private String userId;
    /** 套餐 ID。 */
    @TableField("product_id")
    private Long productId;
    /** 订单号。 */
    @TableField("order_no")
    private String orderNo;
    /** 订单金额。 */
    @TableField("amount")
    private BigDecimal amount;
    /** 支付渠道。 */
    @TableField("payment_channel")
    private String paymentChannel;
    /** 真实支付渠道：STRIPE、PAYPAL。 */
    @TableField("provider")
    private String provider;
    /** 渠道交易 ID。 */
    @TableField("transaction_id")
    private String transactionId;
    /** 支付状态：CREATING、PENDING、SUCCEEDED、FAILED、CANCELED。 */
    @TableField("payment_status")
    private String paymentStatus;
    /** 第三方回调处理时间。 */
    @TableField("callback_time")
    private Date callbackTime;
    /** 状态：0待支付，1成功，2退款。 */
    @TableField("status")
    private Integer status;
    /** 支付时间。 */
    @TableField("pay_time")
    private Date payTime;
    /** 创建时间。 */
    @TableField("created_at")
    private Date createdAt;
}
