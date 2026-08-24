package org.jeecg.modules.system.vo.tsreward;

import lombok.Data;

/** 后台奖励事件状态汇总。 */
@Data
public class TsRewardEventSummaryVo {
    /** 待处理事件数。 */
    private Long pendingCount;
    /** 处理中事件数。 */
    private Long processingCount;
    /** 成功事件数。 */
    private Long successCount;
    /** 失败事件数。 */
    private Long failedCount;
    /** 今日创建事件数。 */
    private Long todayCount;
}
