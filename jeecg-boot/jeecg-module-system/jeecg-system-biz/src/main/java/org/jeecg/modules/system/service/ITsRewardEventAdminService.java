package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventAdminQueryDto;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventRetryDto;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventAdminDetailVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventAdminItemVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventSummaryVo;

/** 统一奖励事件后台管理服务。 */
public interface ITsRewardEventAdminService {

    /** 分页查询奖励事件。 */
    Page<TsRewardEventAdminItemVo> pageEvents(TsRewardEventAdminQueryDto request);

    /** 汇总奖励事件状态。 */
    TsRewardEventSummaryVo summarizeEvents(TsRewardEventAdminQueryDto request);

    /** 查询奖励事件详情。 */
    TsRewardEventAdminDetailVo getEvent(Long id);

    /** 手动重试失败奖励事件。 */
    TsRewardEventResultVo retryEvent(TsRewardEventRetryDto request);
}
