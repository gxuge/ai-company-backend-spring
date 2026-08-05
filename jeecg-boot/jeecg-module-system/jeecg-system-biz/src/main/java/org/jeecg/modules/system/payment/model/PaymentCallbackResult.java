package org.jeecg.modules.system.payment.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * 渠道回调解析结果。
 */
@Value
@Builder
public class PaymentCallbackResult {

    /** 是否为当前业务需要处理的事件。 */
    boolean processable;
    /** 渠道支付意图或支付订单 ID。 */
    String paymentIntentId;
    /** 渠道交易 ID。 */
    String transactionId;
    /** 统一支付状态。 */
    String status;
    /** 回调支付金额。 */
    BigDecimal amount;
    /** 回调币种。 */
    String currency;
    /** 原始回调内容。 */
    String rawResponse;
}
