package org.jeecg.modules.system.payment.model;

import lombok.Builder;
import lombok.Value;

/**
 * 渠道支付创建或查询结果。
 */
@Value
@Builder
public class PaymentProviderResult {

    /** 渠道支付意图或支付订单 ID。 */
    String paymentIntentId;
    /** 渠道交易 ID。 */
    String transactionId;
    /** 统一支付状态。 */
    String status;
    /** Stripe 客户端支付密钥。 */
    String clientSecret;
    /** PayPal 支付跳转地址。 */
    String paymentUrl;
    /** 已脱敏的渠道响应。 */
    String rawResponse;
}
