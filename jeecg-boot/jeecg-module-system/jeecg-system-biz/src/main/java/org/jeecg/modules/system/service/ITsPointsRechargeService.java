package org.jeecg.modules.system.service;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tspoints.TsPointsRechargeCreateDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRechargeDetailDto;
import org.jeecg.modules.system.payment.model.PaymentCallbackResult;
import org.jeecg.modules.system.vo.tspoints.TsPointsRechargeVo;

import org.jeecg.modules.system.entity.TsPointsRechargeProduct;
import java.util.List;

/** 积分充值订单与支付服务。 */
public interface ITsPointsRechargeService {

    /** 查询启用的积分充值商品。 */
    List<TsPointsRechargeProduct> listProducts();

    /** 创建积分充值订单和第三方支付。 */
    TsPointsRechargeVo createPayment(LoginUser user, TsPointsRechargeCreateDto request);

    /** 查询当前用户积分充值订单并同步渠道状态。 */
    TsPointsRechargeVo queryPayment(LoginUser user, TsPointsRechargeDetailDto request);

    /** 尝试结算积分支付回调，未匹配积分支付流水时返回 false。 */
    boolean settleCallback(String provider, PaymentCallbackResult callback);
}
