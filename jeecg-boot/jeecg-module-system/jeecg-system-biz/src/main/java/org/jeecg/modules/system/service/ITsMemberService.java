package org.jeecg.modules.system.service;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsmember.TsMemberBenefitCheckDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberBenefitConsumeDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberOrderCallbackDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberOrderCreateDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberOrderDetailDto;
import org.jeecg.modules.system.vo.tsmember.TsMemberBenefitCheckVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberCompareVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberCurrentVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberOrderVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberPageVo;

import java.util.Date;

/**
 * 会员订阅业务服务。
 */
public interface ITsMemberService {

    /** 获取会员首页配置。 */
    TsMemberPageVo getMemberPage();

    /** 获取 PRO 与 ULTRA 权益对比。 */
    TsMemberCompareVo getMemberCompare();

    /** 获取当前登录用户会员状态。 */
    TsMemberCurrentVo getCurrentMembership(LoginUser user);

    /** 创建当前登录用户的会员订单。 */
    TsMemberOrderVo createOrder(LoginUser user, TsMemberOrderCreateDto request);

    /** 查询当前登录用户的会员订单。 */
    TsMemberOrderVo getOrder(LoginUser user, TsMemberOrderDetailDto request);

    /** 处理当前登录用户订单的支付成功回调。 */
    TsMemberOrderVo handlePaymentCallback(LoginUser user, TsMemberOrderCallbackDto request);

    /** 处理已通过第三方验签的支付成功订单。 */
    TsMemberOrderVo activateVerifiedPayment(
            String orderNo,
            String provider,
            String transactionId,
            Date callbackTime);

    /** 检查当前登录用户的权益可用状态。 */
    TsMemberBenefitCheckVo checkBenefit(LoginUser user, TsMemberBenefitCheckDto request);

    /** 消耗当前登录用户的权益额度。 */
    TsMemberBenefitCheckVo consumeBenefit(LoginUser user, TsMemberBenefitConsumeDto request);
}
