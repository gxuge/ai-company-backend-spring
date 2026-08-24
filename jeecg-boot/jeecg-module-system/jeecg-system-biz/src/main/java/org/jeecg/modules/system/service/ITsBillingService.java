package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsbilling.TsBillingQueryDto;
import org.jeecg.modules.system.vo.tsbilling.TsBillingDetailVo;
import org.jeecg.modules.system.vo.tsbilling.TsBillingRecordVo;
import org.jeecg.modules.system.vo.tsbilling.TsBillingSummaryVo;

/** 双视角统一账单服务。 */
public interface ITsBillingService {

    /** 查询当前用户视角账单。 */
    Page<TsBillingRecordVo> pageUserBills(String userId, TsBillingQueryDto request);

    /** 查询当前用户视角账单详情。 */
    TsBillingDetailVo getUserBill(
            String userId, String recordType, Long recordId);

    /** 查询平台视角账单。 */
    Page<TsBillingRecordVo> pagePlatformBills(TsBillingQueryDto request);

    /** 查询平台视角账单详情。 */
    TsBillingDetailVo getPlatformBill(String recordType, Long recordId);

    /** 汇总平台视角账单。 */
    TsBillingSummaryVo summarizePlatformBills(TsBillingQueryDto request);
}
