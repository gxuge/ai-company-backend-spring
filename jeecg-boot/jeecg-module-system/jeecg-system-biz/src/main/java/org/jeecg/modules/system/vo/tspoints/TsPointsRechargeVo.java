package org.jeecg.modules.system.vo.tspoints;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 积分充值订单与支付响应。 */
@Data
public class TsPointsRechargeVo {
    /** 订单ID。 */
    private Long id;
    /** 充值订单号。 */
    private String orderNo;
    /** 用户ID。 */
    private String userId;
    /** 用户账号。 */
    private String username;
    /** 积分商品ID。 */
    private Long productId;
    /** 商品名称。 */
    private String productName;
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
    /** 支付意图ID。 */
    private String paymentIntentId;
    /** 渠道交易ID。 */
    private String transactionId;
    /** Stripe客户端密钥。 */
    private String clientSecret;
    /** PayPal跳转地址。 */
    private String paymentUrl;
    /** 积分流水号。 */
    private String pointsTransactionNo;
    /** 支付时间。 */
    private Date payTime;
    /** 创建时间。 */
    private Date createdAt;
}
