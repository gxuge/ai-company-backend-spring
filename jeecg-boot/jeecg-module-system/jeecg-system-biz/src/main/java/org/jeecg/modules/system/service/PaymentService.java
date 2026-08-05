package org.jeecg.modules.system.service;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tspayment.TsPaymentCreateDto;
import org.jeecg.modules.system.dto.tspayment.TsPaymentOrderDetailDto;
import org.jeecg.modules.system.vo.tspayment.TsPaymentCreateVo;
import org.jeecg.modules.system.vo.tspayment.TsPaymentOrderVo;

import java.util.Map;

/**
 * 会员支付业务服务。
 */
public interface PaymentService {

    /** 创建会员订单和第三方支付订单。 */
    TsPaymentCreateVo createPayment(LoginUser user, TsPaymentCreateDto request);

    /** 查询当前用户的支付状态。 */
    TsPaymentOrderVo queryPayment(LoginUser user, TsPaymentOrderDetailDto request);

    /** 验签并处理第三方支付回调。 */
    void handleCallback(String provider, String rawBody, Map<String, String> headers);
}
