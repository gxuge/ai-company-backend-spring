package org.jeecg.modules.system.service;

import org.jeecg.modules.system.entity.TsMemberOrder;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;

/** 会员支付成功后的积分赠送衔接服务。 */
public interface ITsMemberPointsGiftService {

    /** 按会员套餐规则幂等赠送积分，未配置规则时返回空。 */
    TsPointsTransactionVo grantForPaidOrder(TsMemberOrder order);
}
