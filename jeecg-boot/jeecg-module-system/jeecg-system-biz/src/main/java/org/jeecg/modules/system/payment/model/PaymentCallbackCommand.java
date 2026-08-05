package org.jeecg.modules.system.payment.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 渠道回调解析命令。
 */
@Value
@Builder
public class PaymentCallbackCommand {

    /** 原始请求体。 */
    String rawBody;
    /** 请求头，键统一使用小写。 */
    Map<String, String> headers;
}
