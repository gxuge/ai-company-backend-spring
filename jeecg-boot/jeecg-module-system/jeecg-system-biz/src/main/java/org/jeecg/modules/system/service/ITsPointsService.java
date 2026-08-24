package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdjustDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsChangeDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRefundDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsTransactionQueryDto;
import org.jeecg.modules.system.vo.tspoints.TsPointsAccountVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;

/** 积分统一记账服务。 */
public interface ITsPointsService {

    /** 查询用户积分账户，不存在时幂等创建。 */
    TsPointsAccountVo getAccount(String userId);

    /** 分页查询用户积分流水。 */
    Page<TsPointsTransactionVo> pageTransactions(
            String userId, TsPointsTransactionQueryDto request);

    /** 查询用户积分流水详情。 */
    TsPointsTransactionVo getTransaction(String userId, Long id);

    /** 增加积分。 */
    TsPointsTransactionVo add(TsPointsChangeDto request);

    /** 消费积分。 */
    TsPointsTransactionVo consume(TsPointsChangeDto request);

    /** 返还积分。 */
    TsPointsTransactionVo refund(TsPointsRefundDto request);

    /** 后台调整积分，操作人必须来自登录态。 */
    TsPointsTransactionVo adjust(
            TsPointsAdjustDto request, String operatorId);
}
