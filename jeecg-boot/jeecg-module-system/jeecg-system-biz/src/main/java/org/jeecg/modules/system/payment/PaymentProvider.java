package org.jeecg.modules.system.payment;

import org.jeecg.modules.system.payment.model.PaymentCallbackCommand;
import org.jeecg.modules.system.payment.model.PaymentCallbackResult;
import org.jeecg.modules.system.payment.model.PaymentCreateCommand;
import org.jeecg.modules.system.payment.model.PaymentProviderResult;
import org.jeecg.modules.system.payment.model.PaymentQueryCommand;

/**
 * 第三方支付渠道统一接口。
 */
public interface PaymentProvider {

    /** 返回渠道编码。 */
    String providerCode();

    /** 创建渠道支付订单。 */
    PaymentProviderResult createPayment(PaymentCreateCommand command);

    /** 查询渠道支付状态。 */
    PaymentProviderResult queryPayment(PaymentQueryCommand command);

    /** 验签并解析渠道回调。 */
    PaymentCallbackResult handleCallback(PaymentCallbackCommand command);
}
