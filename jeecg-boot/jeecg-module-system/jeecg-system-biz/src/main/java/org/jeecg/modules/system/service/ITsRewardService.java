package org.jeecg.modules.system.service;

import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardGrantDto;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardGrantVo;

/** 活动统一奖励发放服务。 */
public interface ITsRewardService {

    /** 幂等发放活动奖励。 */
    TsActivityRewardGrantVo grant(TsActivityRewardGrantDto request);
}
