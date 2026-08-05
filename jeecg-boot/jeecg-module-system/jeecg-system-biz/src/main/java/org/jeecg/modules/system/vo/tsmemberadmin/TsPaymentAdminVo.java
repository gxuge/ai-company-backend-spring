package org.jeecg.modules.system.vo.tsmemberadmin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 支付流水后台列表信息。 */
@Data
public class TsPaymentAdminVo {
    /** 支付流水 ID。 */
    private Long id;
    /** 会员订单 ID。 */
    private Long orderId;
    /** 会员订单号。 */
    private String orderNo;
    /** 用户 ID。 */
    private String userId;
    /** 用户账号。 */
    private String username;
    /** 用户姓名。 */
    private String realname;
    /** 会员套餐 ID。 */
    private Long productId;
    /** 会员等级编码。 */
    private String planCode;
    /** 会员等级名称。 */
    private String planName;
    /** 套餐周期：WEEK、MONTH、QUARTER、YEAR。 */
    private String cycleType;
    /** 支付渠道：STRIPE、PAYPAL。 */
    private String provider;
    /** 渠道支付意图或支付订单 ID。 */
    private String paymentIntentId;
    /** 渠道交易 ID。 */
    private String transactionId;
    /** 支付金额。 */
    private BigDecimal amount;
    /** 三位币种编码。 */
    private String currency;
    /** 支付流水状态。 */
    private String paymentStatus;
    /** 会员订单状态：0待支付，1成功，2退款。 */
    private Integer orderStatus;
    /** 支付流水创建时间。 */
    private Date createdAt;
    /** 订单支付时间。 */
    private Date payTime;
    /** 第三方回调处理时间。 */
    private Date callbackTime;
}
