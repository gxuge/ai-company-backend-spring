package org.jeecg.modules.system.vo.tspayment;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付结果。
 */
@Data
public class TsPaymentCreateVo {

    /** 会员订单号。 */
    private String orderNo;
    /** 支付渠道。 */
    private String provider;
    /** 渠道支付意图或支付订单 ID。 */
    private String paymentIntentId;
    /** Stripe 客户端支付密钥。 */
    private String clientSecret;
    /** PayPal 支付跳转地址。 */
    private String paymentUrl;
    /** 支付状态。 */
    private String paymentStatus;
    /** 支付金额。 */
    private BigDecimal amount;
    /** 三位币种编码。 */
    private String currency;
}
