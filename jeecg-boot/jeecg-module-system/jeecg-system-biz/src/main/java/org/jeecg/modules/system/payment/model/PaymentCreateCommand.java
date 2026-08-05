package org.jeecg.modules.system.payment.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * 渠道支付创建命令。
 */
@Value
@Builder
public class PaymentCreateCommand {

    /** 会员订单号。 */
    String orderNo;
    /** 支付金额。 */
    BigDecimal amount;
    /** 三位币种编码。 */
    String currency;
    /** 支付描述。 */
    String description;
}
