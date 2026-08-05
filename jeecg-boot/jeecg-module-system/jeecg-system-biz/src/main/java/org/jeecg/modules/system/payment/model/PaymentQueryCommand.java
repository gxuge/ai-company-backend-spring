package org.jeecg.modules.system.payment.model;

import lombok.Builder;
import lombok.Value;

/**
 * 渠道支付查询命令。
 */
@Value
@Builder
public class PaymentQueryCommand {

    /** 渠道支付意图或支付订单 ID。 */
    String paymentIntentId;
}
